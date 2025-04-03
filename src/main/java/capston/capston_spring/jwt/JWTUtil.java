package capston.capston_spring.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JWTUtil {

    private final Key key;


    public JWTUtil(@Value("${spring.jwt.secret}") String secret){
        byte[] byteSecreteKey = Decoders.BASE64.decode(secret);
        key = Keys.hmacShaKeyFor(byteSecreteKey);
    }

    public String getEmail(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("email", String.class);
    }

    public String getRole(String token){
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("role", String.class);
    }

    /** 토큰 만료 **/
    public Boolean isExpired(String token) {

        //return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().getExpiration().before(new Date());

        try {
            // JWT 파싱
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key) // key는 JWT 서명에 사용된 키
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 만료 시간 확인
            Date expiration = claims.getExpiration();
            System.out.println("Token expiration: " + expiration); // 디버깅 로그 추가
            System.out.println("Current time: " + new Date()); // 현재 시간 출력

            return expiration.before(new Date()); // 현재 시간이 만료 시간보다 뒤에 있으면 true
        } catch (JwtException | IllegalArgumentException e) {
            // JWT 파싱 실패 시 예외 처리
            System.out.println("JWT parsing failed: " + e.getMessage());
            return true; // 토큰이 만료된 것으로 간주
        }
    }

    /** 토큰 생성 **/
    public String createJwt(String email, String role, Long expiredMs){
        Claims claims = Jwts.claims();
        claims.put("email", email);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 리프레시 토큰 생성 (유효기간: 1일) **/
    public String createRefreshToken(String email, Long expiredMs){
        Claims claims = Jwts.claims();
        claims.put("email", email);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
