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
public class PracticeSession {

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

    @Column(nullable = false)   // 시작시간은 꼭 확인
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    // 연습 모드 구분 (full, highlight)
    @Column(nullable = false)
    private String mode;

    // 생성 시간 자동 저장
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성 시점 자동 세팅
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** 초 단위 값을 LocalDateTime으로 변환하는 메서드 **/
    public void setStartTimeFromSeconds(int seconds) {
        this.startTime = LocalDateTime.of(1970, 1, 1, 0, 0).plusSeconds(seconds);
    }

    public void setEndTimeFromSeconds(int seconds) {
        this.endTime = LocalDateTime.of(1970, 1, 1, 0, 0).plusSeconds(seconds);
    }

    /** LocalDateTime을 초 단위 값으로 변환 **/
    public int getStartTimeInSeconds() {
        return (int) (this.startTime.toEpochSecond(java.time.ZoneOffset.UTC) -
                LocalDateTime.of(1970, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC));
    }

    public int getEndTimeInSeconds() {
        return (int) (this.endTime.toEpochSecond(java.time.ZoneOffset.UTC) -
                LocalDateTime.of(1970, 1, 1, 0, 0).toEpochSecond(java.time.ZoneOffset.UTC));
    }
}