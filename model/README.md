# Live Testing Scripts

This folder contains two webcam-based utilities for validating the dactyl and continuous sign language models right on your workstation. Both scripts rely on MediaPipe for extracting hand landmarks, then mirror the preprocessing used during training before handing the data off to their respective models.

## Prerequisites

- Python 3.9.x (the project uses 3.9.13).
- Install dependencies once:  
  ```bash
  pip install -r model/requirements.txt
  ```
- A webcam accessible via OpenCV.

Press `q`, `Esc`, or close the window to exit either script.

## Dactyl (PyTorch)

```bash
python model/live_dactyl.py <path_to_model.pt>
```

- A default Russian alphabet dictionary is bundled in `model/dictionaries/ru_dactyl_v4.json`.  
  Provide your own mapping (JSON list, JSON dict, or newline-delimited txt) with `--dictionary path/to/file`.
- Additional options mirror the Android app:
  - `--flip-frame` flips the webcam preview (useful for selfie cams).
  - `--flip-input` mirrors the landmark X axis before inference.
  - `--threshold`, `--score-offset`, and `--cooldown-ms` tune the ScoreManager-like logic that decides when to announce a new letter.
- Detected letters are overlayed on the video (with full Cyrillic support) and printed to the terminal when the adjusted score crosses the threshold.

## Sign Language (TensorFlow / Keras)

```bash
python model/live_sign_language.py <path_to_model.keras> --labels <path_to_labels.json>
```

- The script reproduces the `get_landmarks.py` features (21×8 vector per frame) and feeds a rolling window into your sequence model.
- Supply a label map (JSON list/dict or newline-delimited txt) with `--labels` so class indices are translated to words/phrases.
- Tweak `--sequence-length`, `--min-frames`, and `--confidence-threshold` to match your model’s expectations.
- Only one window is shown; predictions are printed once their softmax probability exceeds the threshold.

---

# Скрипты для живого тестирования

В этой папке находятся два утилитарных скрипта, позволяющих проверять модели дактиля и жестового языка через веб-камеру. Оба используют MediaPipe для извлечения ключевых точек руки, повторяют предпросчет из обучения и затем передают данные в соответствующую модель.

## Подготовка

- Python 3.9.x (в проекте используется 3.9.13).
- Зависимости (один раз):  
  ```bash
  pip install -r model/requirements.txt
  ```
- Веб-камера, доступная через OpenCV.

Выход — клавиша `q`, `Esc` или закрытие окна.

## Дактиль (PyTorch)

```bash
python model/live_dactyl.py <path_to_model.pt>
```

- По умолчанию используется словарь `model/dictionaries/dactyl_ru_v4.json`.  
  Можно указать свой JSON/текстовый файл параметром `--dictionary`.
- Дополнительные параметры повторяют настройки Android-приложения:
  - `--flip-frame` зеркалирование превью (для фронтальной камеры).
  - `--flip-input` отражение координат X перед распознаванием.
  - `--threshold`, `--score-offset`, `--cooldown-ms` тонкая настройка логики ScoreManager, отвечающей за объявление новых букв.
- Распознанные буквы накладываются на изображение (поддерживается кириллица) и выводятся в терминал при превышении порога уверенности.

## Жестовый язык (TensorFlow / Keras)

```bash
python model/live_sign_language.py <path_to_model.keras> --labels <path_to_labels.json>
```

- Скрипт формирует признаки так же, как `get_landmarks.py`, и подает скользящее окно кадров в последовательную модель.
- Чтобы индексы классов превращались в слова, передайте карту меток (JSON или текст) через `--labels`.
- Параметры `--sequence-length`, `--min-frames`, `--confidence-threshold` можно подстроить под конкретную архитектуру.
- Используется одно окно; предсказания выводятся только после достижения заданной вероятности.
