# UI/UX Braid Android — gaya console launcher

Layar penuh lanskap, hero gelap, kartu ikon horizontal, judul di tengah-kiri, tombol pill, dan petunjuk gamepad. Sekarang **semua elemen yang terlihat seperti tombol benar-benar bisa diklik**.

## Struktur

Lapisan belakang bersifat dekoratif dan `clickable="false"`:

| Layer | Komponen | Catatan |
|---|---|---|
| L1 | `hero_bg` (ImageView) + `hero_overlay` (gradient) | Tidak pernah menangkap sentuhan |
| L2 | `ScrollView` (`fillViewport`) | Satu-satunya layer interaktif penuh; semua kontrol ada di dalamnya |

Di dalam `ScrollView`:

- **Top bar**: `HorizontalScrollView` berisi kartu **Koleksi** dan **Braid** (aktif), plus tiga `ImageButton` (info, diagnostik, refresh).
- **Spacer fleksibel** supaya konten hero tidak menempel di atas pada layar tinggi; tetap bisa di-scroll di layar pendek.
- **Hero**: judul `BRAID`, garis aksen, metadata platform, subtitle.
- **Aksi**: tombol pill **Play** (putih) dan **Import Game** (frosted).
- **Kartu import**: muncul setelah file dipilih, berisi metadata + progres.
- **Hint gamepad**: `A Play`, `B Keluar` — juga bisa diklik.
- **Status footer**: pesan keadaan launcher/runtime.

Kenapa `ScrollView`? Layout lama menumpuk banyak child `FrameLayout` penuh layar. Di layar pendek, konten bisa tertutup/terpotong. Sekarang tidak ada overlay yang menghalangi tombol, dan semua tombol tetap terjangkau.

## Elemen interaktif

| Elemen | Aksi | Feedback |
|---|---|---|
| Kartu Koleksi | dialog info target | dialog |
| Kartu Braid | pilih target Braid | Toast + status |
| Ikon ℹ | dialog "Tentang" | dialog |
| Ikon diagnostik | paket, versi, ABI, native, gamepad, import | dialog |
| Ikon refresh | pindai ulang gamepad | Toast + status |
| **Play** | jalankan JNI bila runtime nyata; kalau stub → dialog penjelasan | Toast/dialog/status |
| **Import Game** | buka file picker (SAF, fallback `GET_CONTENT`) | kartu import + progres |
| `A Play` | sama seperti tombol Play | status |
| `B Keluar` | `finish()` | keluar |
| Gamepad A / DPAD_CENTER / B | sama seperti di atas | status |

Setiap tombol memakai drawable `ripple` (`card_clickable`, `card_selected`, `pill_primary`, `pill_secondary`, `round_clickable`), jadi ada respons visual saat disentuh.

## State machine

| Keadaan | Native | Gamepad | Pesan |
|---|---|---|---|
| `BELUM SIAP` | tidak termuat | apa saja | `libbraid.so tidak termuat` |
| `STUB` | stub termuat | opsional | runtime game belum ditanamkan |
| `RUNTIME SIAP` | nyata | tidak ada | gamepad belum terhubung |
| `SIAP` | nyata | ada | runtime + gamepad tersedia |
| `IMPORT` | — | — | membaca file / selesai |

## Import Game (lokal saja)

Picker sistem (`ACTION_OPEN_DOCUMENT`, fallback `ACTION_GET_CONTENT`). File hanya dibaca di perangkat:

- nama, ukuran (human readable + byte),
- SHA-256 penuh,
- 16 byte pertama (hex),
- deteksi magic: `XEX`, `CON`/`STFS`/`PIRS`/`LIVE`, `XBE`, ZIP/RAR/7z, ELF,
- ekstensi + catatan format (misal XBE → bukan target repo ini),
- progres tiap 16 MB untuk file besar.

**Tidak ada upload, tidak ada penyalinan ke repo.** Pengingat hukum selalu ditampilkan di bawah laporan. Implementasi: `MainActivity.inspectUri()`.

Lihat [INTEGRATION.md](INTEGRATION.md) untuk kontrak native dan [BRAID-REQUIREMENTS.md](BRAID-REQUIREMENTS.md) untuk analisa target.

## Layar game (`GameActivity`)

Bila `libbraid.so` nyata terpasang, tombol **Play** membuka layar game:

- `SurfaceView` penuh layar sebagai target render native (`ANativeWindow`).
- Loop frame 60 fps yang memanggil `nativeFrame(nowMs)`.
- Input: `A/B/X/Y/LB/RB/DPAD/START` dari gamepad, sentuh layar → axis X/Y + tombol `TOUCH`.
- Audio native di-init saat resume dan dimatikan saat pause.
- `Back` untuk keluar; petunjuk ditampilkan 3 detik pertama.

Saat library masih stub, layar ini **tidak dibuka** — launcher memberi dialog penjelasan. Kontrak lengkap: [INTEGRATION.md](INTEGRATION.md).

## Kenapa belum bisa langsung main?

Karena belum ada hasil recompile, HLE, renderer, dan audio. Sisi Java sudah siap sebagai titik integrasi; yang kosong ada di sisi native.
