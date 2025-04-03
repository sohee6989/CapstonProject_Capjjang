package capston.capston_spring.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditPasswordRequest {
    @NotEmpty(message = "현재 비밀번호를 입력하세요.")
    private String currentPassword;

    @NotEmpty(message = "새 비밀번호를 입력하세요.")
    @Size(min = 8, message = "Password1 should be at least 8 characters")
    private String newPassword1;

    @NotEmpty(message = "비밀번호가 일치해야 합니다.")
    @Size(min = 8, message = "Password1 should be at least 8 characters")
    private String newPassword2;
}
