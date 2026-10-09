# FFmpeg binary licensing

Findings from checking the three FFmpeg sources pinned in
`backend/build.gradle` (`ffmpegBuilds`) for explicit license
information. Recorded while resolving the open licensing question from
the FFmpeg 9.0.2 upgrade.

## Sources

### martin-riedl.de (linux-amd64, macos-arm64, macos-amd64)

- **The archives ship no license text at all.** Each zip contains a
  single bare `ffmpeg` or `ffprobe` binary — no README, no COPYING, no
  attribution file.
- The build server site (https://ffmpeg.martin-riedl.de/) states no
  license for the binaries either. Only the *build script* is licensed
  (Apache 2.0, `git.martin-riedl.de/ffmpeg/build-script`, "Copyright
  2021 Martin Riedl") — that covers the script, not its output.
- The binaries themselves identify their build configuration
  (`ffmpeg -version` / embedded `configuration:` string):

| platform | GPL-relevant configuration flags |
|---|---|
| linux-amd64 | `--enable-gpl --enable-version3 --enable-nonfree` (+ decklink, openssl, x264, x265, lame, ...) |
| macos-arm64 | `--enable-gpl --enable-version3 --enable-openssl` (no `--enable-nonfree`, no decklink) |
| macos-amd64 | `--enable-gpl --enable-version3 --enable-openssl` (same as arm64) |

- The macOS builds therefore follow FFmpeg's standard GPLv3-or-later
  terms (GPL + version3 + Apache-2.0 openssl is a compatible set).
- **The linux-amd64 build is the problem**: `--enable-nonfree` marks it
  as containing components whose terms are incompatible with the GPL.
  The nonfree part here is the **Blackmagic DeckLink SDK**
  (`--enable-decklink`; the build script documents that the SDK must be
  fetched from blackmagicdesign.com manually — it is proprietary and
  not redistributable). Per the FFmpeg project's legal guidance
  (https://www.ffmpeg.org/legal.html), a binary built with
  `--enable-nonfree` cannot be redistributed under GPL terms and needs
  separate permission from the rights holders of the nonfree parts.

### gyan.dev (windows-amd64)

- **Explicit license statement**: the zip's `README.txt` states
  `License: GPL v3` and links the exact source commit
  (`https://github.com/FFmpeg/FFmpeg/commit/946fcce07b`).
- Build configuration confirms it: `--enable-gpl --enable-version3`
  with x264, x265 (essentials build; no nonfree libraries).

## What this means for DRES

- DRES is MIT licensed and invokes ffmpeg/ffprobe only as external
  processes (via Jaffree), so shipping them is "mere aggregation" —
  the GPL does not reach DRES's own code.
- Redistributing GPL binaries with a DRES release still requires
  passing on the license text and source access (or a written offer).
  gyan makes that easy (LICENSE + commit in the archive); the Riedl
  archives require us to carry the attribution ourselves.
- Shipping the **linux-amd64** Riedl build in a DRES release is a real
  legal risk because of the DeckLink-SDK-derived `--enable-nonfree`
  binary. Options if we want to stay clean: ask Martin Riedl for a
  build without decklink (`-SKIP_DECKLINK` exists in the build script),
  use a different Linux source (e.g. johnvansickle or BtbN, both plain
  GPL builds), or accept and document the restriction.

## Practical suggestion

Add a `THIRD-PARTY-NOTICES` (or `doc/ffmpeg-notices.md`) file to the
distribution listing ffmpeg/ffprobe provenance (source URL, version,
sha256, license per platform) and link it from the README. This is also
the natural place for the macos/linux vs windows license differences.
