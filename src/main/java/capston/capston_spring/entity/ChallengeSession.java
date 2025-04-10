package capston.capston_spring.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString   // 디버깅 관련
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChallengeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    // 세션 ID 추가
    @Column(nullable = false)
    private Long sessionId;  // sessionId 필드 추가

    @Column(nullable = false)
    private int startTime;  // 하이라이트 시간으로 설정됨

    @Column(nullable = false)
    private int endTime;    // 하이라이트 시간으로 설정됨

    // 하이라이트 시간 반영 메서드 추가
    public void setHighlightTimesFromSong() {
        this.startTime = this.song.getHighlightStartTime();
        this.endTime = this.song.getHighlightEndTime();
    }

    // duration 계산 메서드
    public String getDuration() {
        int duration = endTime - startTime;
        return duration + "초";  // duration을 "초" 단위로 반환
    }
}
