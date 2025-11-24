import pandas as pd
import numpy as np
import ijson  # для потоковой обработки больших JSON
from tqdm import tqdm

path_annotations = 'model/data/annotations.csv'
path_landmarks = 'model/data/slovo_mediapipe.json'

print("1. Загрузка аннотаций...")
targets = pd.read_csv(path_annotations, sep='\t')
print("✅ Аннотации загружены")

print(f"Колонки: {targets.columns.tolist()}")
print(f"Размер: {targets.shape}")

# Создаем mapping из ID в текст
id_mapping = dict(zip(targets['attachment_id'].astype(str), targets['text']))
print(f"Создано {len(id_mapping)} соответствий ID->текст")

print("2. Потоковая загрузка landmarks...")

def process_large_json_streaming(file_path, id_mapping, max_sessions=None):
    """Обработка большого JSON файла потоково"""
    data = {}
    processed_count = 0
    
    with open(file_path, 'r', encoding='utf-8') as f:
        # Используем ijson для потокового парсинга
        sessions = ijson.kvitems(f, '')
        
        for session_id, frames in tqdm(sessions, desc="Обработка сессий"):
            # Проверяем есть ли этот ID в аннотациях
            if session_id in id_mapping:
                text = id_mapping[session_id]
                data[text] = frames
                processed_count += 1
                
                if processed_count % 100 == 0:
                    print(f"Обработано соответствий: {processed_count}")
            
            # Ограничение для теста
            if max_sessions and processed_count >= max_sessions:
                print(f"Достигнут лимит в {max_sessions} сессий")
                break
    
    return data, processed_count

# Обрабатываем только первые 1000 сессий для теста
data, matched_count = process_large_json_streaming(
    path_landmarks, 
    id_mapping# Ограничиваем для теста
)

print(f"✅ Обработано: {matched_count} соответствий")
print(f"Уникальных жестов: {len(set(data.keys()))}")

# Показываем примеры
print("\nПримеры данных:")
for text, frames in list(data.items())[:3]:
    print(f"  '{text}': {len(frames)} кадров")
    if frames:
        print(f"    Первый кадр: {len(frames[0]['hand 1'])} landmarks")