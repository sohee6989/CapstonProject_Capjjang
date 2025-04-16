package capston.capston_spring.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CorrectionResponse {
    private Long sessionId;
    private String song_title;
}