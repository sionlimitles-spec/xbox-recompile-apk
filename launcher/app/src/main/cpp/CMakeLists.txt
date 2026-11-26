cmake_minimum_required(VERSION 3.22.1)

project(braid_stub LANGUAGES CXX)

add_library(braid_stub SHARED braid_jni.cpp)

target_compile_features(braid_stub PRIVATE cxx_std_17)
target_link_libraries(braid_stub PRIVATE log)

# Catatan:
# - Target ini menghasilkan libbraid_stub.so (bukan libbraid.so) supaya
#   libbraid.so hasil recompile pengguna bisa ditaruh di jniLibs/ tanpa bentrok.
# - Ganti isi braid_jni.cpp dengan kode nyata, atau tambahkan sumber hasil
#   recompile ke target ini, saat runtime sudah tersedia.
