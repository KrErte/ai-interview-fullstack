package ee.krerte.aiinterview.auth;

import ee.krerte.aiinterview.auth.dto.LoginRequest;
import ee.krerte.aiinterview.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ee.krerte.aiinterview.auth.AuthResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/register")
    public ee.krerte.aiinterview.auth.AuthResponse register(@RequestBody RegisterRequest req) {
        return authService.register(req);
    }
}
