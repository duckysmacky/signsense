import importlib
import subprocess

required_libraries = { #add something if u need
    "numpy": "numpy",
    "pandas": "pandas", 
    "matplotlib": "matplotlib",
    "sklearn": "scikit-learn",
    "tensorflow": "tensorflow",
    "torch": "torch",
    "cv2": "opencv-python",
    "mediapipe": "mediapipe"
}

def check_and_install():
    for import_name, package_name in required_libraries.items():
        try:
            importlib.import_module(import_name)
            print(f"✅ {import_name} доступен")
        except ImportError:
            print(f"📦 Устанавливаю {package_name}...")
            subprocess.run(["pip3", "install", package_name], check=True) #change pip3 to pip if 
            print(f"✅ {package_name} установлен")                        #error occures


check_and_install()
