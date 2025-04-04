package capston.capston_spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString   // 디버깅관련
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

    @Column(nullable = false)
    private int startTime;  // 수정: int로 변경

    @Column(nullable = false)
    private int endTime;    // int로 변경

    // duration 계산 메서드 추가
    public String getDuration() {
        int duration = endTime - startTime;
        return String.valueOf(duration);  // 또는 duration + "초"
}
