# Braid (Xbox 360): checklist impor XDK untuk HLE

Daftar ini adalah **perkiraan area** yang biasanya muncul saat recompile XBLA/Xbox 360 ke native. Status sebenarnya **harus diverifikasi dari biner yang Anda miliki secara sah** dengan tool analisa pilihan Anda. Repo ini tidak menyediakan NID, dump, atau file game.

## Checklist area

| Area | Fungsi khas | Prioritas | Catatan untuk Braid |
|---|---|---|---|
| Startup/runtime | init, title ID, heap, thread utama | Tinggi | Harus jalan sebelum apa pun terlihat. |
| Memory/allocator | alloc/free, alignment, heap XDK | Tinggi | Alignmen PowerPC penting untuk struktur game. |
| File system | create/read/seek/find, path relatif | Tinggi | Asset level, tekstur, audio, save. |
| Input | `XInputGetState`, `XInputSetState`, guide | Tinggi | Platformer presisi: gamepad lebih penting dari touch. |
| Audio | XAudio2, voice, buffer, volume | Tinggi | Trek Magnatune diputar normal **dan terbalik**. |
| Render | D3D9 device, tekstur, sprite, blend, state | Tinggi | Engine 2D custom; efek blur/partikel perlu diperhatikan. |
| Waktu/thread | tick count, sleep, sinkronisasi | Tinggi | Rewind menyimpan riwayat state; determinisme krusial. |
| Save/profile | user, storage, progress, puzzle piece | Menengah-tinggi | Progress world dan collectible harus persisten. |
| Font/teks | font, draw text, ukuran string | Menengah | Narasi tiap world ditampilkan sebagai teks. |
| Content/update | title update, package content | Menengah | XBLA sering punya TU; versi berbeda bisa beda import. |
| Movie/video | playback intro/video | Menengah-rendah | Bisa distub dulu bila tidak blocking. |
| Achievements/network | achievement, XUID, leaderboard | Rendah | Umumnya aman di-stub untuk pemakaian lokal. |

## Urutan implementasi yang disarankan

1. Area yang bikin crash saat startup: runtime, memory, file, thread.
2. Input + waktu, supaya game bisa merespons.
3. Render null: pastikan logika berjalan tanpa grafis.
4. Audio + render nyata.
5. Save/profile/content.
6. Fitur opsional: achievement, video, jaringan.

## Cara pakai checklist

- Simpan hasil analisa Anda di folder `analysis/` (sudah di-ignore git).
- Tandai tiap baris: `belum dicek`, `ter-unresolved`, `di-stub`, atau `terimplementasi`.
- Jangan commit dump, NID hasil ekstraksi, atau potongan kode game ke repo publik ini.

Kembali ke [catatan target Braid](braid.md) dan [LEGAL.md](../LEGAL.md).
