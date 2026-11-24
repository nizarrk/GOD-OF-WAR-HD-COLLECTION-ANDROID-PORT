#!/usr/bin/env bash
# O runtime (vitart + o código do GoW) para Android arm64: build/lib/libvitart.so, que a GameActivity carrega.
#   scripts/build-android-runtime.sh            (OPT=-O2, JOBS=4, ANDROID_NDK como no build-android-modules.sh)
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"
NDK="${ANDROID_NDK:-$LOCALAPPDATA/Android/Sdk/ndk/29.0.14206865}"
TC="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$TC/clang.exe" ] || TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
T=--target=aarch64-linux-android28
OBJ=build/rt_obj; mkdir -p "$OBJ" build/lib
# otimizador de SPIR-V (SPIRV-Tools do próprio NDK), para otimizar os shaders no celular ao montar o pacote
SPVT="$NDK/sources/third_party/shaderc/third_party/spirv-tools"
SPVL=build/spvtools/obj/local/arm64-v8a
if [ ! -f "$SPVL/libSPIRV-Tools-opt.a" ]; then
  mkdir -p build/spvtools/jni
  cat > build/spvtools/jni/Android.mk <<'MK'
THIRD:=$(NDK_ROOT)/sources/third_party/shaderc/third_party
SPVTOOLS_LOCAL_PATH:=$(THIRD)/spirv-tools
SPVHEADERS_LOCAL_PATH:=$(THIRD)/spirv-tools/external/spirv-headers
include $(SPVTOOLS_LOCAL_PATH)/Android.mk
MK
  cat > build/spvtools/jni/Application.mk <<'MK'
APP_ABI := arm64-v8a
APP_PLATFORM := android-29
APP_STL := c++_static
APP_MODULES := SPIRV-Tools SPIRV-Tools-opt
APP_CPPFLAGS := -O2 -fPIC -std=c++17 -ffunction-sections -fdata-sections
MK
  (cd build/spvtools && "$NDK/ndk-build.cmd" NDK_PROJECT_PATH=. -j16 >/dev/null)
fi
INC="-Iruntime/arm -Iruntime/vitart -Ithird_party/libatrac9/src -Ithird_party/stb -Ithird_party/font8x8 -I$SPVT/include"
CXXF="$T -std=c++20 ${OPT:--O2} -fPIC -w -include runtime/vitart/plat.h -ffunction-sections -fdata-sections $INC"
export TC CXXF T INC OBJ

# o que é só do Windows fica de fora; o Android tem a sua versão em runtime/vitart/android
SRC=()
for f in runtime/vitart/*.cpp runtime/vitart/android/*.cpp games/gow/*.cpp; do
  case "$f" in */gxm_d3d11.cpp|*/gxm_window.cpp|*/host_input.cpp|runtime/vitart/trophy_banner.cpp|runtime/vitart/video_hd.cpp) continue;; esac
  SRC+=("$f")
done
# cabeçalho mudou: tudo de novo (o teste de data abaixo só olha o .cpp)
HDR="$(ls -t runtime/vitart/*.h runtime/vitart/android/*.h runtime/arm/*.h | head -1)"
for o in "$OBJ"/*.o; do [ -e "$o" ] && [ "$HDR" -nt "$o" ] && rm -f "$o"; done
printf '%s\n' "${SRC[@]}" | xargs -P "${JOBS:-4}" -I{} sh -c \
  'o="$OBJ/$(basename {} .cpp).o"; [ "$o" -nt {} ] && [ -z "${REFAZ:-}" ] || "$TC/clang++" $CXXF -c {} -o "$o" || { echo "FALHOU {}"; exit 255; }'
for c in third_party/libatrac9/src/*.c; do
  o="$OBJ/at9_$(basename "${c%.c}").o"
  [ "$o" -nt "$c" ] || "$TC/clang" $T -std=c99 -O2 -fPIC -w -c "$c" -o "$o"
done
# o Vulkan vem do shim do APK (libvkshim.so, gerado pelo build-android-vkshim.sh): libvulkan.so seria a do
# sistema, que no Android novo ignora a do APK e não carrega o driver escolhido
"$TC/clang++" $T -shared -fPIC -static-libstdc++ -o build/lib/libvitart.so "$OBJ"/*.o "$SPVL/libSPIRV-Tools-opt.a" "$SPVL/libSPIRV-Tools.a" \
  -Wl,--gc-sections -Wl,--no-undefined -Wl,-z,max-page-size=16384 -Wl,-soname,libvitart.so \
  -Lbuild/lib -l:libvkshim.so -laaudio -landroid -llog -ldl -lm
"$TC/llvm-strip" --strip-unneeded build/lib/libvitart.so
echo "pronto: build/lib/libvitart.so ($(du -h build/lib/libvitart.so | cut -f1))"
