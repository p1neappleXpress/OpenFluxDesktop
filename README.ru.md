<p align="center">
  <img src="docs/images/icon.png" width="120" alt="OpenFlux">
</p>

<h1 align="center">OpenFluxDesktop</h1>

<p align="center">
  Клиент OpenFlux для Windows, macOS и Linux.<br>
  Туннель поверх обычных веб-сервисов с автоматическим переключением между ними.
</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Релиз" src="https://img.shields.io/github/v/release/p1neappleXpress/OpenFluxDesktop?display_name=tag&color=4F7CFF"></a>
  <a href="LICENSE"><img alt="Лицензия" src="https://img.shields.io/badge/license-GPL--3.0--or--later-blue"></a>
  <img alt="Платформы" src="https://img.shields.io/badge/platforms-Windows%20%C2%B7%20macOS%20%C2%B7%20Linux-lightgrey">
</p>

<p align="center">
  <a href="README.md">English</a> · <b>Русский</b>
</p>

<p align="center">
  <a href="https://github.com/p1neappleXpress/OpenFlux">Core</a> · <a href="https://github.com/p1neappleXpress/OpenFluxDesktop">Desktop</a> · <a href="https://github.com/p1neappleXpress/OpenFluxAndroid">Android</a> · <a href="https://testflight.apple.com/join/BwnAcdus">iOS (TestFlight, скоро в App Store)</a>
</p>

<p align="center">
  <img src="docs/images/desktop-home.png" alt="OpenFlux на компьютере: экран подключения и боковое меню" width="860">
</p>

## Что это

OpenFluxDesktop — приложение для компьютера на базе [OpenFlux](https://github.com/p1neappleXpress/OpenFlux). Оно запускает ядро OpenFlux отдельным процессом и даёт ему окно, значок в трее и мастер настройки. Трафик идёт через **транспорт**: документ на веб-сервисе (Яндекс, Mail.ru, cups.online, Avito, Телемост, MAX) или обычное TCP-соединение с вашей нодой. В профиле может быть несколько транспортов; когда один перестаёт работать, приложение переходит на следующий по приоритету.

## Возможности

- **Весь трафик или прокси.** Весь трафик компьютера через Wintun (Windows) или utun (macOS), либо локальный прокси SOCKS5 и HTTP. В Windows можно ещё и направить на него системный прокси на время подключения.
- **Несколько транспортов в одном профиле**, у каждого свой приоритет и сеть, по которой ему можно ходить.
- **Шифрование.** AES-256-GCM от края до края с вашим ключом: сервис, который несёт трафик, его не видит.
- **Нода в несколько нажатий.** Мастер ставит выходную ноду на ваш сервер по SSH или на обычный PHP-хостинг, вообще без сервера. Нода обновляется сама и откатывается, если обновление не запустилось.
- **Поделиться ссылкой или QR.** Ссылка `openflux://` несёт весь профиль.
- **Встроенный браузер** для проверок, которые просят некоторые сервисы (SmartCaptcha, вход в аккаунт).
- **Нода на этом компьютере.** Режим «Выходная нода» превращает приложение в ноду, через которую подключаются другие.
- **Обновления из релизов GitHub** со сверкой по `SHA256SUMS.txt`; ставятся только с вашего согласия.

<p align="center">
  <img src="docs/images/desktop-servers.png" alt="Список серверов на компьютере" width="860">
</p>

## Установка

Скачайте файл для своей системы на [странице релизов](../../releases/latest).

| Система | Файл |
|---|---|
| Windows 10/11 x64 | `.msi` или `-setup.exe` для установки, `-portable.zip` без установки |
| Linux x64 | `.deb` (Debian, Ubuntu, Mint) или `.tar.gz` |
| macOS | `-macos-arm64.dmg` (Apple silicon) или `-macos-x64.dmg` (Intel) |

- Сборки для macOS не подписаны: при первом запуске откройте приложение правым кликом → *Открыть*.
- Режиму «весь трафик» нужны права администратора (запрос UAC в Windows, пароль в macOS). В Linux направьте программы на прокси SOCKS5 `127.0.0.1:1080` или HTTP `127.0.0.1:1081`.
- Встроенный браузер (около 230 МБ) скачивается при первом обращении к нему.

## Сборка из исходников

Нужны JDK 17 и Go (или Docker, чтобы собрать без Go).

```bash
git clone --recurse-submodules https://github.com/p1neappleXpress/OpenFluxDesktop.git
cd OpenFluxDesktop
./gradlew :desktopApp:run          # запуск; при первом запуске собирает ядро для этой машины
```

Клонировали без `--recurse-submodules`? Выполните `git submodule update --init --recursive`.

| Команда | Что делает |
|---|---|
| `./gradlew :desktopApp:run` | запустить приложение |
| `scripts/build-core.sh` | собрать ядро вручную из сабмодуля `OpenFlux/` |
| `./gradlew :shared:jvmTest` | тесты |
| `./gradlew -PwindowsPackage=true :desktopApp:packageMsi` | Windows `.msi` (ещё `packageExe`) |
| `./gradlew :desktopApp:packageDmg` | macOS `.dmg` |
| `./gradlew :desktopApp:packageDeb` | Linux `.deb` (нужен `fakeroot`) |

Бинарники ядра в git не лежат: `run` и `package*` сами собирают ядро для этой машины в `desktopApp/resources/<windows|macos|linux>`, если его там нет. `-PskipCore=true` запускает приложение без ядра; тогда *Настройки → Ядро* принимает ваш файл.

## Устройство

```
desktopApp/   Окно, трей, горячие клавиши, упаковка, значки
shared/       Сабмодуль: модели, интерфейсы сервисов, дизайн-система, экраны
  jvmMain/
    core/     Запуск процесса ядра, статус по IPC, проверки Яндекса
    node/     Мастер ноды (ядро --node-wizard через stdin/stdout)
    web/      Встроенный браузер: KCEF вне экрана, роутер прокси, журнал
    platform/ Системный прокси Windows (WinInet), права администратора
    data/     Хранение настроек и профилей
OpenFlux/     Сабмодуль: ядро (CLI), запускается подпроцессом
scripts/      build-core.sh
```

Те же экраны работают на Android в [OpenFluxAndroid](https://github.com/p1neappleXpress/OpenFluxAndroid); оба приложения используют модуль [OpenFluxClientShared](https://github.com/p1neappleXpress/OpenFluxClientShared).

<p align="center">
  <img src="docs/images/phones.png" alt="Тот же интерфейс на телефоне" width="780">
</p>

## Авторы

- **[meepo161](https://github.com/meepo161)** — автор [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), источника интерфейса и логики приложения (`desktopApp/`, `shared/`): сессии с несколькими транспортами и переключением, шифрование, встроенный браузер и проверки, мастер ноды и дизайн-система на Compose. Спасибо!
- **[p1neappleXpress](https://github.com/p1neappleXpress)** — автор ядра [OpenFlux](https://github.com/p1neappleXpress/OpenFlux), которое приложение несёт с собой: туннель, транспорты и протокол согласования.

Прежний независимый клиент для компьютера, жил в этом репозитории, сохранён под тегом [`legacy-native-app`](../../tree/legacy-native-app).

## Лицензия

GNU General Public License v3.0 или новее — см. [LICENSE](LICENSE).
