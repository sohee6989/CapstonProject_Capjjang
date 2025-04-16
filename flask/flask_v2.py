from flask import Flask, request, jsonify
import os
import cv2
import numpy as np
from pose_analysis import process_and_compare_videos, analyze_frame_image, extract_expert_keypoints  # 분석 함수
from s3_helper import download_temp_from_s3  # 실루엣 영상 다운로드용
import traceback

app = Flask(__name__)


def resize_with_aspect_ratio(image, target_width, target_height):
    h, w = image.shape[:2]
    scale = min(target_width / w, target_height / h)
    new_w = int(w * scale)
    new_h = int(h * scale)
    resized = cv2.resize(image, (new_w, new_h))

    top = (target_height - new_h) // 2
    bottom = target_height - new_h - top
    left = (target_width - new_w) // 2
    right = target_width - new_w - left

    padded = cv2.copyMakeBorder(resized, top, bottom, left, right,
                                 cv2.BORDER_CONSTANT, value=[0, 0, 0])
    return padded


# 실시간 프레임 분석 API
@app.route("/analyze", methods=["POST"])
def analyze_frame():
    file = request.files.get("frame")
    song_title = request.form.get("song_title")
    session_id = request.form.get("session_id")
    frame_index = request.form.get("frame_index")

    print("🎯 analyze_frame 호출됨")
    print("✅ song_title:", song_title)
    print("✅ session_id:", session_id)
    print("✅ frame_index:", frame_index)
    print("✅ 파일 여부:", "있음" if file else "없음")

    if file is None:
        return jsonify({"error": "No frame provided"}), 400
    if not song_title:
        return jsonify({"error": "Missing songTitle"}), 400

    image_bytes = np.frombuffer(file.read(), np.uint8)
    image = cv2.imdecode(image_bytes, cv2.IMREAD_COLOR)

    if image is None:
        return jsonify({"error": "Invalid image"}), 400

    try:
        expert_path, _ = download_temp_from_s3(song_title)
        print("다운로드 성공. 경로", expert_path)
        print("전문가 포즈 키포인트 추출 시작")
        expert_kps = extract_expert_keypoints(expert_path)
        print("키포인트 추출 완료")
        print("사용자 프레임 분석 시작")
        score, feedback = analyze_frame_image(image, expert_path)
        print("분석 완료. Score:", score, "Feedback:", feedback)
        return jsonify({"score": score, "feedback": feedback})
    except FileNotFoundError as fnf_err:
        print("파일을 찾을 수 없음:", str(fnf_err))
        return jsonify({"error": "S3 또는 로컬에 전문가 영상을 찾을 수 없습니다."}), 500

    except ValueError as val_err:
        print("포즈 분석 실패:", str(val_err))
        return jsonify({"error": "전문가 영상에서 포즈를 감지하지 못했습니다."}), 500

    except Exception as e:
        print("알 수 없는 예외 발생:", str(e))
        return jsonify({"error": "서버 내부 오류 발생 - " + str(e)}), 500



# 연습 모드: 실루엣만 오버레이
@app.route("/practice-mode", methods=["GET"])
def practice_mode():
    song_title = request.args.get("song_title")
    if not song_title:
        return jsonify({"error": "songTitle 파라미터가 필요합니다."}), 400

    try:
        _, silhouette_path = download_temp_from_s3(song_title)
    except Exception as e:
        return jsonify({"error": f"S3 다운로드 실패: {str(e)}"}), 500

    cap_silhouette = cv2.VideoCapture(silhouette_path)
    cap_webcam = cv2.VideoCapture(0)

    if not cap_silhouette.isOpened():
        return jsonify({"error": "실루엣 영상을 열 수 없습니다."}), 500
    if not cap_webcam.isOpened():
        return jsonify({"error": "웹캠을 열 수 없습니다."}), 500

    while True:
        ret_sil, frame_sil = cap_silhouette.read()
        ret_cam, frame_cam = cap_webcam.read()

        if not ret_sil:
            cap_silhouette.set(cv2.CAP_PROP_POS_FRAMES, 0)
            continue
        if not ret_cam:
            break

        frame_cam = cv2.flip(frame_cam, 1)

        frame_sil = resize_with_aspect_ratio(frame_sil, 640, 480)
        frame_cam = resize_with_aspect_ratio(frame_cam, 640, 480)

        blended = cv2.addWeighted(frame_cam, 0.5, frame_sil, 0.5, 0)

        cv2.imshow("Practice Mode", blended)

        if cv2.waitKey(1) & 0xFF == ord("q"):
            break

    cap_silhouette.release()
    cap_webcam.release()
    cv2.destroyAllWindows()
    return jsonify({"message": "Practice mode 종료"})


# 정확도 모드: 실루엣 + 점수 + 피드백
@app.route("/accuracy-mode", methods=["GET"])
def accuracy_mode():
    song_title = request.args.get("song_title")
    if not song_title:
        return jsonify({"error": "songTitle 파라미터가 필요합니다."}), 400

    try:
        expert_path, expert_shilouett = download_temp_from_s3(song_title)
        process_and_compare_videos(expert_path, expert_shilouett)
        return jsonify({"message": "Accuracy mode 종료"})
    except Exception as e:
        print("오류 발생:", str(e))
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
