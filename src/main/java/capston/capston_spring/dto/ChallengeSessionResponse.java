// 0403 추가

package capston.capston_spring.dto;

import capston.capston_spring.entity.ChallengeSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChallengeSessionResponse {

    private Long sessionId;
    private SongInfo song;
    private int startTime;  // int로 변경
    private int endTime;    // int로 변경
    private String duration;

    public static ChallengeSessionResponse fromEntity(ChallengeSession session) {
        return new ChallengeSessionResponse(
                session.getId(),
                new SongInfo(
                        session.getSong().getId(),
                        session.getSong().getTitle()
                ),
                session.getStartTime(),
                session.getEndTime(),
                session.getDuration()
        );
    }

    @Getter
    @AllArgsConstructor
    public static class SongInfo {
        private Long id;
        private String title;
    }
}
