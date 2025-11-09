import cv2
import mediapipe as mp
import keras #from tensorflow import keras
from google.protobuf.json_format import MessageToDict #может конфликтовать с keras проверь версию
import sys
import time
import numpy as np
import tensorflow as tf

class ModelTestApp:

    def __init__(self, model):
        self.hands = mp.solutions.hands.Hands()
        self.model = model
        self.landmarks_buffer = []
        self.prev_landmarks = None
        self.last_prediction_time = 0

    def __predict(self, frame, frames_for_prediction=10, prediction_interval=0.1):
        current_time = time.time()
        landmarks, _ = self.extract_landmarks(frame)
        self.landmarks_buffer.append(landmarks)
        
        if (current_time - self.last_prediction_time >= prediction_interval and 
            len(self.landmarks_buffer) >= frames_for_prediction):
            
            sequence = np.array(self.landmarks_buffer[-frames_for_prediction:])
            prediction = self.model.predict(sequence.reshape(1, frames_for_prediction, -1), verbose=0)
            
            if prediction is not None:
                predicted_class = np.argmax(prediction[0])
                confidence = np.max(prediction[0])
                print(f"Prediction: Class {predicted_class}, Confidence: {confidence:.2f}")
            
            self.last_prediction_time = current_time
            self.landmarks_buffer = self.landmarks_buffer[5:]

    def predict_online(self, frames_for_prediction=10, prediction_interval=0.1):
        cap = cv2.VideoCapture(0)
        mpHands = mp.solutions.hands # зачем везде инкапсуляция..
        mpDraw = mp.solutions.drawing_utils
        hands = mpHands.Hands(
                    static_image_mode=False,
                    model_complexity=1,
                    min_detection_confidence=0.75, #0.5 в моей модели нужно че то решить будет
                    min_tracking_confidence=0.75, #0.5 в моей модели нужно че то решить будет
                    max_num_hands=2) #1 рука только нужна
        
        while True:
            success, frame = cap.read()
            results = hands.process(frame)
            if not success:
                break
            try:
                for handlms in results.multi_hand_landmarks:
                    mpDraw.draw_landmarks(frame, handlms, mpHands.HAND_CONNECTIONS)
            except:
                mpDraw.draw_landmarks(frame, results.multi_handedness, mpHands.HAND_CONNECTIONS)

            self.__predict(frame, frames_for_prediction, prediction_interval)
            
            cv2.imshow('Gesture Recognition', frame)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
        
        cap.release()
        cv2.destroyAllWindows()

if __name__ == "__main__":
    model_path = sys.argv[1]
    try:
        cam_id = sys.argv[2]
    except:
        cam_id = 0
    model = tf.keras.models.load_model(model_path)
    debug = ModelTestApp(cam_id, model)
    debug.predict_online()