package capston.capston_spring.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * @param filename 업로드할 파일 이름 (예: video123.mp4)
     * @return S3에 직접 업로드할 수 있는 presigned URL
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<String> generatePresignedUrl(@RequestParam String filename) {
        try {
            // S3에 저장될 key 지정 (예: videos/video123.mp4)
            String key = "videos/" + filename;

            // S3에 어떤 요청을 보낼지 명시
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("video/mp4") // 고정해도 되고, 파라미터로 받아도 됨
                    .build();

            // Presigned URL 요청 설정 (유효 시간: 10분)
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(10))
                    .putObjectRequest(objectRequest)
                    .build();

            // Presigned URL 생성
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            URL presignedUrl = presignedRequest.url();

            return ResponseEntity.ok(presignedUrl.toString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Presigned URL 생성 실패: " + e.getMessage());
        }
    }

}
