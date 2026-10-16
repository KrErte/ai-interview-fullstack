package ee.kerrete.ainterview.billing;

/**
 * Paid features. {@link #product()} is the purchase.product value that unlocks it. What a feature gates is decided
 * only in {@link EntitlementService#isLocked(Feature, String)}; callers only ask that question.
 */
public enum Feature {
    ROADMAP("roadmap");

    private final String product;

    Feature(String product) {
        this.product = product;
    }

    public String product() {
        return product;
    }
}
