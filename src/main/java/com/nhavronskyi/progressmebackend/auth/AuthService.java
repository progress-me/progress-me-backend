package com.nhavronskyi.progressmebackend.auth;

import com.nhavronskyi.progressmebackend.auth.dto.AuthResponse;
import com.nhavronskyi.progressmebackend.auth.dto.LoginRequest;
import com.nhavronskyi.progressmebackend.auth.dto.RegisterRequest;
import com.nhavronskyi.progressmebackend.persona.Persona;
import com.nhavronskyi.progressmebackend.persona.PersonaRepository;
import com.nhavronskyi.progressmebackend.user.User;
import com.nhavronskyi.progressmebackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new com.nhavronskyi.progressmebackend.common.EmailAlreadyExistsException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user = userRepository.save(user);

        Persona persona = new Persona();
        persona.setUserId(user.getId());
        personaRepository.save(persona);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user.getId()), refreshToken.getToken(),
                user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return new AuthResponse(jwtService.generateToken(user.getId()), refreshToken.getToken(),
                user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(refreshTokenValue);
        User user = newRefreshToken.getUser();
        return new AuthResponse(jwtService.generateToken(user.getId()), newRefreshToken.getToken(),
                user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }
}
