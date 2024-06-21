# SignSense
![Signsense_text_wide_trans](https://github.com/shroomwastaken/signsense/assets/94703516/b2b75b0a-a9a7-4c79-950a-10bebe58a6bb)
### An Android app for translating dactyl sign language into text

*because knowing sign language is pretty handy*

## About

Signsense is a dactyl translator app developed by two high school students as a (school) project. The app currently supports **only one language for translation** (Russian Dactyl), but we have plans of adding support for English dactyl and ASL for both languages as well. This app is still very much **just a proof-of-concept project** and was recently again taken under development.

Currently theres a goal of rewriting the whole app in order to optimise load times, improve detection, add support for ASL, enable simultanious detection with multiple models and just overall improve the app.

## Features

Current features:
- 4 menus (Main menu, camera translator, information and settings)
- Live dactyl detection and translation from rear and front cameras
- Information menu with proper guide and credits
- Settings menu to configure detection, translation and app itself
- 2 app themes (Light and Dark)
- 2 interface languages (English and Russian)
- 1 translation language (Only Russian *for now*)

Upcoming features:
- English dactyl support
- Russian and English sign language support

## Usage

- When using camera translator, make sure to stand about 1 meter from the signer for proper hand tracking and have the signer's hands visible within the frame, or else the app will not be able to properly track their hands.
- In case of situations with low light levels, use the flashlight function (press lighting icon to toggle).
- After aiming the camera at the signer, the app will immediately track their hands.
- Once the sign will be recognised, it will be shown in the "letter" window and will be added to the queue of "translated word" window.
- Once the app recognises a break between signs, it will mark the word as completed, clear the "translated word" window and add the word to the bottom of the screen, which shows all recently translated words.
- Upon closing the translator this list will be cleared.

## Installation

### Notice: The app was tested on Android versions **8, 9, 10, 11 and 12**, minimum supported version is **Android 8**.

1. Download the APK file from [here](https://www.bit.ly/signsense) or from [releases](https://github.com/duckysmacky/signsense/releases)
2. Open and **install** the apk file
3. Done! The app is now installed on your phone

**If your browser is blocking you from downloading the APK file on mobile:** enable [download from unknown sources](https://www.applivery.com/docs/mobile-app-distribution/android-unknown-sources/) feature in settings

## Bugs and feature requests

The app **might still crash** and **have several bugs** as this release was a bit rushed because of our project presentation. Make sure to submit all bugs to [issues](https://github.com/duckysmacky/signsense/issues), I will try to fix them as soon as possible. Any other feature requests can also be submited there.

## Credits

- [Ducky](github.com/duckysmacky) - lead developer, backend and frontend
- [shroom](github.com/shroomwastaken) - model training and ai specialist
