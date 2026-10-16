<p align="center">
  <img src="docs/images/icon.png" width="120" alt="OpenFlux">
</p>

<h1 align="center">OpenFluxDesktop</h1>

<p align="center">
  The OpenFlux client for Windows, macOS and Linux.<br>
  A tunnel that rides on ordinary web services, with automatic failover between them.
</p>

<p align="center">
  <a href="../../releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/p1neappleXpress/OpenFluxDesktop?display_name=tag&color=4F7CFF"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/license-GPL--3.0--or--later-blue"></a>
  <img alt="Platforms" src="https://img.shields.io/badge/platforms-Windows%20%C2%B7%20macOS%20%C2%B7%20Linux-lightgrey">
</p>

<p align="center">
  <b>English</b> · <a href="README.ru.md">Русский</a>
</p>

<p align="center">
  <a href="https://github.com/p1neappleXpress/OpenFlux">Core</a> · <a href="https://github.com/p1neappleXpress/OpenFluxDesktop">Desktop</a> · <a href="https://github.com/p1neappleXpress/OpenFluxAndroid">Android</a> · <a href="https://testflight.apple.com/join/BwnAcdus">iOS (TestFlight; the App Store release is coming)</a>
</p>

<p align="center">
  <img src="docs/images/desktop-home.png" alt="OpenFlux on the desktop: the connect screen and the sidebar" width="860">
</p>

## What it is

OpenFluxDesktop is the desktop app of [OpenFlux](https://github.com/p1neappleXpress/OpenFlux). It runs the OpenFlux core as a separate process and gives it a window, a tray icon and a setup wizard. Traffic goes through a **transport**: a document on a web service (Yandex, Mail.ru, cups.online, Avito, Telemost, MAX) or a plain TCP connection to your own node. A profile can hold several transports; the app switches between them by priority when one stops working.

## Features

- **Full tunnel or a proxy.** All of the computer's traffic through Wintun (Windows) or utun (macOS), or a local SOCKS5 and HTTP proxy. Windows can also point the system proxy at it while connected.
- **Several transports in one profile**, each with a priority and the network it may use. When the best one fails, the next takes over.
- **Encryption.** AES-256-GCM end to end with a key you set; a profile with a key hides the traffic from the service that carries it.
- **A node in a few clicks.** The wizard installs an exit node on your server over SSH, or sets one up on ordinary PHP hosting with no server at all. The node updates itself and rolls back if an update does not start.
- **Share by link or QR.** An `openflux://` link carries a whole profile.
- **A built-in browser** for the checks some services ask for (SmartCaptcha, a login wall).
- **Your own node on this computer.** The *Exit node* mode turns the app into a node that others connect through.
- **Updates from GitHub releases**, checked against the release's `SHA256SUMS.txt`, installed only when you agree.

<p align="center">
  <img src="docs/images/desktop-servers.png" alt="The servers list on the desktop" width="860">
</p>

## Install

Download the file for your system from the [releases page](../../releases/latest).

| System | File |
|---|---|
| Windows 10/11 x64 | `.msi` or `-setup.exe` to install, `-portable.zip` to run without installing |
| Linux x64 | `.deb` (Debian, Ubuntu, Mint) or `.tar.gz` |
| macOS | `-macos-arm64.dmg` (Apple silicon) or `-macos-x64.dmg` (Intel) |

- The macOS builds are not signed: the first time, open the app with a right click → *Open*.
- Full-tunnel mode needs administrator rights (a UAC prompt on Windows, the password on macOS). On Linux, point programs at the SOCKS5 proxy `127.0.0.1:1080` or the HTTP proxy `127.0.0.1:1081`.
- The built-in browser (about 230 MB) is downloaded the first time it is needed.

## Build from source

Needs JDK 17 and Go (or Docker, to build without Go).

```bash
git clone --recurse-submodules https://github.com/p1neappleXpress/OpenFluxDesktop.git
cd OpenFluxDesktop
./gradlew :desktopApp:run          # run; builds the core for this machine on the first run
```

Already cloned without `--recurse-submodules`? Run `git submodule update --init --recursive`.

| Command | What it does |
|---|---|
| `./gradlew :desktopApp:run` | run the app |
| `scripts/build-core.sh` | build the core by hand from the `OpenFlux/` submodule |
| `./gradlew :shared:jvmTest` | run the tests |
| `./gradlew -PwindowsPackage=true :desktopApp:packageMsi` | Windows `.msi` (also `packageExe`) |
| `./gradlew :desktopApp:packageDmg` | macOS `.dmg` |
| `./gradlew :desktopApp:packageDeb` | Linux `.deb` (needs `fakeroot`) |

The core binaries are not committed to git: `run` and `package*` build the one for this machine into `desktopApp/resources/<windows|macos|linux>` if it is not there. `-PskipCore=true` runs the app without a core; *Settings → Core* then takes a file of your own.

## Structure

```
desktopApp/   Window, tray, shortcuts, packaging, icons
shared/       Submodule: models, service interfaces, design system, screens
  jvmMain/
    core/     Core process launch, IPC status, Yandex checks
    node/     The node wizard (core --node-wizard over stdin/stdout)
    web/      The built-in browser: off-screen KCEF, a proxy router, a log
    platform/ Windows system proxy (WinInet), administrator rights
    data/     Settings and profile storage
OpenFlux/     Submodule: the core (CLI), started as a subprocess
scripts/      build-core.sh
```

The same screens run on Android in [OpenFluxAndroid](https://github.com/p1neappleXpress/OpenFluxAndroid); both use the module [OpenFluxClientShared](https://github.com/p1neappleXpress/OpenFluxClientShared).

<p align="center">
  <img src="docs/images/phones.png" alt="The same interface on a phone" width="780">
</p>

## Credits

- **[meepo161](https://github.com/meepo161)** — author of [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), the source of this app's UI and logic (`desktopApp/`, `shared/`): multi-transport sessions with failover, encryption, the built-in browser and check flow, the node wizard and the Compose design system. Thank you!
- **[p1neappleXpress](https://github.com/p1neappleXpress)** — author of the [OpenFlux](https://github.com/p1neappleXpress/OpenFlux) core this app bundles: the tunnel, the transports and the negotiation protocol.

The previous, independent desktop client that lived in this repository is kept at the [`legacy-native-app`](../../tree/legacy-native-app) tag.

## License

GNU General Public License v3.0 or later — see [LICENSE](LICENSE).
