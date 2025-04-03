package capston.capston_spring.repository;

import capston.capston_spring.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordedVideoRepository extends JpaRepository<RecordedVideo, Long> {

    List<RecordedVideo> findByUserId(Long userId);

    List<RecordedVideo> findByUserIdAndMode(Long userId, VideoMode mode);

    List<RecordedVideo> findByPracticeSessionId(Long sessionId);

    List<RecordedVideo> findByChallengeSessionId(Long sessionId);

    List<RecordedVideo> findByAccuracySessionId(Long sessionId);

    void deleteByUserId(Long id);

    List<RecordedVideo> findByPracticeSession(PracticeSession practiceSession);

    List<RecordedVideo> findByChallengeSession(ChallengeSession challengeSession);

    List<RecordedVideo> findByAccuracySession(AccuracySession accuracySession);
}