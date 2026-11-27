# Administrator system status

The server information dialog (opened from the DRES logo) can display actionable system notices. Administrators see these notices through `GET /api/v2/status/info`; other roles receive the usual public server fields only. The logo shows a warning icon when at least one status has warning or error severity.

## Adding a status check

Implement `SystemStatusProvider` in `backend/src/main/kotlin/dev/dres/api/rest/types/system/SystemStatusProvider.kt`. Its `statuses()` method should be fast, side-effect free, and return zero or more `SystemStatus` values with a stable code, severity (`INFO`, `WARNING`, or `ERROR`), and human-readable actionable message. Register the provider once with `SystemStatusRegistry.register(provider)` during subsystem setup, or add it to the registry's built-in providers. Checks are evaluated when an administrator requests system info; the frontend refreshes that data once per minute while an administrator is logged in. A provider that throws is logged and isolated, so it does not prevent other providers from reporting.

Do not put secrets or sensitive configuration values in status messages. Keep detailed diagnostics in server logs, and make the status message safe to show to administrators in the browser.

The video-preview renderer publishes the transient `FFMPEG_VIDEO_RENDER_FAILED` warning after a render fails while FFmpeg is installed. The status clears after the next successful video preview render. The administrator message avoids exposing media paths or process output. This uses the registry's `publish` and `clear` methods, keyed by status code, for runtime events that are not naturally represented by a health-check provider.
