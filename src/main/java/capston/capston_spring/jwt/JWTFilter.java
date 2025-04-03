package capston.capston_spring.jwt;

import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Role;
import capston.capston_spring.exception.UserNotFoundException;
import capston.capston_spring.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * 스프링 시큐리티 filter chain에 요청에 담긴 JWT를 검증하기 위한 커스텀 필터를 등록
 * JWT 검증 및 SecurityContext에 인증 정보 세팅을 위한 필터.
 * 요청 헤더의 Authorization에서 JWT를 추출하여 검증하고 인증을 처리합니다.
 */
@AllArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        
        // 0. oauth2 요청은 무시
        String requestURI = request.getRequestURI();

        // OAuth2 요청은 필터링하지 않음
        if (requestURI.startsWith("/login/oauth2")) {
            filterChain.doFilter(request, response);
            return;
        }


        // 1. Authorization 헤더에서 토큰 추출
        String authorization = request.getHeader("Authorization");


        // 2. Authorization 헤더가 없거나 "Bearer"로 시작하지 않으면 필터 종료
        if (authorization == null || !authorization.startsWith("Bearer")) {
            filterChain.doFilter(request, response);
            return;
        }


        // 3. Bearer 부분 제거 후 순수 토큰만 획득
        String token = authorization.split(" ")[1];


        // 4. 토큰이 만료되었거나 블랙리스트에 있으면 종료
        if (jwtUtil.isExpired(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 토큰입니다.");
            filterChain.doFilter(request, response);
            return;
        }


        // 5. 토큰에서 사용자 정보 추출
        String email = jwtUtil.getEmail(token);
        String roleString = jwtUtil.getRole(token);

        
        // 6. DB에서 사용자 조회
        AppUser appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다."));


        // 7. Role 값을 enum으로 변환
        Role role = Role.valueOf(roleString.replace("ROLE_", ""));  // 예: "ROLE_ADMIN" -> Role.ADMIN
        

        // 8. CustomUserDetails 생성
        CustomUserDetails customUserDetails = new CustomUserDetails(appUser);


        // 9. 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, token, customUserDetails.getAuthorities());


        // 10. SecurityContext에 인증 정보 설정
        SecurityContextHolder.getContext().setAuthentication(authToken);


        // 11. 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}
