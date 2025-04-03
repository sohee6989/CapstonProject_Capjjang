package capston.capston_spring.controller;

import capston.capston_spring.dto.JoinRequest;
import capston.capston_spring.dto.LoginRequest;
import capston.capston_spring.jwt.JWTUtil;
import capston.capston_spring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class AuthController {

    private final UserService userService;
    private final JWTUtil jwtUtil;


    /** 회원 가입 **/
    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody @Valid JoinRequest joinRequest, BindingResult bindingResult) {
        // 회원가입 폼 오류 (전체 필드 유효성 검사)
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errorMessages);
        }

        // 비밀번호 확인 불일치
        if (!joinRequest.getPassword1().equals(joinRequest.getPassword2())) {
            return ResponseEntity.badRequest().body("The passwords do not match.");
        }

        // 사용자 생성
        try {
            this.userService.create(joinRequest.getName(), joinRequest.getEmail(), joinRequest.getPassword1());
        } catch (DataIntegrityViolationException e) {
            // 중복 사용자 오류 처리
            e.printStackTrace();
            return ResponseEntity.badRequest().body("User already exists.");
        } catch (Exception e) {
            // 다른 예외 처리
            e.printStackTrace();
            return ResponseEntity.badRequest().body("An unexpected error occurred: " + e.getMessage());
        }

        return ResponseEntity.ok().body("User successfully registered.");
    }

    /** 삭제 예정 **/
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest, BindingResult bindingResult) {
        // 로그인 폼 오류 (전체 필드 유효성 검사)
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errorMessages);
        }

        return ResponseEntity.ok().body("The token in the header was verified.");
    }

    @GetMapping({"/loginNaver", "/loginGoogle"})
    public ModelAndView loginOauth(){
        ModelAndView modelAndView = new ModelAndView("socialLogin"); // "socialLogin" 뷰를 반환
        return modelAndView;
    }


    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestHeader("Refresh-Token") String refreshToken){
        if(jwtUtil.isExpired(refreshToken)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token expired"));
        }

        String email = jwtUtil.getEmail(refreshToken);
        String newAccessToken = jwtUtil.createJwt(email, "USER", 60 * 60 * 1000L);

        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

}
