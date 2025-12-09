package ee.kerrete.ainterview.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the current authenticated user into controller methods.
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code @GetMapping("/profile")}
 * public ProfileDto getProfile(@CurrentUser AuthenticatedUser user) {
 *     return profileService.getByUserId(user.id());
 * }
 * </pre>
 *
 * <p>The annotation resolves to an {@link AuthenticatedUser} record containing
 * the user's id, email, and role extracted from the JWT token.</p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
