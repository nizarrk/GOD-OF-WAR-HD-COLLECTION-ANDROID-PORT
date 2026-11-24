<div align="center">

<img src="docs/icone.png" width="160" alt="God of War Collection - Android">

# God of War Collection · Android

**PS Vita → Android arm64 · recompilação estática / static recompilation · Vulkan**

![Android](https://img.shields.io/badge/Android-9%2B%20(API%2028)-3DDC84?logo=android&logoColor=white)
![ABI](https://img.shields.io/badge/ABI-arm64--v8a-0A84FF)
![Vulkan](https://img.shields.io/badge/Vulkan-1.1-AC162C?logo=vulkan&logoColor=white)
![Status](https://img.shields.io/badge/status-demonstração%20%2F%20demo-orange)
![Suporte](https://img.shields.io/badge/suporte%20%2F%20support-nenhum%20%2F%20none-lightgrey)

[🇧🇷 Português](#-português) · [🇺🇸 English](#-english)

</div>

> [!NOTE]
> **🇺🇸 This is a fork maintained by [Nizar (@nizarrk)](https://github.com/nizarrk).** The original port is by **Luis Santos "DESANTA"** ([luisxl15](https://github.com/luisxl15)) and all the credit for it stays with him — this fork only adds the fixes listed in [Changes in this fork](#-changes-in-this-fork). It is published as is, without support.
>
> **🇧🇷 Este é um fork mantido por [Nizar (@nizarrk)](https://github.com/nizarrk).** O port original é de **Luis Santos "DESANTA"** ([luisxl15](https://github.com/luisxl15)) e todo o crédito continua com ele — este fork só traz as correções listadas em [Changes in this fork](#-changes-in-this-fork). É publicado como está, sem suporte.

> [!WARNING]
> **🇧🇷 PROJETO DE DEMONSTRAÇÃO. NÃO HÁ SUPORTE.** Este repositório mostra uma técnica (recompilação estática de um jogo do PS Vita para Android). Ele é publicado como está: não há suporte, garantia, prazo nem atendimento de issues ou pedidos. Pode não funcionar no seu aparelho.
>
> **🇺🇸 DEMONSTRATION PROJECT. NO SUPPORT WILL BE PROVIDED.** This repository showcases a technique (static recompilation of a PS Vita game to Android). It is published as is: no support, no warranty, no schedule, and no handling of issues or requests. It may not work on your device.

> [!CAUTION]
> **🇧🇷** O único lugar oficial é este repositório. APKs de outros sites, do APKVISION ou de canais do YouTube com links encurtados podem conter vírus.
>
> **🇺🇸** This repository is the only official source. APKs from other sites, APKVISION or YouTube channels with shortened links may contain malware.

---

## 💛 Apoie o projeto

O projeto é feito no tempo livre e é de graça. Se ele te ajudou e você quiser apoiar para mantê-lo vivo, qualquer
valor ajuda:

<div align="center">

[![LivePix](https://img.shields.io/badge/LivePix-doe%20via%20Pix-00c2a8?style=for-the-badge)](https://livepix.gg/luisxl15) [![PayPal](https://img.shields.io/badge/PayPal-doe%20com%20cart%C3%A3o%20ou%20PayPal-003087?style=for-the-badge&logo=paypal&logoColor=white)](https://www.paypal.com/donate/?business=7MWMRNN92Z464&no_recurring=0&item_name=God+of+War+HD+Collection)

<a href="https://livepix.gg/luisxl15"><img src="docs/img/livepix-qr.png" width="180" alt="QR Code do LivePix: livepix.gg/luisxl15"></a>

**[livepix.gg/luisxl15](https://livepix.gg/luisxl15)**: Pix, e a sua mensagem aparece na live.

**[PayPal](https://www.paypal.com/donate/?business=7MWMRNN92Z464&no_recurring=0&item_name=God+of+War+HD+Collection)**: cartão ou saldo do PayPal, de qualquer país.

</div>

---

## 📸 Capturas de tela · Screenshots

<div align="center">

<img src="docs/capturas/lancador.jpg" width="100%" alt="Lançador / Launcher">

| | |
|:---:|:---:|
| <img src="docs/capturas/jogo.jpg" alt="Jogo / Gameplay"> | <img src="docs/capturas/trofeus.jpg" alt="Troféus / Trophies"> |
| **Jogando com os controles na tela** · *Playing with touch controls* | **Troféus** · *Trophies* |
| <img src="docs/capturas/controles_cristal.jpg" alt="Controles Cristal"> | <img src="docs/capturas/controles_minimal.jpg" alt="Controles Minimalista"> |
| **Controles Cristal (estilo DualSense)** · *Crystal controls (DualSense-like)* | **Controles Minimalista** · *Minimal controls* |
| <img src="docs/capturas/tela.jpg" alt="Tela / Screen"> | <img src="docs/capturas/abertura.jpg" alt="Abertura / Boot"> |
| **Proporção da tela e texturas HD** · *Aspect ratio and HD textures* | **Abertura com o PS Vita em 3D** · *3D PS Vita boot animation* |

</div>

---

## 🇧🇷 Português

### O que é

O God of War Collection do PS Vita (God of War e God of War II) rodando **nativo** no Android: o código ARM do jogo foi recompilado para C e roda sobre o runtime **vitart**, com a GPU do Vita (GXM) traduzida para **Vulkan**. Não há emulação. É o mesmo projeto do porte de PC, levado para celulares arm64.

**Nada do jogo está neste repositório nem no APK.** Você precisa do seu próprio jogo:

| Arquivo | O que é |
|---|---|
| `gowHD_PSV.pkg` | o PKG **americano** (PCSA00126, 3,3 GB) |
| `work.bin` | a licença (512 bytes) da sua conta |

### Recursos

| | |
|---|---|
| 🎮 **Lançador** | visual do PS Vita com ondas, animação de abertura em 3D, troféus, idiomas e opções |
| 📦 **Instalação** | o lançador acha o PKG e o `work.bin`, confere os dois e tira o jogo do PKG no próprio celular; os dados do porte vêm cifrados das releases e só abrem com a sua licença |
| 🔄 **Atualização automática** | baixa a versão nova das releases com barra de progresso e instala sozinha (Android 12 ou mais novo) |
| 🕹️ **Controles na tela** | três visuais: Clássico, Cristal (estilo DualSense) e Minimalista; dá para arrastar, mudar tamanho e opacidade; controles físicos funcionam |
| 🏆 **Troféus** | notificação durante o jogo e grade completa no lançador |
| 🌐 **Idiomas** | segue o idioma do Android (português, inglês, espanhol, francês, alemão e italiano) |
| ⚡ **GPUs** | Adreno, Mali, Xclipse e PowerVR; Turnip embutido para Adreno; limites de textura próprios para Mali |

### Como instalar

1. Baixe o APK em **[Releases](../../releases)** e instale.
2. Abra o app e permita o acesso aos arquivos.
3. Copie o `gowHD_PSV.pkg` e o `work.bin` para `Download` ou `Documents`. Se o lançador não achar, toque na linha e escolha o arquivo.
4. Toque em **Instalar o jogo**. As texturas HD são opcionais (+1,7 GB).

Já tem a pasta do port copiada do PC? Use **"Já tenho a pasta copiada do PC"** no painel de instalação.

### Como compilar

Precisa do Android NDK (clang, API 29), do JDK 17 e do Git Bash ou de um shell POSIX.

```bash
scripts/build-android-runtime.sh   # libvitart.so
scripts/build-android-vkshim.sh    # libvulkan.so (camada sobre o driver do sistema)
scripts/build-android-pfs.sh       # libpfs_extrair.so (tira o jogo do PKG no celular)
scripts/build-android-modules.sh   # módulos do jogo (gerados a partir do SEU jogo, fora deste repositório)
cd android && ./gradlew assembleRelease
```

Os módulos do jogo saem do seu próprio PKG pelo recompilador do porte de PC; eles não são distribuídos aqui.

### Estrutura

```
android/          app (lançador, jogo, controles, instalador, atualização)
runtime/vitart/   runtime: HLE do Vita, GXM → Vulkan, áudio, troféus
runtime/vkshim/   camada Vulkan
runtime/arm/      suporte ao código ARM recompilado
games/gow/        perfil e opções do God of War
third_party/      bibliotecas usadas (cada uma com a sua licença)
tools/            traduções, extrator do PKG, arte gerada por código
scripts/          compilação para Android
```

---

## 🇺🇸 English

### 💛 Support the project

This project is made in my free time and is free to use. If it helped you and you'd like to help keep it alive, any
amount helps:

<div align="center">

[![PayPal](https://img.shields.io/badge/PayPal-donate%20with%20card%20or%20PayPal-003087?style=for-the-badge&logo=paypal&logoColor=white)](https://www.paypal.com/donate/?business=7MWMRNN92Z464&no_recurring=0&item_name=God+of+War+HD+Collection) [![LivePix](https://img.shields.io/badge/LivePix-Pix%20(Brazil)-00c2a8?style=for-the-badge)](https://livepix.gg/luisxl15)

<a href="https://livepix.gg/luisxl15"><img src="docs/img/livepix-qr.png" width="180" alt="LivePix QR code: livepix.gg/luisxl15"></a>

**[PayPal](https://www.paypal.com/donate/?business=7MWMRNN92Z464&no_recurring=0&item_name=God+of+War+HD+Collection)**: card or PayPal balance, from any country.

**[livepix.gg/luisxl15](https://livepix.gg/luisxl15)**: Brazilian Pix, and your message shows up on the live stream.

</div>

---

### What it is

PS Vita's God of War Collection (God of War and God of War II) running **natively** on Android: the game's ARM code was recompiled to C and runs on the **vitart** runtime, with the Vita GPU (GXM) translated to **Vulkan**. There is no emulation. It is the same project as the PC port, brought to arm64 phones.

**Nothing from the game is in this repository or in the APK.** You need your own copy:

| File | What it is |
|---|---|
| `gowHD_PSV.pkg` | the **US** PKG (PCSA00126, 3.3 GB) |
| `work.bin` | your account's license (512 bytes) |

### Features

| | |
|---|---|
| 🎮 **Launcher** | PS Vita look with waves, 3D boot animation, trophies, languages and options |
| 📦 **Install** | the launcher finds the PKG and `work.bin`, checks both and extracts the game from the PKG on the phone; the port's data comes encrypted from the releases and only opens with your license |
| 🔄 **Auto update** | downloads the new version from the releases with a progress bar and installs it by itself (Android 12 or newer) |
| 🕹️ **Touch controls** | three styles: Classic, Crystal (DualSense-like) and Minimal; drag, resize and change opacity; physical controllers work |
| 🏆 **Trophies** | in-game notification and a full grid in the launcher |
| 🌐 **Languages** | follows the Android language (Portuguese, English, Spanish, French, German and Italian) |
| ⚡ **GPUs** | Adreno, Mali, Xclipse and PowerVR; bundled Turnip for Adreno; Mali-specific texture limits |

### How to install

1. Download the APK from **[Releases](../../releases)** and install it.
2. Open the app and allow file access.
3. Copy `gowHD_PSV.pkg` and `work.bin` to `Download` or `Documents`. If the launcher does not find them, tap the row and pick the file.
4. Tap **Install the game**. HD textures are optional (+1.7 GB).

Already have the port folder copied from the PC? Use **"I already have the folder copied from the PC"** in the install panel.

### How to build

Requires the Android NDK (clang, API 29), JDK 17 and Git Bash or a POSIX shell.

```bash
scripts/build-android-runtime.sh   # libvitart.so
scripts/build-android-vkshim.sh    # libvulkan.so (layer over the system driver)
scripts/build-android-pfs.sh       # libpfs_extrair.so (extracts the game from the PKG on the phone)
scripts/build-android-modules.sh   # game modules (generated from YOUR game, outside this repository)
cd android && ./gradlew assembleRelease
```

The game modules come from your own PKG through the PC port's recompiler; they are not distributed here.

### Layout

```
android/          app (launcher, game, controls, installer, updater)
runtime/vitart/   runtime: Vita HLE, GXM → Vulkan, audio, trophies
runtime/vkshim/   Vulkan layer
runtime/arm/      support for the recompiled ARM code
games/gow/        God of War profile and options
third_party/      libraries used (each with its own license)
tools/            translations, PKG extractor, code-generated art
scripts/          Android build
```

---

## 🔧 Changes in this fork

Fork of [luisxl15/GOD-OF-WAR-HD-COLLECTION-ANDROID-PORT](https://github.com/luisxl15/GOD-OF-WAR-HD-COLLECTION-ANDROID-PORT), maintained by [@nizarrk](https://github.com/nizarrk). The port itself, its credits and the donation links remain the original author's.

- **Diagnostic log**: a full report (device/SoC, GPU and Vulkan driver, driver selection, game files, the tail of the vitart log and recent logcat) is saved on every game launch and on an early exit, and can be opened from the launcher (magnifier shortcut). It is written to `logs/diagnostico.txt` in the game folder and to the app's internal storage.
- **Black screen on Android 17 / Samsung Xclipse (tested on a Galaxy A57, Exynos 1680 / Xclipse 550)** — three fixes:
  - the game window is translucent (`Tema.Jogo`): on Android 17 the opaque window was covering the Vulkan `SurfaceView`, so only the touch controls were visible;
  - the bundled Vulkan layer is now `libvkshim.so` and `libvitart.so` links against it: Android 17 treats `libvulkan.so` as a public library and ignores the APK's copy, so the choice of driver never ran; the shim also falls back to a direct `dlopen` of the driver when `libadrenotools`' linker bypass is unavailable;
  - the present path draws the frame with a render pass instead of `vkCmdBlitImage`, which returns black on the XGL driver (Xclipse 550).
- **Build scripts** updated for the shim rename (`scripts/build-android-vkshim.sh`, `scripts/build-android-runtime.sh`).
- The in-app updater reads releases from this fork (`Instalador.REPO_APP`); the encrypted game-data packages are still fetched from the original repository, which is the only place that has them.
- The GitHub shortcut points to this fork; the original credit stays in the opening animation and the "Support the project" links still go to the original author.
- **Note (game data, not code)**: on a game folder copied from the PC, the pt-BR translation can be baked into `data/mods/gow1_psp2_SCEA.psarc.vtpatch`; disabling that patch restores the game's original English texts while keeping the fix for the black screen.

---

<div align="center">

Port feito e mantido por / made and maintained by **Luis Santos "DESANTA"** · [github.com/luisxl15](https://github.com/luisxl15)

<sub>God of War é marca da Sony Interactive Entertainment. Este projeto não tem ligação com a Sony. · God of War is a trademark of Sony Interactive Entertainment. This project is not affiliated with Sony.</sub>

</div>
