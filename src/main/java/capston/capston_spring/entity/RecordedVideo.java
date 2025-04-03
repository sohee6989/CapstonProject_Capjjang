package capston.capston_spring.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecordedVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoMode mode;

    @ManyToOne
    @JoinColumn(name = "practice_session_id")
    private PracticeSession practiceSession;   // 연습 세션 (선택적 연결)

    @ManyToOne
    @JoinColumn(name = "challenge_session_id")
    private ChallengeSession challengeSession; // 챌린지 세션 (선택적 연결)

    @ManyToOne
    @JoinColumn(name = "accuracy_session_id")
    private AccuracySession accuracySession;   // 정확도 세션 (선택적 연결 - 새로 추가된 필드)

    @Column(nullable = false)
    private String videoPath;         // 저장된 비디오 파일 경로

    @Column(nullable = false)
    private LocalDateTime recordedAt; // 녹화 시간 -> 연결된 session의 시작 시간과 동일하게 맞춰야 할 듯 -> 혹은 없애는게 나을 수도?

    @Column(nullable = false)
    private int duration;             // 영상 길이 (초 단위)

}