// 0403 추가

package capston.capston_spring.dto;

import capston.capston_spring.entity.AppUser;
import capston.capston_spring.entity.Song;
import capston.capston_spring.entity.ChallengeSession;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChallengeSessionResponse {

    private Long sessionId;
    private UserInfo user;
    private SongInfo song;
    private int startTime;  // int로 변경
    private int endTime;    // int로 변경
    private String duration;

    @Getter
    @AllArgsConstructor
    public static class UserInfo {
        private String username;  // 사용자 이름

        // AppUser 객체를 받아 UserInfo 객체로 변환
        public static UserInfo from(AppUser user) {
            return new UserInfo(user.getUsername());
        }
    }

    // 곡 정보 클래스 (내부 클래스)
    @Getter
    @AllArgsConstructor
    public static class SongInfo {
        private Long id;    // 곡 ID
        private String title; // 곡 제목

        // Song 객체를 받아 SongInfo 객체로 변환
        public static SongInfo from(Song song) {
            return new SongInfo(song.getId(), song.getTitle());
        }
    }

    public static ChallengeSessionResponse fromEntity(ChallengeSession session) {
        Song song = session.getSong();      // ChallengeSession에서 Song 객체 가져오기
        AppUser user = session.getUser();   // ChallengeSession에서 AppUser 객체 가져오기

        // Duration 계산 (ChallengeSession의 getDuration 메서드 사용)
        String formattedDuration = session.getDuration();

        // ChallengeSessionResponse 생성 후 반환
        return new ChallengeSessionResponse(
                session.getId(),                    // 세션 ID
                UserInfo.from(user),                // 사용자 정보 변환
                SongInfo.from(song),                // 곡 정보 변환
                session.getStartTime(),             // 시작 시간 (int)
                session.getEndTime(),               // 종료 시간 (int)
                formattedDuration                   // 지속 시간
        );
    }
}
