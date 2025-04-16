package capston.capston_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AccuracySessionDto {
    private Long songId;
    private Double score;
    private String feedback;
    private String mode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
