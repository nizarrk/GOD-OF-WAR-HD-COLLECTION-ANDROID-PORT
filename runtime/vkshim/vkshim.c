// libvulkan.so próprio do APK: o libvitart.so resolve as funções Vulkan aqui (o linker do Android procura na pasta
// do app antes da do sistema), e cada uma é um salto pela tabela g_vkshim (vkshim_stubs.S, gerado pelo
// scripts/build-android-vkshim.sh). Na carga a tabela é preenchida com o loader do sistema ou, se a GameActivity
// escolheu um driver (Turnip etc.), com o loader aberto pela libadrenotools apontando para ele.
//
// Variáveis (a GameActivity define antes do System.loadLibrary):
//   VITART_VK_LIBDIR      nativeLibraryDir (onde ficam os ganchos libmain_hook.so e libhook_impl.so)
//   VITART_VK_DRIVER_DIR  pasta interna do driver (termina em /; não pode ser o /sdcard: o dlopen recusa)
//   VITART_VK_DRIVER      nome do .so do driver dentro dela (vazio = driver do sistema)
// Os parâmetros da libadrenotools são os do DroidDeck (waylandcomp/src/vk_loader.c): pasta temporária <driver>temp.
#include <android/log.h>
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include <adrenotools/driver.h>

#include "vkshim_names.h" // kVkshimNames[], kVkshimCount

__attribute__((visibility("hidden"))) void *g_vkshim[kVkshimCount];

static char g_status[256] = "driver do sistema";

#define LOG(...) __android_log_print(ANDROID_LOG_INFO, "vitart-vk", __VA_ARGS__)

// texto curto do que foi carregado (o lançador e o log do vitart mostram)
__attribute__((visibility("default"))) const char *vitart_vk_driver_status(void) { return g_status; }

static void *abrir(void) {
    const char *nome = getenv("VITART_VK_DRIVER"), *dir = getenv("VITART_VK_DRIVER_DIR");
    const char *libdir = getenv("VITART_VK_LIBDIR");
    if (nome && *nome && dir && *dir && libdir && *libdir) {
        char hooks[1024];
        strncpy(hooks, libdir, sizeof hooks - 2);
        hooks[sizeof hooks - 2] = 0;
        if (hooks[strlen(hooks) - 1] != '/') strcat(hooks, "/");
        char tmp[1100];
        snprintf(tmp, sizeof tmp, "%stemp", dir);
        mkdir(tmp, S_IRWXU | S_IRWXG);
        void *h = adrenotools_open_libvulkan(RTLD_LOCAL | RTLD_NOW, ADRENOTOOLS_DRIVER_CUSTOM, tmp, hooks, dir, nome, NULL, NULL);
        if (h) {
            snprintf(g_status, sizeof g_status, "driver próprio: %s", nome);
            LOG("Vulkan pelo driver próprio %s%s", dir, nome);
            return h;
        }
        // Android novo: o linkernsbypass do libadrenotools não acha mais os símbolos privados do linker. O .so do
        // driver está na pasta privada do app (o dlopen só recusa o armazenamento comum), então tenta direto.
        char caminho[1200];
        snprintf(caminho, sizeof caminho, "%s%s", dir, nome);
        void *direto = dlopen(caminho, RTLD_NOW | RTLD_LOCAL);
        if (direto) {
            snprintf(g_status, sizeof g_status, "driver próprio (dlopen): %s", nome);
            LOG("Vulkan pelo driver próprio, sem adrenotools: %s", caminho);
            return direto;
        }
        const char *erro = dlerror();
        snprintf(g_status, sizeof g_status, "driver próprio %s não abriu (%s); usando o do sistema", nome, erro ? erro : "?");
        LOG("%s", g_status);
    }
    return dlopen("/system/lib64/libvulkan.so", RTLD_NOW | RTLD_LOCAL); // caminho absoluto: "libvulkan.so" acharia este
}

__attribute__((constructor)) static void vkshim_init(void) {
    void *h = abrir();
    if (!h) {
        LOG("nenhum libvulkan abriu: %s", dlerror());
        return;
    }
    typedef void (*Fn)(void);
    Fn (*gipa)(void *, const char *) = (Fn(*)(void *, const char *))dlsym(h, "vkGetInstanceProcAddr");
    int faltam = 0;
    for (int i = 0; i < kVkshimCount; i++) {
        g_vkshim[i] = dlsym(h, kVkshimNames[i]);
        if (!g_vkshim[i] && gipa) g_vkshim[i] = (void *)gipa(NULL, kVkshimNames[i]);
        if (!g_vkshim[i]) faltam++, LOG("função ausente: %s", kVkshimNames[i]);
    }
    LOG("%s; %d funções, %d ausentes", g_status, kVkshimCount, faltam);
}
