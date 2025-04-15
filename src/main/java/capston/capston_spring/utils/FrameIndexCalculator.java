package capston.capston_spring.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class FrameIndexCalculator {

    /**
     * 세션 시작 시각과 현재 시각을 기반으로 프레임 인덱스를 계산한다.
     * 1초마다 프레임 1개 전송된다는 가정 하에 frameIndex = (현재시간 - 시작시간).초
     */
    public static int calculateFrameIndex(LocalDateTime sessionStartTime) {
        Duration duration = Duration.between(sessionStartTime, LocalDateTime.now(ZoneId.of("UTC")));
        return (int) duration.getSeconds();
    }
}