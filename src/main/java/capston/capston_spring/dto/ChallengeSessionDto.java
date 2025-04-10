package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChallengeSessionDto {
    private Long sessionId;  // sessionId 필드
    private Long songId;

    public Long getSessionId() {
        return this.sessionId;
    }
}
