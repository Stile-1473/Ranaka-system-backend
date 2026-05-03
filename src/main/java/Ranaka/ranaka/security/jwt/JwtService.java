package Ranaka.ranaka.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token){

        // We use the JWT subject to store the user's email address
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String email){
        // Example subject: requester1@ranaka.org
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }


    public boolean isTokenValid(String token,String email){
        String username = extractUsername(token);
        return username.equals(email) && !isTokenExpired(token);
    }
    private boolean isTokenExpired(String token){
        Date expirationDate = extractClaim(token,Claims::getExpiration);
        return expirationDate.before(new Date());
    }

    private Claims extractAllClaims(String token){
        // All REST and WebSocket authentication flows ultimately trust this same signature check
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey(){
        byte[] keyBytes = resolveSecretBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveSecretBytes() {
        try {
            byte[] decoded = Decoders.BASE64.decode(jwtSecret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Plain text secrets are supported below for deployment platforms that store raw env values.
        }

        return sha256(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

}
