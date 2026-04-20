package com.example.uaa.controller;

import com.example.uaa.config.SecurityConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final SecurityConfig.RSAKeyHolder rsaKeyHolder;
    private final String issuer;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            SecurityConfig.RSAKeyHolder rsaKeyHolder,
            @org.springframework.beans.factory.annotation.Value("${app.issuer:http://localhost:9000}") String issuer) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.rsaKeyHolder = rsaKeyHolder;
        this.issuer = issuer;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        var jwk = rsaKeyHolder.getPublicJWK();
        return Map.of("keys", java.util.List.of(jwk.toJSONObject()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "client_id", defaultValue = "public-client") String clientId) {

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            List<String> roles = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .collect(Collectors.toList());

            Instant now = Instant.now();
            Instant exp = now.plusSeconds(7200);

            String scopeStr = "read write user product:read product:write product:delete";

            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .subject(username)
                    .issuer(issuer)
                    .issuedAt(now)
                    .expiresAt(exp)
                    .audience(List.of("public-client"))
                    .claim("username", username)
                    .claim("roles", roles)
                    .claim("scope", scopeStr)
                    .build();

            Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("access_token", jwt.getTokenValue());
            response.put("token_type", "Bearer");
            response.put("expires_in", 7200);
            response.put("scope", scopeStr);
            response.put("refresh_token", UUID.randomUUID().toString());
            response.put("username", username);
            response.put("roles", roles);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("error", "invalid_grant");
            error.put("error_description", "Invalid username or password");
            return ResponseEntity.status(401).body(error);
        }
    }

    @PostMapping("/auth/oauth2/success")
    public ResponseEntity<?> oauth2Success(
            @RequestParam("username") String username,
            @RequestParam("roles") String rolesParam) {

        List<String> roles = Arrays.stream(rolesParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<GrantedAuthority> authorities = roles.stream()
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(7200);
        String scopeStr = "read write user product:read product:write product:delete";

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .audience(List.of("public-client"))
                .claim("username", username)
                .claim("roles", roles)
                .claim("scope", scopeStr)
                .build();

        Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", jwt.getTokenValue());
        response.put("token_type", "Bearer");
        response.put("expires_in", 7200);
        response.put("scope", scopeStr);
        response.put("refresh_token", UUID.randomUUID().toString());
        response.put("username", username);
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }
}
