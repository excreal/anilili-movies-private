# Developer announcements

This ports Anilili's announcement source, signed-event verification, WorkManager scheduler,
notification channels, deduplication and notification-tap handling. Video Downloader uses a
separate project key and the `video-downloader-announcement` Nostr addressable-event tag.

Public key: `bb8a9ce60cacac6ee3bfefdc272df9fbc21a8010e2eb61b96aa3558ab97e71b5`.
The private publishing key is in `.signing/announcement-key.properties`, mode 0600, ignored by
Git and excluded from APK inputs. Preserve it with the release signing backups; changing it
requires distributing an app with the new public key.

The app first reads the signed JSON mirror at `http://20.120.96.17/notices/announcement.json`,
then falls back to the original four public relays. The public mirror payload must pass the same
pinned-key signature checks; an expired mirror falls through to relays. The player port now follows Anilili’s mobile cleartext network policy to support its local Cast/external-player HTTP proxy. The HTTP notice mirror still requires the original pinned-key signature checks.
Anilili's mirror address is not reused. The publisher atomically updates this mirror whenever an
announcement is broadcast from this server; dry-runs and update-manifest events do not change it. Background checks run approximately hourly subject to
Android scheduling, with foreground checks throttled to once per 15 minutes. This is polling,
not instant Firebase push. Android notification permission and the Announcements setting must
both be enabled. Routine and urgent channels can be controlled separately in Android settings.

Messages are checked against the pinned project key, NIP-01 event hash, BIP-340 signature,
event kind/tag, expiry and inclusive app-version bounds. Only HTTPS action links are accepted.
IDs are remembered only after a notification is posted. Reusing an ID does not notify again.
Messages open only after the user taps a notification and never interrupt video playback.

## Publishing

The source-derived publisher is `scripts/developer_notices.py`. Install its dependencies
with `pip install -r scripts/requirements-notices.txt` into a Python virtual environment. The session's environment
is `/tmp/notice-publisher-venv` (recreate it when necessary).

Preview and verify a signed message without publishing:

```sh
/tmp/notice-publisher-venv/bin/python scripts/developer_notices.py announce \
  --id example-outage-001 --title 'Source temporarily unavailable' \
  --message 'We are working on restoring this source.' --expires-in-days 2 --dry-run
```

Publishing uses the same command without `--dry-run`, only when explicitly authorized.
Use `announce-fetch` to inspect relay state. Use `announce-clear` only when explicitly
asked to retract an existing announcement. No message was published during this change.
The script retains Anilili's update-manifest commands for source provenance; this app does
not currently consume update manifests and those commands do not update the installed app.

Notification checks show fetch, verification and permission/channel outcomes in Settings. A blocked
channel is not recorded as delivered. Failed source checks retry through WorkManager and do not
consume the normal successful-check throttle. Explicit checks bypass that throttle.
