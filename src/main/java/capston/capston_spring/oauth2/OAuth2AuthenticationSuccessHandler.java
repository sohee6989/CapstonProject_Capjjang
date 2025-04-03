package capston.capston_spring.oauth2;

import capston.capston_spring.jwt.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@AllArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = (String) oAuth2User.getAttributes().get("email");
        String role = (String) oAuth2User.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No authority found"))
                .getAuthority();

        // JWT 발급
        String accessToken = jwtUtil.createJwt(email, role, 60*60*1000L);
        String refreshToken = jwtUtil.createRefreshToken(email, 24*60*60*1000L);

        // 헤더에 JWT 추가
        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("Refresh-Token", refreshToken);

        // OAuth2 로그인 성공 후 프론트엔드 리디렉트 (필요 시) -> 프론트엔드와 연동이 잘 될지 확인 필요
        response.sendRedirect("http://localhost:8080/");
    }
}
