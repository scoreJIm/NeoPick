#!/bin/bash
# =============================================================================
# NEOPick Secrets Management Setup
# =============================================================================
# Usage: ./scripts/secrets-setup.sh [sealed|eso|vault|manual]
#
#   sealed   Create a SealedSecret from .env.prod using kubeseal
#   eso      Set up External Secrets Operator with AWS Secrets Manager
#   vault    Set up HashiCorp Vault and configure secret store
#   manual   Print instructions for manual secret creation
#
# Examples:
#   ./scripts/secrets-setup.sh sealed
#   ./scripts/secrets-setup.sh eso
#   ./scripts/secrets-setup.sh
# =============================================================================

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${REPO_ROOT}/.env.prod"

# -----------------------------------------------------------------------------
# check_prerequisites
# -----------------------------------------------------------------------------
check_prerequisites() {
    local required_tools=("kubectl")
    local missing=()

    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            missing+=("$tool")
        fi
    done

    if [[ ${#missing[@]} -gt 0 ]]; then
        log_error "Missing required tools: ${missing[*]}"
        log_error "Please install them and try again."
        exit 1
    fi

    # Check cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Check your kubeconfig."
        exit 1
    fi
}

# -----------------------------------------------------------------------------
# check_env_file
# -----------------------------------------------------------------------------
check_env_file() {
    if [[ ! -f "$ENV_FILE" ]]; then
        log_error ".env.prod not found at ${ENV_FILE}"
        log_info  "Create one with the following keys:"
        echo
        echo "  DB_USERNAME=your_db_user"
        echo "  DB_PASSWORD=your_db_password"
        echo "  REDIS_PASSWORD=your_redis_password"
        echo "  JWT_SECRET=your_jwt_secret"
        echo "  ALIYUN_ACCESS_KEY=your_aliyun_access_key"
        echo "  ALIYUN_SECRET_KEY=your_aliyun_secret_key"
        echo "  WECHAT_APP_ID=your_wechat_app_id"
        echo "  WECHAT_API_KEY=your_wechat_api_key"
        echo "  ALIPAY_APP_ID=your_alipay_app_id"
        echo "  ALIPAY_PRIVATE_KEY=your_alipay_private_key"
        echo "  AWS_ACCESS_KEY_ID=your_aws_access_key"
        echo "  AWS_SECRET_ACCESS_KEY=your_aws_secret_key"
        exit 1
    fi
}

# -----------------------------------------------------------------------------
# install_kubeseal
# -----------------------------------------------------------------------------
install_kubeseal() {
    if command -v kubeseal &> /dev/null; then
        log_info "kubeseal is already installed: $(kubeseal --version 2>&1 || true)"
        return
    fi

    log_info "Installing kubeseal..."

    local os
    local arch
    os="$(uname -s | tr '[:upper:]' '[:lower:]')"
    arch="$(uname -m)"

    case "$arch" in
        x86_64)  arch="amd64" ;;
        aarch64) arch="arm64" ;;
    esac

    local version="v0.26.3"
    local tarball="kubeseal-${version#v}-${os}-${arch}.tar.gz"
    local url="https://github.com/bitnami-labs/sealed-secrets/releases/download/${version}/${tarball}"

    log_info "Downloading kubeseal ${version} for ${os}/${arch}..."
    curl -sSL "$url" -o "/tmp/${tarball}"
    tar -xzf "/tmp/${tarball}" -C /tmp kubeseal
    sudo mv /tmp/kubeseal /usr/local/bin/kubeseal
    sudo chmod +x /usr/local/bin/kubeseal
    rm -f "/tmp/${tarball}"

    log_info "kubeseal installed: $(kubeseal --version)"
}

# -----------------------------------------------------------------------------
# setup_sealed_secret
# -----------------------------------------------------------------------------
setup_sealed_secret() {
    log_info "=== Setting up Sealed Secrets ==="
    echo

    # 1. Check tools
    check_prerequisites
    check_env_file
    install_kubeseal

    # 2. Check if Sealed Secrets controller is installed
    if ! kubectl get deployment sealed-secrets-controller -n kube-system &> /dev/null; then
        log_warn "Sealed Secrets controller not found in cluster."
        log_info  "Installing via Helm..."
        if ! command -v helm &> /dev/null; then
            log_error "Helm is not installed. Please install Helm first: https://helm.sh"
            log_info  "Or install sealed-secrets manually:"
            log_info  "  kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.26.3/controller.yaml"
            exit 1
        fi
        helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets 2>/dev/null || true
        helm repo update
        helm upgrade --install sealed-secrets sealed-secrets/sealed-secrets \
            --namespace kube-system \
            --wait
    else
        log_info "Sealed Secrets controller is already installed."
    fi

    # 3. Fetch the public cert
    log_info "Fetching controller public key..."
    kubeseal --fetch-cert --controller-name=sealed-secrets --controller-namespace=kube-system \
        > /tmp/sealed-secrets-cert.pem 2>/dev/null || {
        # Fallback: fetch from controller service
        kubeseal --fetch-cert > /tmp/sealed-secrets-cert.pem 2>/dev/null || {
            log_error "Could not fetch the Sealed Secrets public certificate."
            log_error "Run 'kubeseal --fetch-cert' manually to diagnose."
            exit 1
        }
    }

    # 4. Encrypt the .env.prod file
    log_info "Encrypting secrets from ${ENV_FILE}..."

    local output_file="${REPO_ROOT}/k8s/sealed-secret.yaml"

    # Build env-from-file arguments from the expected keys
    # This reads the .env.prod file and creates --from-literal args
    local from_literals=()
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        [[ -z "$key" || "$key" =~ ^[[:space:]]*# ]] && continue
        # Trim whitespace
        key="${key%"${key##*[![:space:]]}"}"
        key="${key#"${key%%[![:space:]]*}"}"
        value="${value%"${value##*[![:space:]]}"}"
        value="${value#"${value%%[![:space:]]*}"}"
        from_literals+=("--from-literal=${key}=${value}")
    done < "$ENV_FILE"

    if [[ ${#from_literals[@]} -eq 0 ]]; then
        log_error "No valid key=value pairs found in ${ENV_FILE}"
        exit 1
    fi

    # Create secret in dry-run mode and pipe through kubeseal
    kubectl create secret generic neopick-api-secret \
        "${from_literals[@]}" \
        --dry-run=client -o yaml 2>/dev/null | \
        kubeseal \
            --format yaml \
            --controller-name=sealed-secrets \
            --controller-namespace=kube-system \
            > "$output_file"

    log_info "SealedSecret written to ${output_file}"

    # 5. Apply
    read -rp "Apply to cluster now? (y/N): " apply_now
    if [[ "$apply_now" =~ ^[Yy]$ ]]; then
        kubectl apply -f "$output_file"
        log_info "SealedSecret applied to cluster."
    fi

    echo
    log_info "=== Sealed Secrets setup complete ==="
    log_info "Add the following to k8s/kustomization.yaml resources:"
    log_info "  - sealed-secret.yaml"
}

# -----------------------------------------------------------------------------
# setup_external_secrets_operator
# -----------------------------------------------------------------------------
setup_external_secrets_operator() {
    log_info "=== Setting up External Secrets Operator (AWS) ==="
    echo

    check_prerequisites

    if ! command -v helm &> /dev/null; then
        log_error "Helm is required. Install from https://helm.sh"
        exit 1
    fi

    # 1. Install ESO
    if ! kubectl get deployment external-secrets -n external-secrets-system &> /dev/null; then
        log_info "Installing External Secrets Operator..."
        helm repo add external-secrets https://charts.external-secrets.io 2>/dev/null || true
        helm repo update
        helm upgrade --install external-secrets external-secrets/external-secrets \
            --namespace external-secrets-system \
            --create-namespace \
            --wait
    else
        log_info "External Secrets Operator is already installed."
    fi

    # 2. Create AWS credentials secret
    log_info "Setting up AWS credentials for SecretStore..."
    local aws_access_key=""
    local aws_secret_key=""

    read -rp "AWS Access Key ID: " aws_access_key
    read -rsp "AWS Secret Access Key: " aws_secret_key
    echo

    if [[ -n "$aws_access_key" && -n "$aws_secret_key" ]]; then
        kubectl create secret generic aws-credentials \
            --from-literal=access-key-id="$aws_access_key" \
            --from-literal=secret-access-key="$aws_secret_key" \
            --dry-run=client -o yaml | kubectl apply -f -
        log_info "AWS credentials secret created/updated."
    else
        log_warn "Skipping AWS credentials. Create manually:"
        log_warn "  kubectl create secret generic aws-credentials --from-literal=access-key-id=... --from-literal=secret-access-key=..."
    fi

    # 3. Apply SecretStore
    log_info "Applying SecretStore..."
    kubectl apply -f "${REPO_ROOT}/k8s/secret-store.yaml"

    # 4. Apply ExternalSecret
    log_info "Applying ExternalSecret..."
    kubectl apply -f "${REPO_ROOT}/k8s/external-secret.yaml"

    echo
    log_info "=== ESO setup complete ==="
    log_info "IMPORTANT: Populate AWS Secrets Manager with the following keys:"
    echo
    echo "  neopick/prod/database    -> { username, password }"
    echo "  neopick/prod/redis       -> { password }"
    echo "  neopick/prod/jwt         -> { secret }"
    echo "  neopick/prod/sms         -> { access-key, secret-key }"
    echo "  neopick/prod/wechat      -> { app-id, api-key }"
    echo "  neopick/prod/alipay      -> { app-id, private-key }"
    echo "  neopick/prod/aws          -> { access-key-id, secret-access-key }"
    echo
    log_info "Add the following to k8s/kustomization.yaml resources:"
    log_info "  - secret-store.yaml"
    log_info "  - external-secret.yaml"
}

# -----------------------------------------------------------------------------
# setup_vault
# -----------------------------------------------------------------------------
setup_vault() {
    log_info "=== Setting up HashiCorp Vault ==="
    echo

    check_prerequisites

    if ! command -v helm &> /dev/null; then
        log_error "Helm is required. Install from https://helm.sh"
        exit 1
    fi

    # 1. Install ESO
    if ! kubectl get deployment external-secrets -n external-secrets-system &> /dev/null; then
        log_info "Installing External Secrets Operator..."
        helm repo add external-secrets https://charts.external-secrets.io 2>/dev/null || true
        helm repo update
        helm upgrade --install external-secrets external-secrets/external-secrets \
            --namespace external-secrets-system \
            --create-namespace \
            --wait
    else
        log_info "External Secrets Operator is already installed."
    fi

    # 2. Install Vault (dev mode for testing; use production vault for real)
    log_info "Installing Vault (dev mode)..."
    helm repo add hashicorp https://helm.releases.hashicorp.com 2>/dev/null || true
    helm repo update
    helm upgrade --install vault hashicorp/vault \
        --set "server.dev.enabled=true" \
        --namespace vault \
        --create-namespace \
        --wait

    # 3. Create Vault SecretStore
    log_info "Creating Vault SecretStore..."
    cat <<'VAULT_STORE' | kubectl apply -f -
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: vault-backend
spec:
  provider:
    vault:
      server: "http://vault.vault.svc.cluster.local:8200"
      path: "secret"
      version: "v2"
      auth:
        tokenSecretRef:
          name: vault-token
          key: token
VAULT_STORE

    # Get root token (dev mode)
    local root_token
    root_token="$(kubectl exec -n vault vault-0 -- vault token create -field=token 2>/dev/null || echo "")"
    if [[ -z "$root_token" ]]; then
        log_warn "Could not retrieve Vault root token. You'll need to set this manually."
    else
        kubectl create secret generic vault-token --from-literal=token="$root_token" --dry-run=client -o yaml | kubectl apply -f -
        log_info "Vault root token stored in Kubernetes secret 'vault-token'."
    fi

    # 4. Instructions for populating Vault
    echo
    log_info "=== Vault setup complete ==="
    log_info "Populate Vault with secrets:"
    echo
    echo "  kubectl exec -n vault vault-0 -- sh -c '"
    echo "    vault kv put secret/neopick/prod/database username=<value> password=<value>"
    echo "    vault kv put secret/neopick/prod/redis    password=<value>"
    echo "    vault kv put secret/neopick/prod/jwt      secret=<value>"
    echo "    vault kv put secret/neopick/prod/sms      access-key=<value> secret-key=<value>"
    echo "    vault kv put secret/neopick/prod/wechat   app-id=<value> api-key=<value>"
    echo "    vault kv put secret/neopick/prod/alipay   app-id=<value> private-key=<value>"
    echo "    vault kv put secret/neopick/prod/aws      access-key-id=<value> secret-access-key=<value>"
    echo "  '"
}

# -----------------------------------------------------------------------------
# manual_instructions
# -----------------------------------------------------------------------------
manual_instructions() {
    echo "=== Manual Secret Creation Instructions ==="
    echo

    check_prerequisites
    check_env_file

    log_info "To create the secret manually from .env.prod:"
    echo
    echo "  kubectl create secret generic neopick-api-secret \\"
    echo "    --from-env-file=.env.prod \\"
    echo "    --namespace=default"
    echo
    log_warn "WARNING: This stores secrets in plaintext in etcd."
    log_warn "For production, use one of the following secure approaches:"
    echo
    echo "  1. Sealed Secrets (recommended for GitOps):"
    echo "     ./scripts/secrets-setup.sh sealed"
    echo
    echo "  2. External Secrets Operator (AWS Secrets Manager):"
    echo "     ./scripts/secrets-setup.sh eso"
    echo
    echo "  3. HashiCorp Vault:"
    echo "     ./scripts/secrets-setup.sh vault"
    echo
    echo "  See docs/SECRETS.md for detailed documentation."
}

# =============================================================================
# Main
# =============================================================================
main() {
    echo
    echo "=================================================="
    echo "  NEOPick Secrets Management Setup"
    echo "=================================================="
    echo

    local mode="${1:-manual}"

    case "$mode" in
        sealed)
            setup_sealed_secret
            ;;
        eso)
            setup_external_secrets_operator
            ;;
        vault)
            setup_vault
            ;;
        manual|help|-h|--help)
            manual_instructions
            ;;
        *)
            log_error "Unknown mode: $mode"
            log_info  "Usage: $0 [sealed|eso|vault|manual]"
            exit 1
            ;;
    esac
}

main "$@"
