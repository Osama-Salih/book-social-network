package com.osama.book.auth;

import com.osama.book.auth.request.AuthenticationRequest;
import com.osama.book.auth.response.AuthenticationResponse;
import com.osama.book.auth.request.RegisterRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Void> register(
            @RequestBody
            @Valid
            final RegisterRequest request) throws MessagingException {
        this.authService.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody
            @Valid
            final AuthenticationRequest request) {
        return ResponseEntity.ok(this.authService.login(request));
    }

    @GetMapping("/activate-account")
    public void confirm(final @RequestParam String token) throws MessagingException {
        this.authService.activateAccount(token);
    }
}
