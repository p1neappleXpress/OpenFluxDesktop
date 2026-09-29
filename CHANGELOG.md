# Changelog

All notable changes to OpenFluxDesktop. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- «Своя нода»: a new channel is no longer Yandex-only. The transports step
  takes any mix of a Yandex document (your own link), a Mail.ru public
  document and cups.online rooms (created automatically), with direct
  always on as the backup; the link and the saved profile carry all of
  them.
- «Автообновление ядра» on the plan step (on by default): the server's
  `openflux-node-update.timer` checks the newest `node-v*` release every
  6 hours, verifies it against the release's `node-install.sh` and
  `SHA256SUMS`, restarts the channels and rolls back if one does not stay
  up. Works on any systemd distribution.
- macOS: «Весь трафик компьютера (TUN)», the core's utun client (as the
  Wintun full tunnel on Windows). The core needs root: on each connect
  macOS asks for an administrator's password; OpenFlux itself runs as a
  normal app. Disconnecting, quitting or a crash of OpenFlux stops the
  root core, which puts the routes back.
- Windows: the full tunnel no longer needs OpenFlux restarted as
  administrator. On each connect Windows asks for permission (UAC) for the
  core alone; disconnecting, quitting or a crash of OpenFlux ends it, and
  the Wintun adapter goes with its routes.

### Changed

- The node wizard no longer signs in to Yandex: the document step takes
  the link of a document you created, and the node gets no account
  cookies. Solving a captcha on the client, or for a node through the
  client, works as before.
- The core also brings, from its main branch:
  - the node wizard installs nodes with the new `node-install.sh` (still
    `node-v1.1.0`), tested on Ubuntu 20.04–24.04, Debian 12–13, Rocky 9,
    Alma 8, Fedora 42, Arch and openSUSE Leap 15.6. On the server,
    `sudo sh /opt/openflux-node/node-install.sh list` shows the channels,
    `remove <channel>` deletes one and `uninstall` removes the node
    completely (channels, core, updater, user);
  - a Windows exit in `l3` mode works (WinDivert), for nodes run by hand;
  - the Go module is `github.com/p1neappleXpress/OpenFlux`.

### Fixed

- The speed and traffic counters work for classic profiles too, not only
  Session ones: bumps `OpenFlux` to
  [`96a0f95`](https://github.com/p1neappleXpress/OpenFlux/commit/96a0f95) (the core reports
  traffic over IPC without a Session, and MAX counts its bytes) and
  `shared` to [`a8bd71e`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/a8bd71e).
- A classic profile in the full tunnel shows as connected once the tunnel
  is up.

### Removed

- The node wizard no longer hands the node a Yandex sign-in (it had no way
  in from the UI since the document step takes your own link).

## [2.1.0] - 2026-09-28

### Added

- A classic profile runs as the exit node: the same l4 exit a Session
  profile starts, with the profile's transport, codec, document and key,
  serving its classic clients and Session clients alike; the link for
  clients shows on the home card. Exit mode no longer demands a Session
  profile.

### Changed

- Share links are read and made by the core, the way every client does:
  bumps `OpenFlux` to [`2ec01a5`](https://github.com/p1neappleXpress/OpenFlux/commit/2ec01a5) (core 0.2.0) and `shared` to
  [`ae5e59a`](https://github.com/p1neappleXpress/OpenFluxClientShared/commit/ae5e59a).
  - A link that picked up line breaks, spaces, non-breaking or zero-width
    characters, padding or the standard base64 alphabet on the way imports,
    as on iOS, instead of «Ссылка повреждена».
  - The same profile makes the same link on Desktop, Android and iOS; the
    core names the encryption context of a Session link, the app no longer
    derives it.
  - A refused link says why: not a link, cut short, letters changed case,
    unknown transport, key too short, and so on.
  - The node wizard installs `node-v1.1.0`, the node build of core 0.2.0.
- The node wizard's document step takes the link of a document you
  created; the button that signed in to Yandex and created one is gone.
- The release notes show this changelog.

### Fixed

- Clients and nodes built from different trees now connect: bumps `OpenFlux`
  to [`f8f3476`](https://github.com/p1neappleXpress/OpenFlux/commit/f8f34767a5732febd5965ac6cf95bad51b70cf99).
  - A classic profile with a key runs the Session and falls back to the
    classic layering for a classic or older node, on the same carrier; a
    node set up as classic serves both kinds of client. Nodes set up for
    the Session stay Session-only.
  - The encryption context follows one rule everywhere, and a client whose
    context differs from the node's finds the node's instead of timing out
    (classic cups.online was the common case).
  - The classic codec (batched or legacy) is no longer a hard requirement:
    both are accepted and the client switches when the node does not answer.
  - boards no longer drops the connection every 20 seconds; yandex and
    mailru reconnect when their socket dies.
  - The log explains a failed handshake: wrong key, a node in the other
    mode, the codec or context picked, a connection taken over by another
    client.

## [2.0.2] - 2026-09-27

### Fixed

- An exit node deployed by the node wizard never actually connected: a
  `.conf`-only `Role = exit` (every node-wizard deployment) left the core's
  internal exit/client flag stuck at its pre-config value, so the exit
  never answered the handshake and crash-looped instead. Bumps `OpenFlux`
  to [`e8f735a`](https://github.com/p1neappleXpress/OpenFlux/commit/e8f735a98c1ba9e091956416fde5c5ef92d3cd66).
- The startup log always printed `Transport: yandex` for session/multi-transport
  profiles regardless of which transports were actually configured (a stale
  flag default, not a functional bug — the correct transports ran either
  way). Now prints the actual list, e.g. `Transport: boards, direct (session)`.
- Cups.online profiles with no room codes couldn't be saved or connected;
  the node generates its own rooms, so an empty value is valid for this
  transport only.
- An exit node always listened for Direct (TCP on `0.0.0.0:<port>`) and put
  it into the clients' link and QR, even when the profile had no Direct
  transport: choosing two transports gave clients three. It now listens
  only when the profile has Direct, at that transport's priority.
  [OpenFluxClientShared#6](https://github.com/p1neappleXpress/OpenFluxClientShared/pull/6).
- A cups.online exit started without room codes left cups.online out of its
  link and QR, so clients had no rooms to join; the link now carries the
  rooms the node created, and is printed again if they change. Bumps
  `OpenFlux` to [OpenFlux#123](https://github.com/p1neappleXpress/OpenFlux/pull/123).
- After switching profiles the "через …" badge could keep the previous
  profile's carrier (for good if the new profile is classic) and an exit
  its old QR: a stopped core's last status and output no longer overwrite
  the new state.

### Added

- Settings → Ядро OpenFlux: a core log-level picker (Выкл / -d / -dd /
  -ddd, the core's `--debug=N`) in place of the "Подробный журнал ядра"
  switch, which could only turn -dd on. -dd is the level that shows
  sessions, handshakes and the encryption (KDF) context; -ddd adds packet
  hexdumps. Bumps `shared` to
  [OpenFluxClientShared#5](https://github.com/p1neappleXpress/OpenFluxClientShared/pull/5).
- Node-wizard deployment logging: every SSH/RPC call and the wizard's own
  step narration now goes to the Logs tab, so a stuck deployment is
  diagnosable without a debugger.
- Home → Подключение: a "Сейчас через" row for profiles with several
  transports, and carriers named as in the app ("Board 2", not `boards-2`)
  there and in the "через …" badge.

## [2.0.1] - 2026-09-27

### Fixed

- Settings → Core → "Выбрать…" filtered the file dialog to `*.exe`
  unconditionally, so on macOS/Linux (where the core binary has no
  extension) it showed nothing and the picker was unusable; the path
  field still took a manually typed/pasted path. Bumps `shared` to
  [OpenFluxClientShared#2](https://github.com/p1neappleXpress/OpenFluxClientShared/pull/2).

## [2.0.0] - 2026-09-27

First release of this app. Replaces the previous independent desktop
client (`tech.p1neapplexpress.openfluxdesktop`, preserved at the
[`legacy-native-app`](../../tree/legacy-native-app) tag) with the Compose
Multiplatform app originally built by [@meepo161](https://github.com/meepo161)
in [OpenFluxClient](https://github.com/meepo161/OpenFluxClient), moved here
with his agreement.

### Added

- Windows/macOS/Linux: full tunnel (Wintun on Windows) or SOCKS5/HTTP
  proxy.
- Multi-transport sessions with automatic failover and priority-based
  switching (including `direct`).
- AES-256-GCM session encryption.
- SmartCaptcha and login handling in a built-in browser (KCEF), including
  a transport check on the exit node, passed through the tunnel from its
  own address.
- A node-deployment wizard: install an exit node on your own VPS over SSH
  from the app.
- `shared/` and `OpenFlux/` as git submodules ([OpenFluxClientShared](https://github.com/p1neappleXpress/OpenFluxClientShared)
  and the [OpenFlux](https://github.com/p1neappleXpress/OpenFlux) core), so
  this app always builds against one pinned, single copy of each instead of
  a vendored one; `prepareCore`'s fallback source now points at that core
  repository instead of a third-party fork.
- `.github/workflows/release.yml`: a `v*` tag builds Windows/Linux/macOS
  packages and publishes them here.

### Credits

- [@meepo161](https://github.com/meepo161) — this app's UI and logic.
- [@p1neappleXpress](https://github.com/p1neappleXpress) — the OpenFlux
  core it bundles.
- [@damnurmum](https://github.com/damnurmum) — `openflux://` links/QR codes
  and the cups.online transport in the core, which this app's share and
  scan screens build on.
