#!/usr/bin/env bash
# libvkshim.so próprio (troca de driver: Turnip/RADV etc.) + ganchos da libadrenotools, em build/lib.
#   scripts/build-android-vkshim.sh        (depois do build-android-runtime.sh: as funções vêm do libvitart.so)
# Nome diferente de libvulkan.so de propósito: no Android novo libvulkan.so é biblioteca pública e o loader
# ignora a do APK (o jogo ia direto no driver do sistema, sem conseguir trocar de driver).
# libadrenotools (BSD-2, Billy Laws), a cópia que o DroidDeck usa (third_party/adrenotools); ADRENOTOOLS=<pasta> troca.
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"
NDK="${ANDROID_NDK:-$LOCALAPPDATA/Android/Sdk/ndk/29.0.14206865}"
TC="$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin"
[ -x "$TC/clang.exe" ] || TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
T=--target=aarch64-linux-android28
AT="${ADRENOTOOLS:-third_party/adrenotools}"
[ -f "$AT/src/driver.cpp" ] || { echo "libadrenotools não encontrada em $AT"; exit 1; }
[ -f build/lib/libvitart.so ] || { echo "falta build/lib/libvitart.so (rode o build-android-runtime.sh)"; exit 1; }
O=build/vkshim; mkdir -p "$O" build/lib
LD="-Wl,-z,max-page-size=16384 -Wl,--no-undefined"
CXXF="$T -std=c++20 -O2 -fPIC -w -I$AT/include -I$AT -I$AT/lib/linkernsbypass"

# 1. as funções: as que o libvitart.so importa + as de entrada do loader
{ "$TC/llvm-nm" -D --undefined-only build/lib/libvitart.so | grep -o '\bvk[A-Za-z0-9]*' ;
  printf '%s\n' vkGetInstanceProcAddr vkGetDeviceProcAddr vkEnumerateInstanceExtensionProperties \
    vkEnumerateInstanceLayerProperties vkEnumerateInstanceVersion vkEnumerateDeviceExtensionProperties \
    vkGetPhysicalDeviceFeatures2 vkGetPhysicalDeviceProperties2; } | sort -u > "$O/nomes.txt"
N=$(wc -l < "$O/nomes.txt")
{ echo "// gerado por scripts/build-android-vkshim.sh"; echo "enum { kVkshimCount = $N };"
  echo "static const char *const kVkshimNames[kVkshimCount] = {"; sed 's/.*/    "&",/' "$O/nomes.txt"; echo "};"; } > "$O/vkshim_names.h"
{ echo "// gerado: cada função salta pela tabela g_vkshim (x16 é o registrador de rascunho do salto, livre aqui)"
  echo ".text"; i=0
  while read -r f; do
    echo ".globl $f"; echo ".type $f,%function"; echo ".p2align 4"
    echo "$f:"; echo "    adrp x16, g_vkshim+$((i*8))"; echo "    ldr x16, [x16, :lo12:g_vkshim+$((i*8))]"; echo "    br x16"
    echo ".size $f, .-$f"; i=$((i+1))
  done < "$O/nomes.txt"; } > "$O/vkshim_stubs.S"
{ echo "{ global:"; sed 's/.*/    &;/' "$O/nomes.txt"; echo "    vitart_vk_driver_status;"; echo "  local: *; };"; } > "$O/vkshim.map"

# 2. libadrenotools e o contorno de namespace (estáticos, dentro do shim e do hook_impl)
for f in "$AT"/src/driver.cpp "$AT"/src/bcenabler.cpp "$AT"/lib/linkernsbypass/android_linker_ns.cpp "$AT"/lib/linkernsbypass/elf_soname_patcher.cpp; do
  "$TC/clang++" $CXXF -c "$f" -o "$O/$(basename "${f%.cpp}").o"
done
NS="$O/android_linker_ns.o $O/elf_soname_patcher.o"

# 3. ganchos (vão para o nativeLibraryDir: a libadrenotools os carrega de lá)
"$TC/clang++" $CXXF -fvisibility=hidden -shared -static-libstdc++ -o build/lib/libhook_impl.so "$AT/src/hook/hook_impl.cpp" $NS \
  $LD -Wl,-soname,libhook_impl.so -llog -landroid -ldl
for h in main_hook file_redirect_hook gsl_alloc_hook; do
  "$TC/clang" $T -O2 -fPIC -w -I"$AT/include" -shared -o "build/lib/lib$h.so" "$AT/src/hook/$h.c" \
    $LD -Wl,-z,global -Wl,-soname,lib$h.so -Lbuild/lib -lhook_impl
done

# 4. o shim
"$TC/clang" $T -O2 -fPIC -w -I"$AT/include" -I"$O" -c runtime/vkshim/vkshim.c -o "$O/vkshim.o"
"$TC/clang" $T -c "$O/vkshim_stubs.S" -o "$O/vkshim_stubs.o"
"$TC/clang++" $T -shared -fPIC -static-libstdc++ -o build/lib/libvkshim.so "$O/vkshim.o" "$O/vkshim_stubs.o" \
  "$O/driver.o" "$O/bcenabler.o" $NS $LD -Wl,-soname,libvkshim.so -Wl,--version-script="$O/vkshim.map" -landroid -llog -ldl
for l in libvkshim libhook_impl libmain_hook libfile_redirect_hook libgsl_alloc_hook; do "$TC/llvm-strip" --strip-unneeded build/lib/$l.so; done
echo "pronto: build/lib/libvkshim.so ($N funções) + ganchos"
