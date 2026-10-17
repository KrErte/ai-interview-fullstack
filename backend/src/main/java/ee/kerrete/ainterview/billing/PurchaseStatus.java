package ee.kerrete.ainterview.billing;

/** Stored lowercase in purchase.status. Only {@link #PAID} grants an entitlement. */
public enum PurchaseStatus {
    PENDING, PAID, REFUNDED;

    public String code() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static PurchaseStatus fromCode(String code) {
        return valueOf(code.toUpperCase(java.util.Locale.ROOT));
    }
}
