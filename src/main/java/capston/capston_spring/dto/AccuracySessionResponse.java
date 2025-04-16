package capston.capston_spring.dto;

import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.entity.AccuracySession;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.Duration;


@Getter
@AllArgsConstructor
public class AccuracySessionResponse {
    private Long sessionId; // 변경된 부분
    private UserInfo user;
    private SongInfo song;
    private Double score;
    private String mode; // 추가된 부분: mode
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration;
    private LocalDateTime timestamp;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private String username;

        public static UserInfo from(AppUser user) {
            return new UserInfo(user.getUsername());  // 수정된 부분
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

    public static AccuracySessionResponse fromEntity(AccuracySession session) {
        Song song = session.getSong();
        String mode = session.getMode();

        // Calculate duration
        Duration duration = Duration.between(session.getStartTime(), session.getEndTime());
        String formattedDuration = String.format("00:00:%02d", duration.toSeconds());

        return new AccuracySessionResponse(
                session.getId(), // 0416 수정: getSessionId() → getId()
                UserInfo.from(session.getUser()),
                SongInfo.from(song),
                session.getAvg_score(),
                session.getMode(),
                session.getStartTime(),
                session.getEndTime(),
                formattedDuration,
                session.getCreatedAt()
        );
    }
}
