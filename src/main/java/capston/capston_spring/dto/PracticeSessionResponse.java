package capston.capston_spring.dto;

import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.entity.PracticeSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PracticeSessionResponse {
    private Long sessionId;
    private UserInfo user;
    private SongInfo song;
    private String mode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime timestamp;
    private String duration;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private String username;

        public static UserInfo from(AppUser user) {
            return new UserInfo(user.getUsername());
        }
    }

    @Getter
    @AllArgsConstructor
    public static class SongInfo {
        private Long id;
        private String title;

        public static SongInfo from(Song song) {
            return new SongInfo(song.getId(), song.getTitle());
        }
    }

    // PracticeSession → PracticeSessionResponse 변환 메서드
    public static PracticeSessionResponse fromEntity(PracticeSession session) {
        Song song = session.getSong();
        String mode = session.getMode(); // "full" 또는 "highlight"

        int startSec = mode.equalsIgnoreCase("full") ? song.getFullStartTime() : song.getHighlightStartTime();
        int endSec = mode.equalsIgnoreCase("full") ? song.getFullEndTime() : song.getHighlightEndTime();

        LocalDateTime baseTime = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime startTime = baseTime.plusSeconds(startSec);
        LocalDateTime endTime = baseTime.plusSeconds(endSec);

        int durationSec = endSec - startSec; // duration 계산 (초 단위)

        // duration 포맷: 00:00:15 형식으로 변환
        String duration = String.format("%02d:%02d:%02d",
                durationSec / 3600,
                (durationSec % 3600) / 60,
                durationSec % 60
        );

        return new PracticeSessionResponse(
                session.getId(),
                UserInfo.from(session.getUser()),
                SongInfo.from(song),
                mode.toLowerCase(),
                startTime,
                endTime,
                session.getCreatedAt(),
                duration // 포맷된 문자열 전달
        );
    }
}