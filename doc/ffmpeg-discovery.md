# FFmpeg discovery and setup

DRES treats FFmpeg as an optional external runtime dependency. The application can start without it; video preview generation and video metadata scans report a clear error until FFmpeg is available.

## Discovery order

DRES searches for a directory containing both executable `ffmpeg` and `ffprobe` binaries in this order:

1. `cache.ffmpegPath` in the configuration (a binary path or its containing directory is accepted).
2. The distribution's adjacent `ffmpeg` directory.
3. The development build's `ext/ffmpeg` directory.
4. `ext/ffmpeg` and `ffmpeg` relative to the current working directory.
5. `<data root>/ffmpeg`.
6. Directories listed in `PATH`.

If FFmpeg is missing, DRES logs a warning and continues to serve non-video features. The CLI prints `setup ffmpeg` as the installation option. On macOS x86-64/ARM64 and Windows x86-64, that command downloads the pinned FFmpeg 9.0.2 build into `<data root>/ffmpeg` by default. Linux downloads are deliberately disabled: the audited Linux build enabled nonfree DeckLink components, and no replacement artifact has yet been vetted. On Linux, install a system FFmpeg package or provide a reviewed build yourself. Use `setup ffmpeg --target <directory>` to choose another destination. A custom target is not searched automatically; set `cache.ffmpegPath` to that directory in the DRES configuration to use it.

Downloads are checked against the platform-specific SHA-256 values pinned in `backend/src/main/kotlin/dev/dres/utilities/FFmpegDownloader.kt`. The archives are extracted to the target directory, and Unix executable permissions are set. Supported automatic download targets are macOS x86-64/ARM64 and Windows x86-64. Linux and other architectures require a manually installed FFmpeg distribution discoverable through configuration or `PATH`.

## License and provenance

DRES does not bundle FFmpeg binaries in its distribution. When a user invokes `setup ffmpeg`, the program downloads the selected third-party build directly from its publisher. The sources and license caveats are documented in [ffmpeg-licensing.md](ffmpeg-licensing.md). In particular, the previously audited Linux build's upstream configuration includes `--enable-nonfree`, so automatic Linux downloads are disabled pending review of a suitable alternative.
