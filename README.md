![Signsense_text_wide_trans](https://github.com/shroomwastaken/signsense/assets/94703516/b2b75b0a-a9a7-4c79-950a-10bebe58a6bb)
# SignSense
#### Android app which translates sign language to English / Russian

*because knowing sign language is pretty handy*

---

Signsense is a dactyl-translator app developed by two high school students as a (school) project. The app currently supports two UI languages and only **one language for translation** (Russian Dactyl), but we have plans of adding support for English dactyl and ASL. This app is still very much **just a proof-of-concept project** and **currently put on hold**, but in case of visible intrest of the publuc we *could continue the development in the future*.

## Features
Currently the app features:
- 4 menus (Main menu, camera translator, information and settings)
- Live Russian dactyl translation from rear camera
- Information menu with proper guide and credits
- Settings menu to configure app itself, detection and translation
- 2 app themes (Light and Dark)
- 2 interface languages (English and Russian)
- 1 translation language (Only Russian *for now*)

## Usage
- When using camera translator, make sure to stand about 1 meter from the signer for proper hand tracking and have the signer's hands visible within the frame, or else the app will not be able to properly track their hands.
- In case of situations with low light levels, use the flashlight function (press lighting icon to toggle).
- After aiming the camera at the signer, the app will immediately track their hands.
- Once the sign will be recognised, it will be shown in the "letter" window and will be added to the queue of "translated word" window.
- Once the app recognises a break between signs, it will mark the word as completed, clear the "translated word" window and add the word to the bottom of the screen, which shows all recently translated words.
- Upon closing the translator this list will be cleared.

## Installation
### The app was tested on Android versions **8, 9, 10, 11 and 12**, minimum supported version is **Android 8**.
To install the app and try it out yourself:
1. Download the APK file below (**if your browser is blocking you from downloading, enable [download from unknown sources](https://www.applivery.com/docs/mobile-app-distribution/android-unknown-sources/) feature in settings**)
2. Click on the file, press *install*
3. Wait for the installation to finish and you are done!

## Bugs and feature requests
The app **might still crash** and **have several bugs** as this release was a bit rushed because of our project presentation. Make sure to submit all bugs to [issues](https://github.com/duckysmacky/signsense/issues), I will try to fix them as soon as possible. Any other feature requests can also be submited there.

## Credits
- [Ducky](github.com/duckysmacky) - lead dev and design
- [shroom](github.com/shroomwastaken) - model training and ai specialist

---

*The app itself is pretty big (around 450mb) because of different libraries (such as OpenCV, Mediapipe and PyTorch) used in the project*

### Because of the this big file size of the .apk file, Github doesn't really want to upload it here for some reason, so below there is a download link to the same .apk file from Mediafire. I will try to solve this issue as soon as possible.

# [DOWNLOAD](bit.ly/signsense)
