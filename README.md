# Ghost Transfer — Android

Приложение для передачи файлов и текста по локальной сети.
Совместимо с Windows PowerShell скриптом `transfer_v6_11_3.bat` (Ghost Transfer for Windows).

**Проект в активной разработке — версия v11-beta.**

## Возможности
- 📡 Сервер: раздаёт файл и текст по HTTP (порт 12500, совместим с Windows-клиентом)
- 📥 Клиент: сканирует сеть, скачивает файлы и текст
- 📒 Контакты: записная книжка устройств с ping-проверкой
- 📂 Встроенный файловый браузер (без SAF — работает на TV Box)
- 📺 TV Box: landscape-layout, D-pad навигация
- 🌐 ZeroTier / VPN: ручной ввод IP для устройств вне локальной сети

## Протокол
HTTP порт 12500. Эндпоинты:
- `GET /ping` → `OK`
- `GET /info` → `HasFile=0|1\nFileName=...\nHasText=0|1`
- `GET /text` → текст
- `GET /file` → файл (attachment)

## Сборка локально
```bash
# Требует: openjdk-21-jdk, aapt, dalvik-exchange, zipalign, apksigner
# android-sdk-platform-23, libandroid-23-java (из репозиториев Ubuntu)
bash build_apk.sh
```

## GitHub Actions
APK собирается автоматически при каждом push — скачать можно во вкладке Actions.
