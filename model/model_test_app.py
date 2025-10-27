import cv2
import mediapipe as mp
import keras
from google.protobuf.json_format import MessageToDict
import sys
import asyncio

class ModelTestApp:

    def __init__(self, camId: int = 0, model_path: str = "", model: keras.Model = None):
        self.__mpHands = mp.solutions.hands
        self.__mpDraw = mp.solutions.drawing_utils
        self.__hands = self.__mpHands.Hands(
                    static_image_mode=False,
                    model_complexity=1,
                    min_detection_confidence=0.75,
                    min_tracking_confidence=0.75,
                    max_num_hands=2)
        
        self.__cap = cv2.VideoCapture(camId)
    
        if not model:
            self.__model = keras.models.load_model(model_path)
        else:
            self.__model = model

    async def __show_landmarks(self, results):
        try:
            for handlms in results.multi_hand_landmarks:
                self.__mpDraw.draw_landmarks(self.__img, handlms, self.__mpHands.HAND_CONNECTIONS)
        except:
            self.__mpDraw.draw_landmarks(self.__img, results.multi_handedness, self.__mpHands.HAND_CONNECTIONS)

    async def __predict(self):
        pass


    async def show_window(self):
        while True:
            self.__success, self.__img = self.__cap.read()

            self.__img = cv2.flip(self.__img, 1)
        
            # Convert BGR image to RGB image
            imgRGB = cv2.cvtColor(self.__img, cv2.COLOR_BGR2RGB)

            # Process the RGB image
            results = self.__hands.process(imgRGB)
        
            await asyncio.create_task(self.__show_landmarks(results))
            prediction = await asyncio.create_task(self.__predict())
            # Display Video and when 'q' is entered, destroy the window
            cv2.imshow('Image', self.__img)
            if cv2.waitKey(1) & 0xff == ord('q'):
                break

if __name__ == "__main__":
    async def main():
        debug = ModelTestApp(0, r"C:\Users\DOOMB\Desktop\signsense\model\modelultracool4.pt")
        await asyncio.create_task(debug.show_window())
    asyncio.run(main())