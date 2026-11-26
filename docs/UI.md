# UI/UX launcher — Gaya PS5/Console Launcher

Konsep UI mengikuti referensi **PS5 / Console Launcher**: layar penuh lanskap, hero image dinamis, kartu ikon horizontal di kiri atas, judul/logo di tengah-kiri, tombol pill di bawah, navigasi gamepad di pojok kanan bawah.

## Struktur berlapis (dari belakang ke depan)

| Layer | Komponen | Deskripsi |
|---|---|---|
| **L1** | **Hero Background** | ImageView full-screen (`ContentScale.Crop`), gradient overlay gelap di kiri/bawah, canvas garis gelombang transparan (opsional). |
| **L2a** | **Top Bar Kiri** | `HorizontalScrollView`/`ListView` kartu ikon bulat (sudut 16dp), aktif = border putih tebal + scale 1.1. |
| **L2b** | **Top Bar Kanan** | Row ikon bulat (diameter 40–48dp), latar putih transparan (`alpha 0.15`), garis tepi tipis. |
| **L3** | **Hero Info & Action** | Tengah-kiri: logo game (PNG transparan) + subjudul kecil. Action: tombol pill Play (putih solid), tombol pill sekunder (frosted glass, border tipis putih). |
| **L4** | **Controller Hints** | Pojok kanan bawah: ikon lingkaran (X) Play dan (O) Back. |

## File terkait
- `launcher/app/src/main/res/layout/activity_main.xml` — layout layered (FrameLayout root).
- `launcher/app/src/main/res/drawable/hero_overlay.xml` — gradient overlay gelap.
- `launcher/app/src/main/res/drawable/icon_card_bg.xml` — rounded card untuk kartu ikon.
- `launcher/app/src/main/res/drawable/button_primary.xml` — tombol pill putih solid.
- `launcher/app/src/main/res/drawable/button_secondary.xml` — tombol pill transparan (frosted glass).
- `launcher/app/src/main/res/drawable/controller_icon_bg.xml` — lingkaran transparan untuk indikator.
- `launcher/app/src/main/res/animator/fade_in.xml` — animasi fade masuk.
- `launcher/app/src/main/res/animator/fade_out.xml` — animasi fade keluar.
- `launcher/app/src/main/res/values/styles.xml` — theme gelap + aksen.
- `launcher/app/src/main/res/values/strings.xml` — teks UI.

## State machine (tetap)

| State | Gamepad | `libbraid.so` | Pesan |
|---|---|---|---|
| `BELUM SIAP` | apa saja | tidak ada | `Status: BELUM SIAP — tambahkan libbraid.so.` |
| `LIBRARY SIAP` | tidak ada | termuat | `Status: LIBRARY SIAP — gamepad belum terhubung.` |
| `IMPORT` | ada | apa saja | `Status: IMPORT — pilih file game.` |
| `SIAP` | ada | termuat | `Status: SIAP — gamepad dan library tersedia.` |

## Import Game (lokal saja)

Tombol **Import Game** membuka file picker sistem (`ACTION_OPEN_DOCUMENT`). File hanya **dibaca di perangkat** untuk metadata:

- nama file
- ukuran (byte)
- SHA-256
- 16 byte pertama (hex)

Hasil tampil di kartu **IMPORT · FILE GAME** (muncul setelah pilih). **Tidak ada upload, tidak ada penyimpanan ke repo.** Pengingat hukum selalu ditampilkan.

Implementasi: `MainActivity.inspectUri()` — baca via `ContentResolver`, hash via `MessageDigest`.

## Animasi
- **Fade-in/out** saat ganti state (`res/animator/fade_in.xml`, `res/animator/fade_out.xml`).
- **Scale kartu** saat pilih ikon (`res/animator/scale_card.xml`).
- **Slide horizontal** untuk ganti hero image (`res/animator/slide_hero.xml`).

## Kenapa belum bisa langsung main?
Karena belum ada `libbraid.so`, renderer, dan runtime HLE. Launcher ini **diagnostik + titik integrasi**. Lihat [INTEGRATION.md](INTEGRATION.md).
