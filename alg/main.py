import cv2
import mediapipe as mp
import math

class handDetector():
	def __init__(self, mode=False, maxHands=1, modelComplexity=1, detectionCon=0.5, trackCon=0.5):
		self.mode = mode
		self.maxHands = maxHands
		self.modelComplexity = modelComplexity
		self.detectionCon = detectionCon
		self.trackCon = trackCon

		self.mpHands = mp.solutions.hands
		self.hands = self.mpHands.Hands(self.mode, self.maxHands, self.modelComplexity, self.detectionCon, self.trackCon)
		self.mpDraw = mp.solutions.drawing_utils
		self.tipIds = [4, 8, 12, 16, 20] 

	def findHands(self, img, draw=True):
		imgRGB = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
		self.results = self.hands.process(imgRGB)
		if self.results.multi_hand_landmarks:
			for handLms in self.results.multi_hand_landmarks:
				if draw:
					self.mpDraw.draw_landmarks(img, handLms, self.mpHands.HAND_CONNECTIONS)
		return img

	def findPosition(self, img, handNo=0, draw=True):
		xList = []
		yList = []
		bbox = []
		self.lmList = []
		if self.results.multi_hand_landmarks:
			myHand = self.results.multi_hand_landmarks[handNo]
			for id, lm in enumerate(myHand.landmark):
				h, w, c = img.shape
				cx, cy = int(lm.x*w), int(lm.y*h)
				xList.append(cx)
				yList.append(cy)
				self.lmList.append([id, cx, cy])
				if draw:
					cv2.circle(img, (cx, cy), 5, (255,0,255), cv2.FILLED)
			xmin, xmax = min(xList), max(xList)
			ymin, ymax = min(yList), max(yList)
			bbox = xmin, ymin, xmax, ymax

			if draw:
				cv2.rectangle(img, (bbox[0]-20, bbox[1]-20), (bbox[2]+20, bbox[3]+20), (0, 255, 0), 2)
		return self.lmList, bbox

	def findDistance(self, p1, p2, img, draw=True):
		x1, y1 = self.lmList[p1][1], self.lmList[p1][2]
		x2, y2 = self.lmList[p2][1], self.lmList[p2][2]
		cx, cy = (x1+x2)//2, (y1+y2)//2

		if draw:
			cv2.circle(img, (x1,y1), 15, (255,0,255), cv2.FILLED)
			cv2.circle(img, (x2,y2), 15, (255,0,255), cv2.FILLED)
			cv2.line(img, (x1,y1), (x2,y2), (255,0,255), 3)
			cv2.circle(img, (cx,cy), 15, (255,0,255), cv2.FILLED)

		length = math.hypot(x2-x1, y2-y1)
		return length, img, [x1, y1, x2, y2, cx, cy]

	def fingersUp(self):
		fingers = []
		
		# Thumb
		if self.lmList[self.tipIds[0]][1] < self.lmList[self.tipIds[0]-1][1]:
			fingers.append(1)
		else:
			fingers.append(0)

		# 4 Fingers
		for id in range(1,5):
			if self.lmList[self.tipIds[id]][2] < self.lmList[self.tipIds[id]-2][2]:
				fingers.append(1)
			else:
				fingers.append(0)
		return fingers

def main():
	detector = handDetector()
	while True:
		img = cv2.imread("./alg/test.jpg")
		img = detector.findHands(img)

		cv2.imshow("Image", img)
		cv2.waitKey(1)


if __name__ == "__main__":
	main()