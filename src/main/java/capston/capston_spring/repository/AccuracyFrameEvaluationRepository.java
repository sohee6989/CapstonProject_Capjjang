package capston.capston_spring.repository;

import capston.capston_spring.entity.AccuracyFrameEvaluation;
import capston.capston_spring.entity.AccuracySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccuracyFrameEvaluationRepository extends JpaRepository<AccuracyFrameEvaluation, Long>{
    List<AccuracyFrameEvaluation> findBySessionId(Long sessionId);
    List<AccuracyFrameEvaluation> findBySession(AccuracySession session);

}
