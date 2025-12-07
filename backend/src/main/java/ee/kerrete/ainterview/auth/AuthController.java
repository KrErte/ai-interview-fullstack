package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.LoginRequest;
import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }
}
