package ee.kerrete.ainterview.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    boolean existsByUserIdAndProductAndStatus(Long userId, String product, PurchaseStatus status);

    /** FP-61: the latest pending row of a user's product, reused by the next checkout. */
    Optional<Purchase> findFirstByUserIdAndProductAndStatusOrderByIdDesc(Long userId, String product, PurchaseStatus status);

    /** FP-62: the purchase of a Stripe Checkout session. */
    Optional<Purchase> findByStripeSessionId(String stripeSessionId);

    /** The purchase paid through a Stripe PaymentIntent (refund events name it). */
    Optional<Purchase> findByStripePaymentIntentId(String stripePaymentIntentId);
}
