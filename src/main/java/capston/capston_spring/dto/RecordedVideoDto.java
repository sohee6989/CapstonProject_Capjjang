package capston.capston_spring.dto;

import capston.capston_spring.entity.VideoMode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecordedVideoDto {
    // id하나로 통일하고, ViewMode로 받기
    private Long SessionId;
    private VideoMode videoMode;
    private String videoPath;
    private LocalDateTime recordedAt;
    private int duration;
}