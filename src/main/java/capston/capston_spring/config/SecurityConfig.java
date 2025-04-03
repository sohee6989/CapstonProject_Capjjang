package capston.capston_spring.config;

import capston.capston_spring.jwt.JWTFilter;
import capston.capston_spring.jwt.JWTUtil;
import capston.capston_spring.jwt.LoginFilter;
import capston.capston_spring.oauth2.OAuth2AuthenticationSuccessHandler;
import capston.capston_spring.repository.UserRepository;
import capston.capston_spring.service.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AllArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OAuth2 로그인
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    //AuthenticationManager가 인자로 받을 AuthenticationConfiguraion 객체 생성자 주입
    private final AuthenticationConfiguration authenticationConfiguration;
    private final ObjectMapper objectMapper;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;



    // AuthenticationManager Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() { // security를 적용하지 않을 리소스
        return web -> web.ignoring()
                // error endpoint를 열어줘야 함, favicon.ico 추가!
                .requestMatchers("/error", "/favicon.ico");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        // CSRF, CORS -> JWT방식을 사용하므로 disable로 설정
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(Customizer.withDefaults());


        // 폼 로그인 방식, http basic 인증 방식 disable
        http.httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);


        // Session Stateless 설정
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));


        // 권한 규칙 작성
        /** 게스트 모드 활성화 -> user에 대한 요청을 athenticated 처리**/
        http
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        .requestMatchers("/join",  "/", "/login", "/login/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-resources/**").permitAll() // 회원가입, 로그인 요청 모두 접근 허가
                        .requestMatchers("/admin/**").hasRole("ADMIN")  // ADMIN만 접근 가능
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN") // USER, ADMIN 모두 접근 가능
                        .anyRequest().permitAll() // 그 외 모든 요청 인증처리
                )
                .exceptionHandling((exceptionConfig) -> exceptionConfig
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))); // 인증되지 않은 사용자 처리 -> 401 error


        // oauth2 설정
        http
                .oauth2Login(oauth -> oauth // OAuth2 로그인 기능에 대한 여러 설정의 진입점
                // OAuth2 로그인 성공 이후 사용자 정보를 가져올 때의 설정을 담당
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 로그인 성공 시 핸들러
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        // 로그인 실패 시 핸들러
                        .failureHandler((request, response, exception) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth2 로그인 실패: " + exception.getMessage());
                        })
        );

        // jwt 필터 추가
        http
                .addFilterBefore(new JWTFilter(jwtUtil, userRepository), LoginFilter.class);


        // 필터 추가
        // LoginFilter()는 인자를 받음 (AuthenticationManager() 메소드에 authenticationConfiguration 객체를 넣어야 함) 따라서 등록 필요
        System.out.println("Registering LoginFilter");
        LoginFilter loginFilter = new LoginFilter(authenticationManager(authenticationConfiguration), objectMapper, jwtUtil);
        //loginFilter.setFilterProcessesUrl("/user/login");
        http
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
        System.out.println("LoginFilter is working");

        http
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write("{\"message\": \"로그아웃되었습니다. 클라이언트에서 JWT를 삭제하세요.\"}");
                        })
                        .permitAll()
                );

        // 리턴
        return http.build();
    }


}
