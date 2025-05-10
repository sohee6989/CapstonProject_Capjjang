package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessage {
    private String role;
    private String content;
}
