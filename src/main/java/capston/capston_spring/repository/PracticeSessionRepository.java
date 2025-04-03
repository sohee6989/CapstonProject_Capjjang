package capston.capston_spring.repository;

import capston.capston_spring.entity.PracticeSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {
    List<PracticeSession> findByUserId(Long userId);
    List<PracticeSession> findBySongIdAndUserId(Long songId, Long userId);
    void deleteByUserId(Long id);
}
