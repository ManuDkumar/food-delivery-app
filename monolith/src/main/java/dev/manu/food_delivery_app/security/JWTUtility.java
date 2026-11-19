package dev.manu.food_delivery_app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import dev.manu.food_delivery_app.entity.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTUtility {


    private final SecretKey key;
    private final long expiration;

    public JWTUtility(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }


    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token).getBody();
    }

    public boolean validateToken(UserDetails userDetails, String token) {
        try {
            Claims claims = parseToken(token);
            return  claims.getSubject().equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().before(new Date());
    }
}
