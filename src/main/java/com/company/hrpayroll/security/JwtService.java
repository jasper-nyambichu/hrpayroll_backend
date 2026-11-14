// security/JwtService.java
package com.company.hrpayroll.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class
JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    // The value written in application.yaml as the fallback. Anyone with the source code knows it, so a
    // server using it is warned at every start until JWT_SECRET is set in its .env (with this same value
    // first, if nobody should be logged out; a new value later signs everyone out once).
    private static final String PUBLIC_DEFAULT = "24hx2bKUfYqv6pY0ydoUW9FdDQIpPoesNaH62ICiiBk=";

    @jakarta.annotation.PostConstruct
    void warnIfDefaultSecret() {
        if (PUBLIC_DEFAULT.equals(secret)) {
            org.slf4j.LoggerFactory.getLogger(JwtService.class).warn(
                    "JWT_SECRET is not set: login tokens are signed with the default key from the source code. "
                            + "Set JWT_SECRET in this server's .env (openssl rand -base64 32).");
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, accessTokenExpiryMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshTokenExpiryMs);
    }

    private String buildToken(UserDetails userDetails, long expiryMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}