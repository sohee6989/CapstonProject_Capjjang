# flask_v2.py

from flask import Flask, request, jsonify
import os
import cv2
from pose_analysis import process_and_compare_videos  # 실시간 정확도 분석 함수 불러오기

app = Flask(__name__)


def resize_with_aspect_ratio(image, target_width, target_height):
    h, w = image.shape[:2]
    scale = min(target_width / w, target_height / h)
    new_w = int(w * scale)
    new_h = int(h * scale)
    resized = cv2.resize(image, (new_w, new_h))

    # 여백 채우기 (위/아래 or 좌/우)
    top = (target_height - new_h) // 2
    bottom = target_height - new_h - top
    left = (target_width - new_w) // 2
    right = target_width - new_w - left

    padded = cv2.copyMakeBorder(resized, top, bottom, left, right, cv2.BORDER_CONSTANT, value=[0, 0, 0])
    return padded

# 연습 모드: 실루엣만 오버레이
@app.route("/practice-mode", methods=["GET"])
def practice_mode():
    song_title = request.args.get("songTitle")
    if not song_title:
        return jsonify({"error": "songTitle 파라미터가 필요합니다."}), 400

    expert_video_path = f"videos1/{song_title}_expert.mp4"  # _expert 추가
    silhouette_path = f"static/output/{song_title}_silhouette.mp4"

    cap_silhouette = cv2.VideoCapture(silhouette_path)
    cap_webcam = cv2.VideoCapture(0)
    # cap_expert = cv2.VideoCapture(expert_video_path)

    if not cap_silhouette.isOpened():
        return jsonify({"error": " 실루엣 영상을 열 수 없습니다."}), 500
    if not cap_webcam.isOpened():
        return jsonify({"error": " 웹캠을 열 수 없습니다."}), 500
    # if not cap_expert.isOpened():
        # return jsonify({"error": " 전문가 영상을 열 수 없습니다."}), 500
    
    while True:
        ret_sil, frame_sil = cap_silhouette.read()
        ret_cam, frame_cam = cap_webcam.read()
        # ret_exp, frame_exp = cap_expert.read()

        if not ret_sil:
            cap_silhouette.set(cv2.CAP_PROP_POS_FRAMES, 0)
            continue
        # if not ret_exp:
            # cap_expert.set(cv2.CAP_PROP_POS_FRAMES, 0)
            # continue
        if not ret_cam:
            break

        # 웹캠 영상을 좌우 반전하여 정상적으로 표시
        frame_cam = cv2.flip(frame_cam, 1)

        # 크기 맞추기       
        TARGET_WIDTH = 680
        TARGET_HEIGHT = 1200  # 세로 꽉 차게 (모바일 기준)

        frame_sil = resize_with_aspect_ratio(frame_sil, TARGET_WIDTH, TARGET_HEIGHT)
        frame_cam = resize_with_aspect_ratio(frame_cam, TARGET_WIDTH, TARGET_HEIGHT)


        # 실루엣 오버레이
        blended = cv2.addWeighted(frame_cam, 0.5, frame_sil, 0.5, 0)

        # 전문가 + 사용자(오버레이) 나란히 출력
        # combined = cv2.hconcat([frame_exp, blended])
        cv2.imshow("Practice Mode", blended)

        if cv2.waitKey(1) & 0xFF == ord("q"):
            break

    cap_silhouette.release()
    cap_webcam.release()
    # cap_expert.release()
    cv2.destroyAllWindows()
    return jsonify({"message": "Practice mode 종료"})


# 정확도 모드: 실루엣 + 점수 + 피드백
@app.route("/accuracy-mode", methods=["GET"])
def accuracy_mode():
    song_title = request.args.get("songTitle")
    if not song_title:
        return jsonify({"error": "songTitle 파라미터가 필요합니다."}), 400

    expert_video_path = f"videos1/{song_title}_expert.mp4"
    silhouette_path = f"video_uploads/{song_title}_silhouette.mp4"

    # 영상 재생과 동시에 점수 분석 시작
    try:
        process_and_compare_videos(song_title=song_title)
  
        return jsonify({"message": "Accuracy mode 종료"})
    except Exception as e:
        print("오류 발생:", str(e))
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
