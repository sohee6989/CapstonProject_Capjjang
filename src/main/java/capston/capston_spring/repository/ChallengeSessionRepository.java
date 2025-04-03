package capston.capston_spring.repository;

import capston.capston_spring.entity.ChallengeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeSessionRepository extends JpaRepository<ChallengeSession, Long> {
    void deleteByUserId(Long id);

    List<ChallengeSession> findByUserId(Long userId);

    List<ChallengeSession> findBySongId(Long songId);

    List<ChallengeSession> findByUserIdAndSongId(Long userId, Long songId);
}
