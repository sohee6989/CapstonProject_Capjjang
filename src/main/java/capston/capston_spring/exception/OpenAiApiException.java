package capston.capston_spring.exception;

import lombok.Getter;

@Getter
public class OpenAiApiException extends RuntimeException {
    private final int statusCode;  // 상태코드 추가

    public OpenAiApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public OpenAiApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
