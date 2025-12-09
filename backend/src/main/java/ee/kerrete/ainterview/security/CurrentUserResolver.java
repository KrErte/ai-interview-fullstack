package ee.kerrete.ainterview.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentUser} annotated method parameters to {@link AuthenticatedUser}.
 *
 * <p>This resolver extracts the authenticated user from Spring Security's context
 * and injects it into controller method parameters.</p>
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
            && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return null;
        }

        Object principal = auth.getPrincipal();

        // New flow: principal is AuthenticatedUser
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        // Backward compatibility: principal might be email string
        if (principal instanceof String email) {
            // Create a minimal AuthenticatedUser without id/role
            return new AuthenticatedUser(null, email, null);
        }

        return null;
    }
}
