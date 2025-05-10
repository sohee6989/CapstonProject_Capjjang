package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatCompletionResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private Usage usage;       // 토큰 사용량 정보
    private List<Choice> choices;  // GPT가 반환한 응답 목록

    @Getter
    @Setter
    public static class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;
    }

    @Getter
    @Setter
    public static class Choice {
        private int index;
        private Message message;
        private String finish_reason;
    }

    @Getter
    @Setter
    public static class Message {
        private String role;
        private String content;
    }
}
