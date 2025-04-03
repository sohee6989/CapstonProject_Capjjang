package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecordedVideoDto {
    private Long practiceSessionId;
    private Long challengeSessionId;
    private Long accuracySessionId;
    private String videoPath;
    private LocalDateTime recordedAt;
    private int duration;
}