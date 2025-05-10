import os
from flask import Flask, request, jsonify
import cv2
import numpy as np
import json
import logging
from pose import extract_pose_keypoints, compare_pose_directional

# ─────────────────────────────
# 환경 감지 (운영 vs 개발)
ENV = os.getenv('APP_ENV', 'dev')

if ENV == 'prod':
    SAVE_DIR = "/home/ubuntu/app/saved_frames"
    REF_VIDEO_DIR = "/home/ubuntu/app/ref_videos"
    REF_POSE_DIR = "/home/ubuntu/app/ref_poses"
    LOG_FILE = '/var/log/pose_eval_prod.log'
else:
    SAVE_DIR = "./saved_frames"
    REF_VIDEO_DIR = "./ref_videos"
    REF_POSE_DIR = "./ref_poses"
    LOG_FILE = None  # 콘솔 출력

# ─────────────────────────────
# 로깅 설정
if LOG_FILE:
    logging.basicConfig(
        filename=LOG_FILE,
        level=logging.INFO,
        format='%(asctime)s %(levelname)s %(message)s'
    )
else:
    logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)

# ─────────────────────────────
# S3 업로드 옵션 (필요 시 주석 해제)
# import boto3
# s3 = boto3.client('s3')
# def upload_to_s3(local_file_path, s3_bucket, s3_key):
#     try:
#         s3.upload_file(local_file_path, s3_bucket, s3_key)
#         url = f"https://{s3_bucket}.s3.ap-northeast-2.amazonaws.com/{s3_key}"
#         return url
#     except Exception as e:
#         app.logger.error(f"S3 업로드 실패: {e}")
#         return None

# ─────────────────────────────
# 안무가 JSON 불러오기 (캐시 구조)
pose_cache = {}

def get_ref_pose_from_disk(song_title: str, frame_index: int):
    if song_title not in pose_cache:
        file_path = f"{REF_POSE_DIR}/{song_title}_ref_pose_filtered_1sec_normalized.json"
        try:
            with open(file_path, "r") as f:
                pose_cache[song_title] = json.load(f)
        except FileNotFoundError:
            app.logger.error(f"Reference pose file not found for {song_title}")
            return None
        except json.JSONDecodeError:
            app.logger.error(f"Error decoding JSON from {file_path}")
            return None

    ref_data = pose_cache[song_title]
    ref_pose_by_frame = {entry['frame']: entry['keypoints'] for entry in ref_data}
    return ref_pose_by_frame.get(frame_index)

# ─────────────────────────────
@app.route("/analyze", methods=["POST"])
def pose_eval():
    image = request.files.get("frame")
    song_title = request.form.get("song_title")
    session_id = request.form.get("session_id")
    frame_index = int(request.form.get("frame_index", 0))

    app.logger.info(f"[REQUEST] song_title={song_title}, session_id={session_id}, frame_index={frame_index}")

    if not image or not song_title:
        app.logger.warning("Missing parameters")
        return jsonify({"error": "Missing parameters"}), 400

    npimg = np.frombuffer(image.read(), np.uint8)
    frame = cv2.imdecode(npimg, cv2.IMREAD_COLOR)

    # 사용자 프레임 → 키포인트 추출
    user_kps = extract_pose_keypoints(frame)
    if user_kps is None:
        app.logger.warning("User pose not detected in the frame")
        return jsonify({
            "score": 0,
            "feedback": "WORST",
            "frame_index": frame_index
        })

    # 사용자 프레임 저장
    save_user_path = f"{SAVE_DIR}/user_{session_id}_frame_{frame_index}.png"
    cv2.imwrite(save_user_path, frame)

    # 전문가 프레임 저장
    expert_video = cv2.VideoCapture(f"{REF_VIDEO_DIR}/{song_title}.mp4")
    expert_video.set(cv2.CAP_PROP_POS_FRAMES, frame_index)
    ret, expert_frame = expert_video.read()
    if ret:
        save_expert_path = f"{SAVE_DIR}/expert_{session_id}_frame_{frame_index}.png"
        cv2.imwrite(save_expert_path, expert_frame)
        expert_video.release()
    else:
        app.logger.warning(f"Failed to capture expert frame {frame_index} for {song_title}")
        expert_video.release()

    # 전문가 키포인트 불러오기
    expert_kps = get_ref_pose_from_disk(song_title, frame_index)
    if expert_kps is None:
        app.logger.warning(f"No reference keypoints for frame {frame_index} of {song_title}")
        return jsonify({"error": f"No reference pose for frame {frame_index}"}), 400

    # 사용자와 전문가 키포인트 비교
    try:
        score = compare_pose_directional(user_kps, expert_kps)
    except Exception as e:
        app.logger.error(f"Pose comparison failed: {e}")
        return jsonify({"error": "Pose comparison failed"}), 500

    feedback = 'BEST' if score > 95 else 'GOOD' if score > 87 else 'BAD'

    # S3 업로드 예시 (옵션)
    # user_s3_url = upload_to_s3(save_user_path, "your-bucket", f"user_frames/{session_id}_frame_{frame_index}.png")
    # expert_s3_url = upload_to_s3(save_expert_path, "your-bucket", f"expert_frames/{session_id}_frame_{frame_index}.png")

    app.logger.info(f"[RESULT] Frame: {frame_index}, Score: {score}, Feedback: {feedback}")

    return jsonify({
        "score": score,
        "feedback": feedback,
        "frame_index": frame_index
        # "user_image_url": user_s3_url,
        # "expert_image_url": expert_s3_url
    })

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000, debug=False)
