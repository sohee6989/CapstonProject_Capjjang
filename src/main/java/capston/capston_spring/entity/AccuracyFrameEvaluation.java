package capston.capston_spring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccuracyFrameEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private AccuracySession session;

    private Integer frameIndex;

    private Double score;

    @Column(length = 500)  // 피드백 내용 ?: 피드백 길이 제한
    private String feedback;

}
