#!/usr/bin/env python3
"""
Real-time tester for continuous sign language models (TensorFlow / Keras).

The script mirrors the preprocessing in get_landmarks.py by generating
21×8 features (x/y/z/visibility + deltas) per frame, maintaining a rolling
window, and feeding it into a sequence model to obtain predictions.

Only one OpenCV window is used — the webcam stream with landmarks overlayed.
"""

from __future__ import annotations

import argparse
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Sequence, Tuple

import cv2
import mediapipe as mp
import numpy as np
import tensorflow as tf


NUM_LANDMARKS = 21
FEATURES_PER_LANDMARK = 8


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Live tester for TensorFlow sign language models.")
    parser.add_argument("model_path", type=Path, help="Path to the SavedModel directory or .keras file.")
    parser.add_argument(
        "--labels",
        type=Path,
        help="Optional label map (JSON or txt) to convert class IDs to words.",
    )
    parser.add_argument(
        "--sequence-length",
        type=int,
        default=32,
        help="Number of frames stored in the rolling window passed to the model.",
    )
    parser.add_argument(
        "--min-frames",
        type=int,
        default=12,
        help="Minimum number of frames required before running inference.",
    )
    parser.add_argument(
        "--confidence-threshold",
        type=float,
        default=0.45,
        help="Minimum softmax probability needed to announce a prediction.",
    )
    parser.add_argument(
        "--camera-index",
        type=int,
        default=0,
        help="OpenCV camera index (default: 0).",
    )
    parser.add_argument(
        "--flip-frame",
        action="store_true",
        help="Flip webcam preview horizontally (useful for selfie cameras).",
    )
    return parser.parse_args()


def load_labels(path: Optional[Path]) -> Optional[List[str]]:
    if not path:
        return None
    if not path.exists():
        print(f"[WARN] Label map {path} does not exist – ignoring.", file=sys.stderr)
        return None
    try:
        if path.suffix.lower() == ".json":
            with path.open("r", encoding="utf-8") as fp:
                data = json.load(fp)
            if isinstance(data, dict):
                try:
                    items = sorted(data.items(), key=lambda kv: int(kv[0]))
                except (ValueError, TypeError):
                    items = data.items()
                return [str(label) for _, label in items]
            if isinstance(data, list):
                return [str(label) for label in data]
            print(f"[WARN] Unsupported JSON label format in {path}.", file=sys.stderr)
            return None
        with path.open("r", encoding="utf-8") as fp:
            labels = [line.strip() for line in fp if line.strip()]
        return labels or None
    except Exception as exc:  # pragma: no cover - IO heavy
        print(f"[WARN] Failed to read label map {path}: {exc}", file=sys.stderr)
        return None


class LandmarkFeatureExtractor:
    def __init__(self) -> None:
        self._prev = np.zeros(4, dtype=np.float32)

    def reset(self) -> None:
        self._prev[:] = 0.0

    def from_landmarks(self, hand_landmarks: mp.framework.formats.landmark_pb2.NormalizedLandmarkList) -> List[float]:
        frame_features: List[float] = []
        for landmark in hand_landmarks.landmark:
            dx = landmark.x - self._prev[0]
            dy = landmark.y - self._prev[1]
            dz = landmark.z - self._prev[2]
            dv = landmark.visibility - self._prev[3]
            frame_features.extend(
                [
                    landmark.x,
                    landmark.y,
                    landmark.z,
                    landmark.visibility,
                    dx,
                    dy,
                    dz,
                    dv,
                ]
            )
            self._prev[:] = (landmark.x, landmark.y, landmark.z, landmark.visibility)
        return frame_features


@dataclass
class PredictionResult:
    label: str
    confidence: float
    index: int


def format_label(idx: int, confidence: float, labels: Optional[Sequence[str]]) -> PredictionResult:
    label = labels[idx] if labels and 0 <= idx < len(labels) else f"class_{idx}"
    return PredictionResult(label=label, confidence=confidence, index=idx)


def main() -> None:
    args = parse_args()
    labels = load_labels(args.labels)

    model = tf.keras.models.load_model(str(args.model_path))
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=1,
        min_detection_confidence=0.6,
        min_tracking_confidence=0.35,
    )
    drawing_utils = mp.solutions.drawing_utils
    drawing_styles = mp.solutions.drawing_styles
    feature_extractor = LandmarkFeatureExtractor()
    frame_buffer: List[List[float]] = []
    last_prediction: Optional[PredictionResult] = None

    cap = cv2.VideoCapture(args.camera_index)
    if not cap.isOpened():
        print(f"[ERROR] Could not open camera index {args.camera_index}.", file=sys.stderr)
        sys.exit(1)

    print("Press 'q' to exit.")
    try:
        while True:
            success, frame = cap.read()
            if not success:
                print("[WARN] Failed to read frame from camera.", file=sys.stderr)
                break
            if args.flip_frame:
                frame = cv2.flip(frame, 1)

            rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(rgb)

            overlay_text = "No hand detected"
            if results.multi_hand_landmarks:
                hand_landmarks = results.multi_hand_landmarks[0]
                drawing_utils.draw_landmarks(
                    frame,
                    hand_landmarks,
                    mp_hands.HAND_CONNECTIONS,
                    drawing_styles.get_default_hand_landmarks_style(),
                    drawing_styles.get_default_hand_connections_style(),
                )
                frame_features = feature_extractor.from_landmarks(hand_landmarks)
                frame_buffer.append(frame_features)
                if len(frame_buffer) > args.sequence_length:
                    frame_buffer.pop(0)
            else:
                feature_extractor.reset()
                frame_buffer.clear()

            prediction: Optional[PredictionResult] = None
            if len(frame_buffer) >= args.min_frames:
                frames_np = np.array(frame_buffer, dtype=np.float32)
                inputs = tf.ragged.constant([frames_np], dtype=tf.float32)
                logits = model(inputs, training=False)
                if isinstance(logits, (list, tuple)):
                    logits = logits[0]
                probs = tf.nn.softmax(logits, axis=-1)[0].numpy()
                idx = int(np.argmax(probs))
                confidence = float(probs[idx])
                overlay_text = f"{format_label(idx, confidence, labels).label}: {confidence:.2f}"
                if confidence >= args.confidence_threshold:
                    prediction = format_label(idx, confidence, labels)
                    if (
                        last_prediction is None
                        or prediction.index != last_prediction.index
                        or abs(prediction.confidence - last_prediction.confidence) >= 1e-3
                    ):
                        print(f"[sign] {prediction.label} ({prediction.confidence:.2f})")
                        last_prediction = prediction

            cv2.putText(
                frame,
                overlay_text,
                (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (0, 0, 0),
                4,
                cv2.LINE_AA,
            )
            cv2.putText(
                frame,
                overlay_text,
                (10, 30),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.8,
                (255, 255, 255),
                2,
                cv2.LINE_AA,
            )

            window_name = "Sign Language Recognition"
            cv2.imshow(window_name, frame)

            key = cv2.waitKey(1) & 0xFF
            if key == ord("q") or key == 27:
                break
            if cv2.getWindowProperty(window_name, cv2.WND_PROP_VISIBLE) < 1:
                break
    finally:
        cap.release()
        hands.close()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
