package capston.capston_spring.repository;

import capston.capston_spring.entity.AccuracySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccuracySessionRepository extends JpaRepository<AccuracySession, Long> {

    List<AccuracySession> findByUserId(Long userId);
    List<AccuracySession> findByUserIdAndSongId(Long userId, Long songId);
    void deleteByUserId(Long id);

    Optional<AccuracySession> findBySessionId(Long sessionId);



}