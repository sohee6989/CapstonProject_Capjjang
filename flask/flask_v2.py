# flask_v2.py

from flask import Flask, request, jsonify
import os
import cv2
from pose_analysis import process_and_compare_videos  # 실시간 정확도 분석 함수 불러오기
from s3_helper import download_temp_from_s3

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


# 연습 모드: 실루엣만 오버레이
@app.route("/practice-mode", methods=["GET"])
def practice_mode():
    song_title = request.args.get("songTitle")
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
    song_title = request.args.get("songTitle")
    if not song_title:
        return jsonify({"error": "songTitle 파라미터가 필요합니다."}), 400

    # 영상 재생과 동시에 점수 분석 시작
    try:
        process_and_compare_videos(song_title=song_title)
        return jsonify({"message": "Accuracy mode 종료"})
    except Exception as e:
        print("오류 발생:", str(e))
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
