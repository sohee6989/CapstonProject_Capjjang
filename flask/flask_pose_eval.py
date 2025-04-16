from flask import Flask, request, jsonify
import cv2
import numpy as np
import math
import json
import boto3
import os
from pose import extract_pose_keypoints, compare_pose_directional
import logging
logging.basicConfig(level=logging.DEBUG)

app = Flask(__name__)

# ────────────────────────
# S3에서 안무가 JSON 불러오기 (캐시식 구조로 반복 요청 대비)
pose_cache = {}

def get_ref_pose_from_disk(song_title: str, frame_index: int):
    if song_title not in pose_cache:
        file_path = f"./ref_poses/{song_title}_ref_pose_filtered_1sec_normalized.json"
        with open(file_path, "r") as f:
            pose_cache[song_title] = json.load(f)

    ref_data = pose_cache[song_title]
    ref_pose_by_frame = {entry['frame']: entry['keypoints'] for entry in ref_data}
    return ref_pose_by_frame.get(frame_index)

# ────────────────────────
# 실시간 포즈 평가 API
@app.route("/analyze", methods=["POST"])
def pose_eval():
    image = request.files.get("frame")
    song_title = request.form.get("song_title")
    session_id = request.form.get("session_id")
    frame_index = int(request.form.get("frame_index", 0))

    app.logger.info(f"image={image}, song_title={song_title}, session_id={session_id}, frame_index={frame_index}")

    if not image or not song_title:
        app.logger.warning("Missing parameters")
        return jsonify({"error": "Missing parameters"}), 400

    npimg = np.frombuffer(image.read(), np.uint8)
    frame = cv2.imdecode(npimg, cv2.IMREAD_COLOR)

    # 사용자 프레임 → 키포인트 추출
    user_kps = extract_pose_keypoints(frame)

    # 전문가 키포인트 불러오기
    expert_kps = get_ref_pose_from_disk(song_title, frame_index)


    if expert_kps is None:
        app.logger.warning(f"No reference keypoints for frame {frame_index} of {song_title}")
        return jsonify({"error": f"No reference pose for frame {frame_index}"}), 400

    # 사용자와 전문가 키포인트 비교
    score = compare_pose_directional(user_kps, expert_kps)
    feedback = 'Best' if score > 90 else 'Good' if score > 80 else 'Bad'

    return jsonify({
        "score": score,
        "feedback": feedback,
        "frame_index": frame_index
    })

if __name__ == '__main__':
    app.run(host="0.0.0.0", port=5000)
