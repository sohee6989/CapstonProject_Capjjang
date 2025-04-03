package capston.capston_spring.jwt;

import capston.capston_spring.dto.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

@AllArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final ObjectMapper objectMapper;
    private final JWTUtil jwtUtil;



    /**
     * 인증 처리 메소드
     *
     * UsernamePasswordAuthenticationFilter와 동일하게 UsernamePasswordAuthenticationToken 사용
     * StreamUtils를 통해 request에서 messageBody(JSON) 반환
     * 요청 JSON Example
     * {
     *    "email" : "aaa@bbb.com"
     *    "password" : "test123"
     * }
     * 꺼낸 messageBody를 objectMapper.readValue()로 Map으로 변환 (Key : JSON의 키 -> email, password)
     * Map의 Key(email, password)로 해당 이메일, 패스워드 추출 후
     * UsernamePasswordAuthenticationToken의 파라미터 principal, credentials에 대입
     *
     * AbstractAuthenticationProcessingFilter(부모)의 getAuthenticationManager()로 AuthenticationManager 객체를 반환 받은 후
     * authenticate()의 파라미터로 UsernamePasswordAuthenticationToken 객체를 넣고 인증 처리
     * (여기서 AuthenticationManager 객체는 ProviderManager -> SecurityConfig에서 설정)
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

            System.out.println("Request Body: " + messageBody);  // 메시지 바디 출력

            // 요청 본문이 비어있는지 확인
            if (messageBody == null || messageBody.isEmpty()) {
                throw new RuntimeException("Request body is empty");
            }

            Map<String, String> usernamePasswordMap = this.objectMapper.readValue(messageBody, Map.class);

            // 이메일과 비밀번호 추출
            String email = usernamePasswordMap.get("email");
            String password = usernamePasswordMap.get("password");

            if (email == null || password == null) {
                throw new RuntimeException("Email or password is missing");
            }


            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password);

            // AuthenticationManager로 인증 위임
            return this.authenticationManager.authenticate(authToken);
        } catch (IOException e) {  // AuthenticationException은 래핑하지 않고 그대로 던지기 -> 부모 클래스에서 실패 처리를 감지하지 못하고, unsuccessfulAuthentication이 호출되지 않는 문제가 생기기 때문
            throw new RuntimeException("IO failed", e);
        }
    }

    //로그인 성공시 실행하는 메소드 (여기서 JWT를 발급하면 됨)
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {
        // 1. 인증된 사용자 정보 가져오기
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = customUserDetails.getEmail();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String role = authorities.iterator().next().getAuthority();

        // 액세스 토큰 (1시간 유효)
        String accessToken = jwtUtil.createJwt(email, role, 60*60*1000L);
        // 리스레시 토큰 (1일 유효)
        String refreshToken = jwtUtil.createRefreshToken(email, 24*60*60*1000L);
        response.addHeader("Authorization", "Bearer " + accessToken);
        response.addHeader("Refresh-Token", refreshToken);
    }

    //로그인 실패시 실행하는 메소드
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        //response.setStatus(401);

        System.out.println("Login failed: " + failed.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized

    }


}
