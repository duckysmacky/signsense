#!/usr/bin/env python3
"""
Real-time tester for dactyl (finger spelling) recognition models.

The script mirrors the Android pipeline by:
  * capturing webcam frames,
  * extracting MediaPipe hand landmarks (x/y only),
  * flattening them to a 42-value vector (21 landmarks × 2),
  * feeding the vector into a PyTorch model compiled for dactyl recognition.

Recognised letters are overlayed on the webcam feed and printed to stdout when
the model confidence (raw score + 60, same as in the app) crosses a threshold.
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import os
from pathlib import Path
from typing import List, Optional, Sequence, Tuple

import cv2
import mediapipe as mp
import numpy as np
import torch
from torch import nn
try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:  # pragma: no cover - optional dependency
    Image = ImageDraw = ImageFont = None


DEFAULT_DICT_PATH = Path(__file__).resolve().parent / "dictionaries" / "dactyl_ru_v4.json"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Live dactyl (finger spelling) tester.")
    parser.add_argument("model_path", type=Path, help="Path to the PyTorch .pt model.")
    parser.add_argument(
        "--dictionary",
        type=Path,
        default=DEFAULT_DICT_PATH,
        help="Path to a label map (JSON or txt) that converts model classes to letters.",
    )
    parser.add_argument(
        "--threshold",
        type=float,
        default=80.0,
        help="Confidence threshold applied after adding the score offset (default: 80.0).",
    )
    parser.add_argument(
        "--score-offset",
        type=float,
        default=60.0,
        help="Score offset added to the raw logits before thresholding (mirrors ScoreManager in the Android app).",
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
        help="Flip the preview horizontally (useful for selfie cameras).",
    )
    parser.add_argument(
        "--flip-input",
        action="store_true",
        help="Flip landmark X coordinates before feeding them into the model (mirrors the mobile 'flip' preference).",
    )
    parser.add_argument(
        "--device",
        default="cpu",
        help="Torch device string to load the model on (default: cpu).",
    )
    parser.add_argument(
        "--cooldown-ms",
        type=int,
        default=500,
        help="Minimal duration (in milliseconds) between repeated announcements of the same letter.",
    )
    return parser.parse_args()


def load_labels(path: Path) -> Optional[List[str]]:
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
        print(f"[WARN] Failed to parse label map {path}: {exc}", file=sys.stderr)
        return None


def resolve_default_font() -> Optional[Path]:
    candidates: List[Path] = []
    windir = os.environ.get("WINDIR")
    if windir:
        candidates.extend(
            [
                Path(windir) / "Fonts" / "arial.ttf",
                Path(windir) / "Fonts" / "segoeui.ttf",
            ]
        )
    candidates.extend(
        [
            Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
            Path("/usr/share/fonts/truetype/freefont/FreeSans.ttf"),
            Path("/Library/Fonts/Arial Unicode.ttf"),
        ]
    )
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    return None


class TextRenderer:
    def __init__(self) -> None:
        self._pil_available = (
            Image is not None and ImageDraw is not None and ImageFont is not None
        )
        self._font = None
        if self._pil_available:
            font_path = resolve_default_font()
            if font_path:
                try:
                    self._font = ImageFont.truetype(str(font_path), 28)
                except Exception:
                    self._font = None

    def draw(self, frame: np.ndarray, text: str, position: Tuple[int, int]) -> np.ndarray:
        if not text:
            return frame
        image = Image.fromarray(frame)
        draw = ImageDraw.Draw(image)
        draw.text(
            position,
            text,
            font=self._font,
            fill=(255, 255, 255),
            stroke_width=3,
            stroke_fill=(0, 0, 0),
        )
        return np.array(image)

class DactylModel:
    def __init__(self, model_path: Path, device: str = "cpu") -> None:
        self.device = torch.device(device)
        self.model = self._load_model(model_path)
        self.model.eval()

    def _load_model(self, path: Path) -> nn.Module:
        try:
            return torch.jit.load(str(path), map_location=self.device)
        except (RuntimeError, ValueError):
            module = torch.load(str(path), map_location=self.device)
            if hasattr(module, "eval"):
                module.eval()
            return module

    def predict(self, features: np.ndarray) -> np.ndarray:
        tensor = torch.tensor(features, dtype=torch.float32, device=self.device).view(1, -1)
        with torch.no_grad():
            logits = self.model(tensor)
        if isinstance(logits, (list, tuple)):
            logits = logits[0]
        return logits.detach().cpu().numpy().reshape(-1)


def extract_xy_features(
    hand_landmarks: mp.framework.formats.landmark_pb2.NormalizedLandmarkList,
    flip_input: bool,
) -> List[float]:
    features: List[float] = []
    for landmark in hand_landmarks.landmark:
        x = 1.0 - landmark.x if flip_input else landmark.x
        features.extend([x, landmark.y])
    return features


def format_label(idx: int, labels: Optional[Sequence[str]]) -> str:
    if labels and 0 <= idx < len(labels):
        return labels[idx]
    return f"class_{idx}"


def main() -> None:
    args = parse_args()
    labels = load_labels(args.dictionary)
    if not labels:
        raise RuntimeError(
            "No labels were loaded. Ensure the dictionary file exists and contains at least one entry."
        )

    model = DactylModel(args.model_path, device=args.device)
    mp_hands = mp.solutions.hands
    hands = mp_hands.Hands(
        static_image_mode=False,
        max_num_hands=1,
        min_detection_confidence=0.6,
        min_tracking_confidence=0.4,
    )
    drawing_utils = mp.solutions.drawing_utils
    drawing_styles = mp.solutions.drawing_styles

    cap = cv2.VideoCapture(args.camera_index)
    if not cap.isOpened():
        print(f"[ERROR] Could not open camera index {args.camera_index}.", file=sys.stderr)
        sys.exit(1)

    last_label: Optional[str] = None
    last_emit_ms = 0.0
    text_renderer = TextRenderer()

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
                features = extract_xy_features(hand_landmarks, args.flip_input)
                scores = model.predict(np.array(features, dtype=np.float32))
                best_idx = int(np.argmax(scores))
                adjusted_score = float(scores[best_idx]) + args.score_offset
                candidate_label = format_label(best_idx, labels)
                overlay_text = f"{candidate_label}: {adjusted_score:.1f}"

                now_ms = time.time() * 1000
                if (
                    adjusted_score >= args.threshold
                    and (candidate_label != last_label or now_ms - last_emit_ms >= args.cooldown_ms)
                ):
                    print(f"[dactyl] {candidate_label} ({adjusted_score:.1f})")
                    last_label = candidate_label
                    last_emit_ms = now_ms

            frame = text_renderer.draw(frame, overlay_text, (10, 30))

            window_name = "Dactyl Recognition"
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
