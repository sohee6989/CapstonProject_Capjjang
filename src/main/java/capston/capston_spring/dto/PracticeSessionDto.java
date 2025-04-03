package capston.capston_spring.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Getter
@Setter
public class PracticeSessionDto {
    private Long songId;
    private String mode;          // full 또는 highlight (모드 선택)
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 초 단위 값을 LocalDateTime으로 변환하는 헬퍼 메서드
    public void setStartTimeFromSeconds(int seconds) {
        this.startTime = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
    }

    public void setEndTimeFromSeconds(int seconds) {
        this.endTime = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
    }
}