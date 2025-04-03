package capston.capston_spring.controller;

import capston.capston_spring.dto.CustomUserDetails;
import capston.capston_spring.entity.AppUser;
import capston.capston_spring.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class TestController {


    // 사용자 로그인 테스트 
    private final UserService userService;
    
    
    // 서버에서 영상 불러오기 테스트
    private static final String BUCKET_NAME = "danzle-s3-bucket";
    private static final String REGION = "ap-northeast-2";
    private static final String BASE_URL = "https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com";


    @PostMapping("/test")
    @ResponseBody
    public ResponseEntity<String> test(Authentication authentication){
        if (authentication == null || !authentication.isAuthenticated()){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요한 요청입니다.");
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        AppUser user = this.userService.getUser(userDetails.getEmail());
        String name = user.getUsername();
        return ResponseEntity.ok( name + "님 안녕하세요");
    }


    @ResponseBody
    @GetMapping("/")
    public String show(){
        return "루트페이지";
    }


    @GetMapping("/guide-url")
    public ResponseEntity<String> getGuideVideoUrl(@RequestParam("songName") String songName) {
        String key = "songs/" + songName + "/contourwithaudio_x100.mp4";
        String fullUrl = BASE_URL + "/" + key;
        return ResponseEntity.ok(fullUrl);
    }


}
