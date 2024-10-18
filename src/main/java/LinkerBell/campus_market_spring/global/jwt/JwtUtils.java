package LinkerBell.campus_market_spring.global.jwt;

import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Value("${jwt.access.expiration}")
    private Long accessExpiredTime;
    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiredTime;

    public String generateAccessToken(String email, Role role) {
        Claims claims = Jwts.claims();
        claims.put("email", email);
        claims.put("role", role.getKey());

        return Jwts.builder()
            .setSubject(email)
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + accessExpiredTime))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshToken(String email, Role role) {
        Claims claims = Jwts.claims();
        claims.put("email", email);
        claims.put("role", role.getKey());

        return Jwts.builder()
            .setSubject(email)
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshExpiredTime))
            .signWith(getKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        if (token == null) {
            throw new CustomException(ErrorCode.JWT_IS_NULL);
        }
        try {
            return !isExpired(token);
        } catch (MalformedJwtException e) {
            throw new CustomException(ErrorCode.INVALID_JWT);
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.EXPIRED_JWT);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        log.info("Authorization header token is : " + token);
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        Set<GrantedAuthority> authorities = new HashSet<>();
        String role = (String) claims.get("role");
        authorities.add(new SimpleGrantedAuthority(role));

        UserDetails principal = new User(claims.get("email").toString(), "", authorities);

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
    }

    private boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private Key getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }
}
