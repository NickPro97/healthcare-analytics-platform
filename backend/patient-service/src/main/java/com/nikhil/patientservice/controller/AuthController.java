package com.nikhil.patientservice.controller;

import com.nikhil.patientservice.config.JwtUtil;
import com.nikhil.patientservice.dto.LoginRequest;
import com.nikhil.patientservice.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /** Bad credentials raise AuthenticationException, which GlobalExceptionHandler turns into 401. */
    @PostMapping({"/login", "/api/auth/login"})
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("CLINICIAN");

        String token = jwtUtil.generateToken(auth.getName(), role);
        return new LoginResponse(token, auth.getName(), role, jwtUtil.getExpirationMs() / 1000);
    }
}
