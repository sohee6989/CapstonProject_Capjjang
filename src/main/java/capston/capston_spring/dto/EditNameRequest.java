package capston.capston_spring.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditNameRequest {

    @NotEmpty(message = "이름은 비어 있을 수 없습니다.")
    private String name;
}
