package com.precious.finance_tracker.configurations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${security.jwt.jwt-secret}")
    private String secretKey;

    @Value("${security.jwt.jwt-expiry}")
    private long jwtExpiry;

    public Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(this.secretKey);

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractClaims(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(this.getSigningKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    public <T> T extractClaim(String jwt, Function<Claims, T> claimsResolver) {
        Claims claims = this.extractClaims(jwt);

        return claimsResolver.apply(claims);
    }

    public String extractUsername(String jwt) {
        return this.extractClaim(jwt, Claims::getSubject);
    }

    public String generateToken(
            String userEmail,
            Map<String, Object> extraClaims
    ) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userEmail)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + this.jwtExpiry))
                .signWith(this.getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateToken(String userEmail) {
        return this.generateToken(userEmail, new HashMap<>());
    }

    public boolean isTokenValid(String jwt, UserDetails userDetails) {
        return this.extractUsername(jwt).equals(userDetails.getUsername())
                && this.extractClaim(jwt, Claims::getExpiration).after(new Date(System.currentTimeMillis()));
    }
}
