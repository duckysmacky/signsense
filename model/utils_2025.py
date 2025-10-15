import cv2
import mediapipe as mp
import os

class Preprocess:

  @staticmethod
  def extract_hand_landmarks(video_path):
    
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
      static_image_mode=False,
      max_num_hands=2,
      min_detection_confidence=0.5,
      min_tracking_confidence=0.5
    )
    
    landmarks_data = []
    
    for root, _, files in os.walk(video_path):
      for file in files:
        if not(file.endswith(())):
          continue
        
        cap = cv2.VideoCapture(os.path.join(root, file))
        while True:
          success, img = cap.read()
          if not success:
            break
          
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
        
        cap.release()
        
    return landmarks_data
