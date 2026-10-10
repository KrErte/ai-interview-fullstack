package ee.kerrete.ainterview.model;

/**
 * Product analytics event types. {@link #code()} is the value stored in product_event.event_type.
 */
public enum ProductEventType {
    /** A roadmap/learning plan was created for the user. */
    PLAN_GENERATED("plan_generated"),
    /** A roadmap task changed from not completed to completed. */
    TASK_COMPLETED("task_completed");

    private final String code;

    ProductEventType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
