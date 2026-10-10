package ee.kerrete.ainterview.service;

import ee.kerrete.ainterview.model.ProductEventType;

import java.util.Map;

/**
 * Application event published by business code when a product analytics event happened.
 * {@link ProductEventService} writes it after the business transaction committed (or immediately
 * when there is no transaction), so analytics can never break or roll back the user's action and a
 * rolled-back action never leaves an event behind.
 */
public record ProductEventOccurred(String email, ProductEventType type, Map<String, ?> details) {
}
