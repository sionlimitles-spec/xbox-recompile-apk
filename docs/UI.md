# UI/UX launcher

Tampilan launcher sengaja dibuat **dark console-style generik** (biru gelap + aksen biru + kartu). Ini bukan replika PlayStation/PS4 dan tidak memakai logo atau asset Sony.

## Layout

```text
XBOX RECOMPILE            ← header + garis aksen biru
Launcher · diagnostik lokal

┌ GAMEPAD ─────────────┐
│ - Xbox Controller …  │
└──────────────────────┘
┌ NATIVE LIBRARY ──────┐
│ libbraid.so status   │
└──────────────────────┘
┌ TARGET ──────────────┐
│ Braid · Xbox 360     │
└──────────────────────┘
Status: ...
```

File terkait:
- `launcher/app/src/main/res/layout/activity_main.xml`
- `launcher/app/src/main/res/values/styles.xml`
- `launcher/app/src/main/res/drawable/card_bg.xml`
- `launcher/app/src/main/res/drawable/accent_bar.xml`
- `launcher/app/src/main/res/values/strings.xml`

## State machine

| State | Gamepad | `libbraid.so` | Pesan |
|---|---|---|---|
| `BELUM SIAP` | apa saja | tidak ada | `Status: BELUM SIAP — tambahkan libbraid.so.` |
| `LIBRARY SIAP` | tidak ada | termuat | `Status: LIBRARY SIAP — gamepad belum terhubung.` |
| `SIAP` | ada | termuat | `Status: SIAP — gamepad dan library tersedia.` |

State dihitung ulang setiap:
- Activity dibuat/dilanjutkan,
- gamepad ditambahkan, dihapus, atau berubah.

## Kenapa tidak langsung tampil seperti game?

Karena belum ada:
- renderer/SurfaceView,
- runtime HLE,
- `libbraid.so` hasil recompile.

Launcher ini hanya **diagnostik + titik integrasi**. Lihat [INTEGRATION.md](INTEGRATION.md) untuk sambungan native library dan [targets/braid.md](targets/braid.md) untuk target game.
