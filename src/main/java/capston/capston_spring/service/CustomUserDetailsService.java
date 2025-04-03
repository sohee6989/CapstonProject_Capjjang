package capston.capston_spring.service;

import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.exception.UserNotFoundException;
import capston.capston_spring.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@AllArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** 이메일로 찾도록 변경 **/
    @Override
    public UserDetails loadUserByUsername(String email) throws UserNotFoundException {

        // DB에서 회원 조회
        Optional<AppUser> user = this.userRepository.findByEmail(email);

        if(user.isPresent()){
            return new CustomUserDetails(user.get());  // UserDetails에 담아서 return하면 AutneticationManager가 검증 함
        }

        throw null;
    }





}
