package capston.capston_spring.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class JoinRequest {
    @Email(message = "Invalid email format")
    @NotEmpty(message = "Email is required")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Invalid email domain")
    private String email;

    @NotEmpty(message = "Username is required")
    @Size(min = 3, message = "Username should be at least 3 characters")
    private String name;

    @NotEmpty(message = "Password is required")
    @Size(min = 8, message = "Password1 should be at least 8 characters")
    private String password1;

    @NotEmpty(message = "Password confirmation is required")
    @Size(min = 8, message = "Password2 should be at least 8 characters")
    private String password2;

    @AssertTrue(message = "You must accept the terms and conditions")
    private boolean termsAccepted;
}
