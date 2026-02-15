package com.innercircle.sacco.security.service;

import com.innercircle.sacco.security.entity.UserAccount;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtService {

    @Value("${oauth2.jwt.rsa.public-key:}")
    private String rsaPublicKeyPem;

    @Value("${oauth2.jwt.rsa.private-key:}")
    private String rsaPrivateKeyPem;

    private NimbusJwtEncoder jwtEncoder;
    private NimbusJwtDecoder jwtDecoder;

    @PostConstruct
    public void init() {
        KeyPair keyPair;
        if (!rsaPublicKeyPem.isBlank() && !rsaPrivateKeyPem.isBlank()) {
            log.info("Loading RSA keys from configuration");
            keyPair = loadRsaKeyFromConfig();
        } else {
            log.warn("RSA keys not configured — generating ephemeral key pair (dev mode only)");
            keyPair = generateRsaKey();
        }

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        // Build RSAKey for encoder
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        // Initialize encoder and decoder
        this.jwtEncoder = new NimbusJwtEncoder((jwkSelector, securityContext) -> jwkSelector.select(new com.nimbusds.jose.jwk.JWKSet(rsaKey)));
        this.jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    /**
     * Generates an access token (JWT) for the given user.
     * Token contains: sub (username), userId, email, roles, authorities.
     * Expiry: 1 hour. Issuer: innercircle-sacco
     */
    public String generateAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(3600); // 1 hour

        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        Set<String> authorities = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .collect(Collectors.toSet());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("innercircle-sacco")
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("authorities", authorities)
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Generates a secure refresh token (32-byte hex string, NOT a JWT).
     */
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Decodes and validates a JWT token.
     */
    public Jwt decodeToken(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * Exposes the JwtDecoder for SecurityConfig to use as a bean.
     */
    public NimbusJwtDecoder getJwtDecoder() {
        return jwtDecoder;
    }

    private KeyPair loadRsaKeyFromConfig() {
        try {
            String publicKeyContent = rsaPublicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            String privateKeyContent = rsaPrivateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA keys from configuration", ex);
        }
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
