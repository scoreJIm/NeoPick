# NEOPick Secrets Management

> Security documentation for managing secrets in NEOPick deployments.  
> Last updated: 2026-08-12

---

## Overview

NEOPick uses Kubernetes Secrets to inject sensitive configuration into the application. The `deployment.yaml` in this repository contains a **plaintext Secret for local development only**. Production deployments must use one of the three approaches described below.

**Never commit plaintext secrets to version control.**

### Available Approaches

| Approach | GitOps Safe | Auto-Rotate | External Provider | Complexity |
|---|---|---|---|---|
| Sealed Secrets | Yes | No (manual rotation) | No | Low |
| External Secrets Operator (ESO) | Yes | Yes (auto-refresh) | AWS SM / GCP SM / Vault | Medium |
| HashiCorp Vault + ESO | Yes | Yes (auto-refresh) | Vault | High |

---

## Approach 1: Sealed Secrets (Recommended for GitOps)

**Concept:** Encrypt the entire Kubernetes Secret with a cluster-specific key. The encrypted (sealed) secret is safe to commit to Git. Only the Sealed Secrets controller running in the target cluster can decrypt it.

### Architecture

```
Developer encrypts secrets  -->  Commit SealedSecret YAML to Git  -->  ArgoCD/Flux applies
                                                                       |
                                                              Sealed Secrets Controller
                                                                       |
                                                              Kubernetes Secret (decrypted)
                                                                       |
                                                              Application pods mount via secretRef
```

### Setup

#### Step 1: Install kubeseal CLI

```bash
# macOS
brew install kubeseal

# Linux (manual)
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.26.3/kubeseal-0.26.3-linux-amd64.tar.gz
tar -xzf kubeseal-*.tar.gz kubeseal
sudo mv kubeseal /usr/local/bin/
```

#### Step 2: Install Sealed Secrets Controller

```bash
helm repo add sealed-secrets https://bitnami-labs.github.io/sealed-secrets
helm repo update
helm install sealed-secrets sealed-secrets/sealed-secrets \
  --namespace kube-system
```

#### Step 3: Create .env.prod

Create a file at the repository root named `.env.prod` (this file is `.gitignore`d and must never be committed):

```env
DB_USERNAME=neopick_prod_user
DB_PASSWORD=super_secret_db_password
REDIS_PASSWORD=super_secret_redis_password
JWT_SECRET=your-256-bit-jwt-secret-key
ALIYUN_ACCESS_KEY=LTAI5tYourAccessKey
ALIYUN_SECRET_KEY=your_secret_key_here
WECHAT_APP_ID=wx1234567890abcdef
WECHAT_API_KEY=your_wechat_api_key
ALIPAY_APP_ID=2021000000000000
ALIPAY_PRIVATE_KEY=MIIEvQIBADANBg...your_private_key
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/...your_aws_secret
```

#### Step 4: Generate SealedSecret

```bash
# Using the helper script (easiest)
./scripts/secrets-setup.sh sealed

# Or manually
kubectl create secret generic neopick-api-secret \
  --from-env-file=.env.prod \
  --dry-run=client -o yaml | \
  kubeseal --format yaml > k8s/sealed-secret.yaml
```

#### Step 5: Deploy

```bash
# Apply the SealedSecret
kubectl apply -f k8s/sealed-secret.yaml

# Verify the Secret was created
kubectl get secret neopick-api-secret -o yaml
```

#### Step 6: Enable in Kustomization

In `k8s/kustomization.yaml`, add `- sealed-secret.yaml` to the resources list and remove or comment out the `secretGenerator` block.

---

## Approach 2: External Secrets Operator (AWS Secrets Manager)

**Concept:** The External Secrets Operator (ESO) syncs secrets from an external provider (AWS Secrets Manager, GCP Secret Manager, Azure Key Vault, etc.) into Kubernetes Secrets. Applications reference the Kubernetes Secret as normal.

### Architecture

```
AWS Secrets Manager  <--sync--  External Secrets Operator  -->  Kubernetes Secret  -->  Application pods
```

### Setup

#### Step 1: Install ESO

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm repo update
helm install external-secrets external-secrets/external-secrets \
  --namespace external-secrets-system \
  --create-namespace
```

#### Step 2: Create AWS Credentials Secret

```bash
kubectl create secret generic aws-credentials \
  --from-literal=access-key-id=<YOUR_AWS_ACCESS_KEY> \
  --from-literal=secret-access-key=<YOUR_AWS_SECRET_KEY>
```

The IAM user associated with these credentials needs:
- `secretsmanager:GetSecretValue`
- `secretsmanager:DescribeSecret`

#### Step 3: Populate AWS Secrets Manager

Create secrets in AWS Secrets Manager with the following structure:

| AWS Secret Name | Properties |
|---|---|
| `neopick/prod/database` | `username`, `password` |
| `neopick/prod/redis` | `password` |
| `neopick/prod/jwt` | `secret` |
| `neopick/prod/sms` | `access-key`, `secret-key` |
| `neopick/prod/wechat` | `app-id`, `api-key` |
| `neopick/prod/alipay` | `app-id`, `private-key` |
| `neopick/prod/aws` | `access-key-id`, `secret-access-key` |

Example with AWS CLI:

```bash
aws secretsmanager create-secret \
  --name neopick/prod/database \
  --secret-string '{"username":"dbuser","password":"dbpassword"}' \
  --region us-east-1

aws secretsmanager create-secret \
  --name neopick/prod/jwt \
  --secret-string '{"secret":"your-jwt-secret"}' \
  --region us-east-1

# ... repeat for all keys
```

#### Step 4: Apply Kubernetes Resources

```bash
# Apply the SecretStore (tells ESO how to authenticate with AWS)
kubectl apply -f k8s/secret-store.yaml

# Apply the ExternalSecret (tells ESO what to sync)
kubectl apply -f k8s/external-secret.yaml
```

#### Step 5: Verify Sync

```bash
# Check ExternalSecret status
kubectl get externalsecret neopick-api-external-secret

# Verify the Kubernetes Secret was created
kubectl get secret neopick-api-secret

# Check ESO logs for sync details
kubectl logs -n external-secrets-system deploy/external-secrets
```

#### Step 6: Enable in Kustomization

In `k8s/kustomization.yaml`, add `- secret-store.yaml` and `- external-secret.yaml` to the resources list. Remove or comment out the `secretGenerator` block.

---

## Approach 3: HashiCorp Vault + ESO

**Concept:** Use HashiCorp Vault as the central secrets backend. ESO bridges Vault and Kubernetes.

### Architecture

```
HashiCorp Vault  <--sync--  ESO  -->  Kubernetes Secret  -->  Application pods
```

### Setup

#### Step 1: Install Vault

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update

# Production: enable HA, auto-unseal, and raft storage
helm install vault hashicorp/vault \
  --namespace vault \
  --create-namespace \
  --set "server.ha.enabled=true" \
  --set "server.ha.raft.enabled=true"
```

#### Step 2: Configure Kubernetes Auth

```bash
kubectl exec -n vault vault-0 -- vault auth enable kubernetes

kubectl exec -n vault vault-0 -- vault write auth/kubernetes/config \
  kubernetes_host="https://kubernetes.default.svc"
```

#### Step 3: Store Secrets in Vault

```bash
kubectl exec -n vault vault-0 -- sh -c '
vault kv put secret/neopick/prod/database username=<value> password=<value>
vault kv put secret/neopick/prod/redis    password=<value>
vault kv put secret/neopick/prod/jwt      secret=<value>
vault kv put secret/neopick/prod/sms      access-key=<value> secret-key=<value>
vault kv put secret/neopick/prod/wechat   app-id=<value> api-key=<value>
vault kv put secret/neopick/prod/alipay   app-id=<value> private-key=<value>
vault kv put secret/neopick/prod/aws      access-key-id=<value> secret-access-key=<value>
'
```

#### Step 4: Create Vault SecretStore and ExternalSecret

Create a SecretStore pointing to Vault (adjust server URL and auth method as needed):

```yaml
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
        kubernetes:
          mountPath: "kubernetes"
          role: "neopick-api"
```

#### Step 5: Run the Helper Script

```bash
./scripts/secrets-setup.sh vault
```

---

## Rotating Secrets

### Sealed Secrets Rotation

1. Update the value in `.env.prod`
2. Re-encrypt and update the SealedSecret:
   ```bash
   kubectl create secret generic neopick-api-secret \
     --from-env-file=.env.prod \
     --dry-run=client -o yaml | \
     kubeseal --format yaml > k8s/sealed-secret.yaml
   kubectl apply -f k8s/sealed-secret.yaml
   ```
3. Restart pods to pick up new values:
   ```bash
   kubectl rollout restart deployment/neopick-api
   ```

### ESO Rotation

1. Update the secret value in AWS Secrets Manager (or Vault)
2. ESO will pick up the change on the next `refreshInterval` (default: 1 hour)
3. Trigger an immediate refresh:
   ```bash
   kubectl annotate externalsecret neopick-api-external-secret \
     force-sync="$(date +%s)" --overwrite
   ```
4. Restart pods if the application caches secret values:
   ```bash
   kubectl rollout restart deployment/neopick-api
   ```

### Emergency Secret Rotation

If a secret is compromised, you must rotate it immediately:

```bash
# 1. Rotate the secret at the source (AWS SM / Vault / .env.prod)

# 2. Force ESO refresh (if using ESO)
kubectl annotate externalsecret neopick-api-external-secret \
  force-sync="$(date +%s)" --overwrite

# 3. If using Sealed Secrets, re-encrypt and apply
./scripts/secrets-setup.sh sealed && kubectl apply -f k8s/sealed-secret.yaml

# 4. Force restart all pods (critical to pick up new secrets)
kubectl rollout restart deployment/neopick-api

# 5. Verify new values are loaded
kubectl logs -l app=neopick-api | grep -i "started"
```

---

## Local Development

For local development, use the `.env` file approach with Docker Compose or direct JVM args. The plaintext Secret in `deployment.yaml` uses `${VAR}` placeholders and is replaced at deploy time by the CI/CD pipeline or `envsubst`.

### Local .env File

Create `.env` in the project root (this file is `.gitignore`d):

```env
DB_USERNAME=neopick_dev
DB_PASSWORD=dev_password
REDIS_PASSWORD=
JWT_SECRET=dev-secret-do-not-use-in-prod
# ... other keys
```

The Spring Boot application loads these via the `dotenv` library (already configured in `application.yml`).

### Running Locally

```bash
# With Maven
./mvnw spring-boot:run

# With Docker Compose
docker compose up -d
```

### Important: Never Commit Secrets

Always verify before committing:

```bash
# Check what would be committed
git diff --staged

# The .gitignore should already exclude:
#   .env
#   .env.prod
#   *.pem
#   credentials.json
```

---

## File Reference

| File | Purpose |
|---|---|
| `k8s/deployment.yaml` | Deployment + dev-only plaintext Secret |
| `k8s/sealed-secret.yaml` | SealedSecret template (encrypted, safe to commit) |
| `k8s/external-secret.yaml` | ExternalSecret config for ESO |
| `k8s/secret-store.yaml` | SecretStore pointing to AWS Secrets Manager |
| `scripts/secrets-setup.sh` | Helper script for all 3 approaches |
| `docs/SECRETS.md` | This documentation |

---

## Security Checklist

- [ ] Plaintext .env files are in `.gitignore`
- [ ] `.env.prod` is never committed to the repository
- [ ] Production cluster uses Sealed Secrets or ESO
- [ ] IAM roles for ESO follow least-privilege principle
- [ ] Secret rotation process is documented and tested
- [ ] Audit logs are enabled on the external secret provider
- [ ] Kubernetes RBAC restricts access to Secrets in the namespace
- [ ] Network policies restrict pod-to-pod communication
