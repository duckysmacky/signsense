import cv2
import mediapipe as mp
import keras #from tensorflow import keras
from google.protobuf.json_format import MessageToDict #может конфликтовать с keras проверь версию
import sys
import asyncio #нет смысла - cv2 работает синхронно

class ModelTestApp:

    def __init__(self, camId: int = 0, model_path: str = "", model: keras.Model = None):
        self.__mpHands = mp.solutions.hands # зачем везде инкапсуляция..
        self.__mpDraw = mp.solutions.drawing_utils
        self.__hands = self.__mpHands.Hands(
                    static_image_mode=False,
                    model_complexity=1,
                    min_detection_confidence=0.75, #0.5 в моей модели нужно че то решить будет
                    min_tracking_confidence=0.75, #0.5 в моей модели нужно че то решить будет
                    max_num_hands=2) #1 рука только нужна
        
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
        while True: #зачем записывать переменные как параметры класса?
            self.__success, self.__img = self.__cap.read() #success, img = self.__cap.read()

            self.__img = cv2.flip(self.__img, 1)
        
            imgRGB = cv2.cvtColor(self.__img, cv2.COLOR_BGR2RGB)

            results = self.__hands.process(imgRGB)
        
            await asyncio.create_task(self.__show_landmarks(results))
            prediction = await asyncio.create_task(self.__predict())

            cv2.imshow('Image', self.__img)
            if cv2.waitKey(1) & 0xff == ord('q'):
                break # освободи ресурсы в конце

if __name__ == "__main__":
    async def main():
        debug = ModelTestApp(0, r"C:\Users\DOOMB\Desktop\signsense\model\modelultracool4.pt")
        await asyncio.create_task(debug.show_window())
    asyncio.run(main())
