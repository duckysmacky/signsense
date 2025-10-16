import cv2
import mediapipe as mp
import os
import pandas as pd
from sklearn.model_selection import train_test_split

class Preprocess:

  @staticmethod
  def extract_hand_landmarks(video_path):
    
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
      static_image_mode=False,
      max_num_hands=2, # Better 2 for signs
      min_detection_confidence=0.5,
      min_tracking_confidence=0.5
    )
    
    landmarks_data = []
    attachment = []

    # Popular video formats
    video_formats = ('.mp4', '.avi', '.mov', '.mkv', '.webm')

    for root, _, files in os.walk(video_path):
      for file in files:
        if not file.endswith(video_formats):
          continue
        attachment.append(file)

        cap = cv2.VideoCapture(os.path.join(root, file))
        fps = cap.get(cv2.CAP_PROP_FPS)
        frame_interval = max(1, int(fps * 0.1))
        frame_count = 0

        while True:
          success, img = cap.read()
          if not success:
            break
          
          if frame_count % frame_interval != 0:
            frame_count += 1
            continue

          imgRGB = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)         
          results = hands.process(imgRGB)         
          frame_landmarks = []
          
          if results.multi_hand_landmarks:
            for hand_landmarks in results.multi_hand_landmarks:
              hand_data = []
              for landmark in hand_landmarks.landmark:
                hand_data.append({
                  'x': landmark.x,
                  'y': landmark.y, 
                  'z': landmark.z,
                  'visibility': landmark.visibility
                })
              frame_landmarks.append(hand_data)
          
          landmarks_data.append(frame_landmarks)
          frame_count += 1
        
        cap.release()
  
    return landmarks_data, attachment
  
  @staticmethod
  def train_test_split(landmarks, attachment, target_path:str):

    csv_file = pd.read_csv(target_path)
    old_target = csv_file['text']
    attachment_id = csv_file['attachment_id']
    optional = csv_file[['height','width', 'lenght', 'begin', 'end']]

    target = []
    for filename in attachment:
      found = False
      for key, value in zip(attachment_id, old_target):
        if filename == key:
          target.append(value)
          found = True
          break
    
    video_train, video_test, target_train, target_test = train_test_split(
        landmarks, 
        target, 
        test_size=0.2, 
        random_state=42,
        stratify=target
    )
    
    return video_train, video_test, target_train, target_test

  @staticmethod
  def add_attribs(landmarks_data, scale:bool = False): # +scale
    pass


class Make_model:

  @staticmethod
  def model_1():
    pass
