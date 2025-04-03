package capston.capston_spring.controller;

import capston.capston_spring.dto.PasswordResetNewPasswordRequest;
import capston.capston_spring.dto.PasswordResetRequest;
import capston.capston_spring.dto.PasswordResetVerifyRequest;
import capston.capston_spring.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/password-reset")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    // 1. 이메일 입력 후 인증 코드 요청
    @PostMapping("/request")
    public ResponseEntity<String> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request, BindingResult bindingResult) {
        // 폼 형식 오류
        if (bindingResult.hasErrors()) {
            String errorMessages = bindingResult.getAllErrors().toString();
            return ResponseEntity.badRequest().body(errorMessages);
        }

        this.passwordResetService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("Verification code sent.");
    }


    // 2. 인증 코드 검증 (이메일 + 코드 함께 확인)
    @PostMapping("/verify")
    public ResponseEntity<?> verifyResetCode(@RequestBody @Valid PasswordResetVerifyRequest request, BindingResult bindingResult) {
        // 폼 형식 오류
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errorMessages);
        }

        return passwordResetService.verifyCode(request.getEmail(), request.getVerificationCode())
                ? ResponseEntity.ok("Verification successful.")
                : ResponseEntity.badRequest().body("Invalid verification code.");
    }


    // 3. 새 비밀번호 설정
    @PostMapping("/new-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid PasswordResetNewPasswordRequest request, BindingResult bindingResult) {
        // 비밀번호 확인 불일치
        if (!request.getNewPassword1().equals(request.getNewPassword2())) {
            return ResponseEntity.badRequest().body("The new passwords do not match.");
        }

        // 폼 형식 오류
        if (bindingResult.hasErrors()) {
            List<String> errorMessages = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errorMessages);
        }

        this.passwordResetService.resetPassword(request.getEmail(), request.getNewPassword1());
        return ResponseEntity.ok("Password successfully reset.");
    }
}
