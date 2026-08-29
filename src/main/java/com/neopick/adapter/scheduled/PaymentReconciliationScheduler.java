package com.neopick.adapter.scheduled;

import com.neopick.adapter.persistence.entity.PaymentJpaEntity;
import com.neopick.adapter.persistence.repository.PaymentJpaRepository;
import com.neopick.domain.payment.PaymentStatus;
import com.neopick.port.payment.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Daily scheduled task that queries Alipay for pending payments to reconcile
 * any payments that may have succeeded but whose callbacks were missed.
 */
@Component
public class PaymentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentGateway paymentGateway;

    public PaymentReconciliationScheduler(PaymentJpaRepository paymentJpaRepository,
                                           PaymentGateway paymentGateway) {
        this.paymentJpaRepository = paymentJpaRepository;
        this.paymentGateway = paymentGateway;
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void reconcilePendingPayments() {
        log.info("Starting daily payment reconciliation...");
        List<PaymentJpaEntity> pendingPayments = paymentJpaRepository
                .findByStatus(PaymentStatus.PENDING.name());

        int reconciled = 0;
        for (PaymentJpaEntity entity : pendingPayments) {
            try {
                PaymentGateway.QueryResult result =
                        paymentGateway.queryPayment(entity.getId().toString());

                if (result.success() && ("TRADE_SUCCESS".equals(result.tradeStatus())
                        || "TRADE_FINISHED".equals(result.tradeStatus()))) {
                    log.warn("Reconciliation: payment {} is PAID on gateway but PENDING locally - "
                            + "callback may have been missed", entity.getId());
                    reconciled++;
                }
            } catch (Exception e) {
                log.error("Reconciliation query failed for payment {}: {}",
                        entity.getId(), e.getMessage());
            }
        }

        log.info("Payment reconciliation complete: {} pending payments checked, {} anomalies found",
                pendingPayments.size(), reconciled);
    }
}
