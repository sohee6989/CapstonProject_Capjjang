package capston.capston_spring.repository;

import capston.capston_spring.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    @Override
    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByEmail(String email);
    // 추가: username으로 사용자 조회
    Optional<AppUser> findByUsername(String username); // 기존 findByUsername → findByName 으로 수정
}