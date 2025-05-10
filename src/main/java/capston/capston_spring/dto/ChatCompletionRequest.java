package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatCompletionRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature = 1.0;  // optional
}
