package capston.capston_spring.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * 삭제 예정
 */
public class EditProfileImageRequest {
    @NotEmpty(message = "프로필 이미지 URL을 입력하세요.")
    private String profileImageUrl;
}
