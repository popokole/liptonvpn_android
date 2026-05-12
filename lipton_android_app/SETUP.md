# LiptonVPN Android — Инструкция по сборке

## 1. Добавление xray-core бинарника

Приложение использует xray-core для VPN-туннеля. Нужно добавить бинарник вручную:

### Скачать бинарник
1. Перейти на https://github.com/XTLS/Xray-core/releases
2. Скачать версию для Android:
   - `Xray-android-arm64-v8a.zip` — для ARM64 (большинство современных телефонов)
   - `Xray-android-arm32-v7a.zip` — для старых ARM устройств

### Установить бинарник
Распакуйте архив и скопируйте файл `xray` (без расширения) в:

```
app/libs/jniLibs/arm64-v8a/libxray.so    ← переименовать xray → libxray.so
app/libs/jniLibs/armeabi-v7a/libxray.so  ← для ARM32 (опционально)
app/libs/jniLibs/x86_64/libxray.so       ← для x86_64 эмуляторов (опционально)
```

### Добавить geo-данные
Скачайте и поместите в `app/src/main/assets/xray/`:
- `geoip.dat`   — https://github.com/v2fly/geoip/releases
- `geosite.dat` — https://github.com/v2fly/domain-list-community/releases

Приложение автоматически найдёт geo-файлы рядом с бинарником в `nativeLibraryDir`.

## 2. Сборка

1. Открыть папку `lipton_android_app` в **Android Studio Hedgehog** (или новее)
2. Дождаться синхронизации Gradle
3. Нажать **Run** или собрать APK через **Build → Build APK**

### Требования
- Android Studio Hedgehog 2023.1.1+
- JDK 17
- Android SDK 34

## 3. Структура проекта

```
app/src/main/java/com/lipton/vpn/
├── MainActivity.kt              — точка входа
├── MainViewModel.kt             — управление состоянием
├── data/
│   ├── model/Server.kt          — модель сервера (vless/vmess/trojan)
│   ├── model/Subscription.kt    — модель подписки
│   ├── SettingsManager.kt       — DataStore настройки
│   ├── SubscriptionManager.kt   — загрузка и парсинг подписок
│   └── XrayConfigGenerator.kt   — генерация JSON-конфига xray
├── service/
│   ├── LiptonVpnService.kt      — Android VpnService + управление xray
│   └── BootReceiver.kt          — автозапуск при загрузке
└── ui/
    ├── MainScreen.kt            — главный экран
    ├── theme/Theme.kt           — тёмно-зелёная тема
    └── components/
        ├── ConnectButton.kt     — анимированная кнопка подключения
        ├── ServerList.kt        — список серверов с пингом
        ├── SubscriptionPanel.kt — управление подписками
        └── SettingsPanel.kt     — настройки (bypass RU, домены, сброс)
```

## 4. Публикация в Google Play

- `minSdk`: 24 (Android 7.0, 2016) — поддержка 99%+ устройств
- `targetSdk`: 34 (Android 14)
- Протоколы: VLESS, VMess, Trojan (Reality, TLS, WS, gRPC)
- Подписки только с домена `sub.popokole.online`
