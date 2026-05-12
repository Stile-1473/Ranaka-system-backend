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

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:2592000000}")
    private long refreshJwtExpiration;

    public String extractUsername(String token){

        // We use the JWT subject to store the user's email address
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims,T> claimsResolver){
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(String email){
        return generateToken(email, ACCESS_TOKEN_TYPE, jwtExpiration);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, REFRESH_TOKEN_TYPE, refreshJwtExpiration);
    }

    public boolean isAccessTokenValid(String token, String email){
        return isTokenValid(token, email, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token, String email) {
        return isTokenValid(token, email, REFRESH_TOKEN_TYPE);
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

    private String generateToken(String email, String tokenType, long expiration) {
        return Jwts.builder()
                .subject(email)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isTokenValid(String token, String email, String expectedType) {
        String username = extractUsername(token);
        String tokenType = extractClaim(token, claims -> claims.get(TOKEN_TYPE_CLAIM, String.class));

        return username.equals(email)
                && expectedType.equalsIgnoreCase(tokenType)
                && !isTokenExpired(token);
    }

    private SecretKey getSigningKey(){
        byte[] keyBytes = resolveSecretBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveSecretBytes() {
        String normalizedSecret = jwtSecret == null ? "" : jwtSecret.trim();
        if ((normalizedSecret.startsWith("\"") && normalizedSecret.endsWith("\""))
                || (normalizedSecret.startsWith("'") && normalizedSecret.endsWith("'"))) {
            normalizedSecret = normalizedSecret.substring(1, normalizedSecret.length() - 1).trim();
        }

        try {
            byte[] decoded = Decoders.BASE64.decode(normalizedSecret);
            if (decoded.length >= 32) {
                return decoded;
            }
        } catch (RuntimeException ignored) {
            // Plain text secrets are supported below for deployment platforms that store raw env values.
        }

        return sha256(normalizedSecret.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

}
