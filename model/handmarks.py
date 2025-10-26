import cv2
import mediapipe as mp
import os
import pandas as pd
import numpy as np
import sys


def extract_hand_landmarks(video_path):
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=1,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
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
                        frame_landmarks.append([ #возможно стоит поменять на extend
                            landmark.x,
                            landmark.y, 
                            landmark.z,
                            landmark.visibility
                        ])
                    landmarks_video.append(frame_landmarks)
                else:
                    landmarks_video.append([0.0]*(21*4))
            
            landmarks_data.append(landmarks_video)
            cap.release()
            print(f"✅ {file}: {len(landmarks_video)} кадров")

    hands.close()
    return landmarks_data, attachment


def find_targets(landmarks, attachment, target_path:str):
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


if __name__ == "__main__":
    path_train = '/Users/iaroslav/Desktop/test_python'
    path_test = '/Users/iaroslav/Downloads/test_slovo.csv'

    if len(sys.argv) > 1:
        path_train = sys.argv[1]
        path_test = sys.argv[2]

    landmarks, attachment = extract_hand_landmarks(path_train)
    landmarks, target = find_targets(landmarks, attachment, path_test)

    data_to_save = {
        'landmarks': landmarks,
        'targets': target,
        'attachments': attachment
    }
    np.save('landmarks.npy', data_to_save, allow_pickle=True)

    print("🎯 Все данные сохранены в .npy файлы!")

    # ДЛЯ ЗАПУСКА В ТЕРМИНАЛЕ СО СВОИМИ ФАЙЛАМ - ПЕРЕЙДИТЕ В ТЕКУЩУЮ ДИРЕКТОРИЮ
    # ЗАТЕМ ВВЕДИТЕ СЛЕДУЮЩУЮ КОМАНДУ:
    # python handmarks.py (путь к файлу с видео) (путь к csv файлу с метками)
    # ПРОБЕЛЫ НА УКАЗАННЫХ МЕСТАХ; ЕСЛИ НЕ РАБОТАЕТ ПОПРОБУЙТЕ python3 вместо python