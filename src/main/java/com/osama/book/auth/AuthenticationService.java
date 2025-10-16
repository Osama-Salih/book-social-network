package com.osama.book.auth;

import com.osama.book.auth.request.AuthenticationRequest;
import com.osama.book.auth.response.AuthenticationResponse;
import com.osama.book.auth.request.RegisterRequest;
import com.osama.book.email.EmailService;
import com.osama.book.email.EmailTemplateName;
import com.osama.book.role.RoleRepository;
import com.osama.book.security.JwtService;
import com.osama.book.user.Token;
import com.osama.book.user.TokenRepository;
import com.osama.book.user.User;
import com.osama.book.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.mailing.frontend.activation_url}")
    private String activationUrl;

    public void register(final RegisterRequest request) throws MessagingException {
        var userRole = this.roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER was not initialized"));

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(this.passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        this.userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(final User user) throws MessagingException {
        final String newToken = generateAndSaveActivationToken(user);
        this.emailService.sendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Activate account");
    }

    private String generateAndSaveActivationToken(final User user) {
        final String generatedCode = generateActivationCode(6);
        final Token token = Token.builder()
                .token(generatedCode)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        this.tokenRepository.save(token);
        return generatedCode;
    }

    private String generateActivationCode(final int length) {
        final String characters = "0123456789";
        final StringBuilder codeBuilder = new StringBuilder();
        final SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    public AuthenticationResponse login(final AuthenticationRequest request) {
        final Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var claims = new HashMap<String, Object>();
        User user = (User) auth.getPrincipal();
        claims.put("fullname", user.fullName());
        String token = jwtService.generateToken(claims, user);

        return AuthenticationResponse.builder()
                .token(token).build();
    }
}
