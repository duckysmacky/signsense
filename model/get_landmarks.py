#!/usr/bin/env python3

"""
Extracts hand landmarks from sign language videos using MediaPipe and saves them
in a NumPy file for model training.
"""

import os
import sys
import argparse
import cv2
import mediapipe as mp
import pandas as pd
import numpy as np
from pathlib import Path


def extract_hand_landmarks(video_path: str):
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=1,
        min_detection_confidence=0.6,
        min_tracking_confidence=0.35
    )
    
    landmarks_data = []
    attachment = []
    video_formats = ('.mp4')

    for root, _, files in os.walk(video_path):
        for file in files:
            if not file.endswith(video_formats):
                continue
            attachment.append(file)
            landmarks_video = []

            prev_x, prev_y, prev_z, prev_visibility = [0.0] * 4

            cap = cv2.VideoCapture(os.path.join(root, file))
            if not cap.isOpened():
                print(f"❌ Не удалось открыть: {file}")
                continue
      
            while True:
                success, img = cap.read()
                if not success:
                    break

                imgRGB = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)         
                results = hands.process(imgRGB)         
                
                if results.multi_hand_landmarks:
                    hand_landmarks = results.multi_hand_landmarks[0]
                    frame_landmarks = []
                    for landmark in hand_landmarks.landmark:

                        dx = landmark.x - prev_x
                        dy = landmark.y - prev_y
                        dz = landmark.z - prev_z
                        d_visibility = landmark.visibility - prev_visibility
                        
                        # Сохраняем текущие + изменения
                        frame_landmarks.extend([
                            landmark.x,
                            landmark.y, 
                            landmark.z,
                            landmark.visibility,
                            dx,
                            dy,
                            dz,
                            d_visibility
                        ])

                        # Обновляем предыдущие значения
                        prev_x, prev_y, prev_z, prev_visibility = [
                            landmark.x,
                            landmark.y, 
                            landmark.z,
                            landmark.visibility
                        ]
                        
                    landmarks_video.append(frame_landmarks)
                else:
                    # Заполняем нулями: 21 landmarks × 8 признаков
                    landmarks_video.append([0.0] * (21 * 8))
                    # Сбрасываем предыдущие координаты при потере руки
                    prev_x, prev_y, prev_z, prev_visibility = [0.0] * 4
            
            landmarks_data.append(landmarks_video)
            cap.release()
            print(f"✅ {file}: {len(landmarks_video)} кадров")

    hands.close()
    return landmarks_data, attachment


def find_targets(landmarks: list, attachment: list, target_path: str):
    csv_file = pd.read_csv(target_path, sep='\t')
    old_target = csv_file['text']
    attachment_id = csv_file['attachment_id']

    target = []
    for filename in attachment:
        found = False
        for key, value in zip(attachment_id, old_target):
            if filename == key+'.mp4':
                target.append(value)
                found = True
                break
        if not found:
            target.append("UNKNOWN")
  
    return landmarks, target

def main():
    """
    Usage: python get_landmarks.py "<path_to_videos>" "<path_to_csv>"
    """

    parser = argparse.ArgumentParser(description="Extract hand landmarks from sign language videos.")
    parser.add_argument("videos", help="Path to directory containing .mp4 files")
    parser.add_argument("labels", help="Path to CSV/TSV file with labels")
    parser.add_argument("-o", "--output", default="data/processed/landmarks.npy", help="Output .npy file path (default: data/processed/landmarks.npy)")
    parser.add_argument("-n", "--max", type=int, default=None, help="Process only the first N videos (for testing)")
    args = parser.parse_args()

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    
    print("🔎 Обработка видео в " + args.videos)

    landmarks, attachment = extract_hand_landmarks(args.videos)
    landmarks, target = find_targets(landmarks, attachment, args.labels)

    data_to_save = {
        'landmarks': landmarks,
        'targets': target,
        'attachments': attachment
    }
    np.save(args.output, data_to_save, allow_pickle=True)

    print("🎯 Все данные сохранены в " + args.output)

if __name__ == "__main__": main()