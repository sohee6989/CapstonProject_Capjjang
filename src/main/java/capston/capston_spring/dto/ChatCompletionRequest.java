package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ChatCompletionRequest {
    private String model;
    private List<Map<String, String>> messages;
    private double temperature;
}
