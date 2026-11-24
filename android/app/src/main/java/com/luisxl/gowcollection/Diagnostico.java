package com.luisxl.gowcollection;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Coletor e visualizador de diagnóstico: informações do aparelho, GPU, Vulkan, arquivos do jogo,
// logs do vitart/vkshim e logcat recente. Ajuda a identificar telas pretas, falhas de driver e crashes.
public final class Diagnostico {
    private static final String TAG = "Diagnostico";
    private static final String ARQ_SAIDA_RECENTE = "saida_recente.txt";
    private static final String ARQ_ULTIMO_CRASH = "ultimo_crash.txt";

    private Diagnostico() {}

    // Configura tratador global de exceções não capturadas
    public static void instalarTratadorExcecao(Context context) {
        final Context appCtx = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler padrao = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                registrarCrash(appCtx, thread, throwable);
            } catch (Throwable t) {
                Log.e(TAG, "Falha ao registrar crash", t);
            }
            if (padrao != null) {
                padrao.uncaughtException(thread, throwable);
            }
        });
    }

    public static void registrarCrash(Context context, Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("DATA/HORA: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n");
        sb.append("THREAD: ").append(thread != null ? thread.getName() : "desconhecida").append("\n");
        sb.append("EXCEÇÃO: ").append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        sb.append("STACKTRACE:\n");
        for (StackTraceElement el : throwable.getStackTrace()) {
            sb.append("    em ").append(el.toString()).append("\n");
        }
        Throwable c = throwable.getCause();
        while (c != null) {
            sb.append("CAUSADO POR: ").append(c.getClass().getName()).append(": ").append(c.getMessage()).append("\n");
            for (StackTraceElement el : c.getStackTrace()) {
                sb.append("    em ").append(el.toString()).append("\n");
            }
            c = c.getCause();
        }
        try {
            File f = new File(context.getFilesDir(), ARQ_ULTIMO_CRASH);
            Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            File saida = new File(context.getFilesDir(), ARQ_SAIDA_RECENTE);
            Files.write(saida.toPath(), ("O jogo travou (crash em " + throwable.getClass().getSimpleName() + ")").getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    public static void registrarSaidaPrecoce(Context context, long duracaoMs) {
        try {
            String msg = "O jogo encerrou após apenas " + (duracaoMs / 1000f) + "s (possível falha de inicialização ou Vulkan).";
            File saida = new File(context.getFilesDir(), ARQ_SAIDA_RECENTE);
            Files.write(saida.toPath(), msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    public static String verificarSaidaRecente(Context context) {
        File f = new File(context.getFilesDir(), ARQ_SAIDA_RECENTE);
        if (f.isFile()) {
            try {
                String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                //noinspection ResultOfMethodCallIgnored
                f.delete();
                return s;
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static void gravarInicioJogo(Context context, File raiz, String principal) {
        try {
            String info = "INÍCIO DO JOGO: " + principal + " às " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()) + "\n";
            File f = new File(context.getFilesDir(), "ultimo_inicio.txt");
            Files.write(f.toPath(), info.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    // Gera o relatório completo
    public static String gerarRelatorio(Context context, String motivo) {
        StringBuilder sb = new StringBuilder();
        String dataHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        sb.append("=====================================================\n");
        sb.append("       RELATÓRIO DE DIAGNÓSTICO · GOW COLLECTION     \n");
        sb.append("=====================================================\n");
        sb.append("Data/Hora: ").append(dataHora).append("\n");
        if (motivo != null && !motivo.isEmpty()) {
            sb.append("Motivo: ").append(motivo).append("\n");
        }

        // App
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            sb.append("Versão do App: ").append(pi.versionName).append(" (código ").append(pi.versionCode).append(")\n");
        } catch (PackageManager.NameNotFoundException ignored) {}

        // Dispositivo
        sb.append("\n--- [DISPOSITIVO E HARDWARE] ---\n");
        sb.append("Fabricante: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Marca: ").append(Build.BRAND).append("\n");
        sb.append("Modelo: ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(" / ").append(Build.DEVICE).append(")\n");
        sb.append("Hardware: ").append(Build.HARDWARE).append(" | Placa: ").append(Build.BOARD).append("\n");
        if (Build.VERSION.SDK_INT >= 31) {
            sb.append("SoC Model: ").append(Build.SOC_MODEL).append("\n");
        }
        sb.append("ro.board.platform: ").append(prop("ro.board.platform")).append("\n");
        sb.append("ro.soc.model: ").append(prop("ro.soc.model")).append("\n");
        sb.append("ro.vendor.qti.soc_name: ").append(prop("ro.vendor.qti.soc_name")).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        if (Build.VERSION.SDK_INT >= 23) {
            sb.append("Patch de Segurança: ").append(Build.VERSION.SECURITY_PATCH).append("\n");
        }
        sb.append("ABIs suportadas: ").append(String.join(", ", Build.SUPPORTED_ABIS)).append("\n");

        // Memória
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                long totalMb = mi.totalMem >> 20;
                long availMb = mi.availMem >> 20;
                sb.append("Memória RAM: ").append(availMb).append(" MB livres de ").append(totalMb).append(" MB");
                if (mi.lowMemory) sb.append(" [ALERTA: MEMÓRIA BAIXA]");
                sb.append("\n");
            }
        } catch (Exception e) {
            sb.append("Memória RAM: erro ao ler (").append(e.getMessage()).append(")\n");
        }

        // GPU e Vulkan
        sb.append("\n--- [GPU E DRIVER VULKAN] ---\n");
        GpuInfo gpu = GpuInfo.detectar(context);
        sb.append("GPU detectada: ").append(gpu.nome).append("\n");
        sb.append("Família da GPU: ").append(gpu.familia.rotulo).append(" (").append(gpu.familia.name()).append(")\n");
        sb.append("Origem da detecção: ").append(gpu.origem.isEmpty() ? "padrão" : gpu.origem).append("\n");
        if (!gpu.nomeKernel.isEmpty()) sb.append("Kernel GPU model: ").append(gpu.nomeKernel).append("\n");
        sb.append("É Adreno: ").append(gpu.familia.adreno() ? "Sim" : "Não").append("\n");
        sb.append("É Mali: ").append(gpu.familia.mali() ? "Sim" : "Não").append("\n");
        sb.append("É Samsung Xclipse: ").append(gpu.xclipse() ? "Sim" : "Não").append("\n");

        // HALs no sistema
        sb.append("HALs gráficos no sistema:\n");
        conferirArquivo(sb, "  /vendor/lib64/hw/vulkan.samsung.so", "/vendor/lib64/hw/vulkan.samsung.so");
        conferirArquivo(sb, "  /vendor/lib64/hw/vulkan.adreno.so", "/vendor/lib64/hw/vulkan.adreno.so");
        conferirArquivo(sb, "  /vendor/lib64/hw/vulkan.powervr.so", "/vendor/lib64/hw/vulkan.powervr.so");
        conferirArquivo(sb, "  /system/lib64/libvulkan.so", "/system/lib64/libvulkan.so");
        conferirArquivo(sb, "  /dev/mali0", "/dev/mali0");
        conferirArquivo(sb, "  /sys/class/kgsl/kgsl-3d0", "/sys/class/kgsl/kgsl-3d0");

        // Informações que o Vulkan gravou
        Map<String, String> vk = GpuInfo.vulkan(context);
        if (!vk.isEmpty()) {
            sb.append("Dados retornados pelo Vulkan no último jogo:\n");
            for (Map.Entry<String, String> e : vk.entrySet()) {
                sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
        } else {
            sb.append("Dados do Vulkan: nenhum registro prévio em files/vk_dispositivo.txt (o jogo ainda não iniciou o Vulkan).\n");
        }

        // Escolha de driver
        sb.append("Configuração de driver no app:\n");
        sb.append("  Modo: ").append(Drivers.modo(context)).append("\n");
        sb.append("  Driver selecionado: ").append(Drivers.android(context).isEmpty() ? "Automático" : Drivers.android(context)).append("\n");
        Drivers.Driver dJogo = Drivers.paraJogo(context);
        if (dJogo != null) {
            sb.append("  Driver que será usado: ").append(dJogo.nome).append(" (").append(dJogo.biblioteca).append(")\n");
        } else {
            sb.append("  Driver que será usado: Driver do sistema (aparelho)\n");
        }

        // Verificação específica Samsung / Tela Preta
        sb.append("\n--- [ANÁLISE DE COMPATIBILIDADE · SAMSUNG / BLACK SCREEN] ---\n");
        boolean isSamsung = "samsung".equalsIgnoreCase(Build.MANUFACTURER);
        if (isSamsung) {
            sb.append("! Aparelho Samsung detectado (").append(Build.MODEL).append(").\n");
            if (gpu.familia.adreno()) {
                sb.append("- Chipset Snapdragon com Adreno. Drivers Turnip podem funcionar, mas verifique se o modelo suporta.\n");
            } else {
                sb.append("- Chipset Exynos ou outro (Mali / Xclipse / PowerVR).\n");
                sb.append("- IMPORTANTE: Drivers Turnip NÃO são compatíveis com GPUs Mali ou Samsung Xclipse.\n");
                sb.append("  O modo de driver DEVE estar em 'Driver do aparelho'.\n");
                sb.append("- Em aparelhos Samsung Exynos/Mali, a composição de tela do sistema (AFBC/Gralloc) pode causar\n");
                sb.append("  tela preta quando há blit direto na swapchain. A opção de apresentação por Render Pass\n");
                sb.append("  (VITART_PRESENT_RP=1) deve estar ativa para evitar tela preta.\n");
            }
        } else if (!gpu.familia.adreno()) {
            sb.append("! GPU que não é Adreno (").append(gpu.rotulo()).append(").\n");
            sb.append("  Use sempre o 'Driver do aparelho'.\n");
        } else {
            sb.append("Aparelho Adreno padrão. Se ocorrer tela preta, teste o 'Driver do aparelho' ou outra versão do Turnip.\n");
        }

        // Pastas e arquivos do jogo
        sb.append("\n--- [PASTAS E ARQUIVOS DO JOGO] ---\n");
        File raiz = Pastas.raiz();
        sb.append("Pasta raiz: ").append(raiz.getAbsolutePath()).append("\n");
        sb.append("Pasta existe: ").append(raiz.isDirectory() ? "Sim" : "Não").append("\n");

        boolean perm = temPermissaoArmazenamento(context);
        sb.append("Permissão de arquivos totais (MANAGE_EXTERNAL_STORAGE): ").append(perm ? "Concedida" : "NÃO CONCEDIDA").append("\n");
        String faltando = Pastas.faltando(raiz);
        sb.append("Status dos arquivos do jogo: ").append(faltando == null ? "COMPLETO (Tudo OK)" : ("FALTANDO: " + faltando)).append("\n");

        sb.append("Arquivos detalhados:\n");
        conferirArquivo(sb, "  game/PCSA00126/eboot.bin", new File(raiz, "game/PCSA00126/eboot.bin"));
        conferirArquivo(sb, "  game/PCSA00126/exec/gow1_psp2_SCEA.psarc", new File(raiz, "game/PCSA00126/exec/gow1_psp2_SCEA.psarc"));
        conferirArquivo(sb, "  game/PCSA00126/exec/gow2_psp2_SCEA.psarc", new File(raiz, "game/PCSA00126/exec/gow2_psp2_SCEA.psarc"));
        conferirPastaShaders(sb, new File(raiz, "shaders"));
        conferirArquivo(sb, "  data/vitart_options.ini", new File(raiz, "data/vitart_options.ini"));
        conferirArquivo(sb, "  vitart_env.txt", new File(raiz, "vitart_env.txt"));

        // Bibliotecas nativas do APK
        sb.append("Bibliotecas nativas no APK (nativeLibraryDir):\n");
        File libDir = new File(context.getApplicationInfo().nativeLibraryDir);
        conferirArquivo(sb, "  libvitart.so", new File(libDir, "libvitart.so"));
        conferirArquivo(sb, "  libvkshim.so (camada Vulkan)", new File(libDir, "libvkshim.so"));
        conferirArquivo(sb, "  libvt_eboot.so", new File(libDir, "libvt_eboot.so"));
        conferirArquivo(sb, "  libvt_gow1.so", new File(libDir, "libvt_gow1.so"));
        conferirArquivo(sb, "  libvt_gow2.so", new File(libDir, "libvt_gow2.so"));
        conferirArquivo(sb, "  libvt_libc.so", new File(libDir, "libvt_libc.so"));
        conferirArquivo(sb, "  libvt_libfios2.so", new File(libDir, "libvt_libfios2.so"));

        // Último Crash Java
        File crashFile = new File(context.getFilesDir(), ARQ_ULTIMO_CRASH);
        if (crashFile.isFile()) {
            sb.append("\n--- [ÚLTIMO CRASH REGISTRADO] ---\n");
            try {
                sb.append(new String(Files.readAllBytes(crashFile.toPath()), StandardCharsets.UTF_8)).append("\n");
            } catch (Exception ignored) {}
        }

        // Log do vitart (armazenamento interno ou externo)
        sb.append("\n--- [LOG DO VITART (ÚLTIMAS LINHAS)] ---\n");
        File logInterno = new File(context.getFilesDir(), "vitart_internal.log");
        File logExterno = new File(raiz, "logs/vitart.log");
        if (logExterno.isFile() && logExterno.length() > 0) {
            sb.append("Arquivo: ").append(logExterno.getPath()).append(" (").append(logExterno.length()).append(" bytes)\n");
            sb.append(ultimasLinhas(logExterno, 60));
        } else if (logInterno.isFile() && logInterno.length() > 0) {
            sb.append("Arquivo (interno): ").append(logInterno.getPath()).append(" (").append(logInterno.length()).append(" bytes)\n");
            sb.append(ultimasLinhas(logInterno, 60));
        } else {
            sb.append("Nenhum arquivo de log do vitart encontrado.\n");
        }

        // Logcat recente
        sb.append("\n--- [LOGCAT RECENTE (PROCESSOS VITART / VULKAN / CRASH)] ---\n");
        sb.append(obterLogcatRecente(120));

        sb.append("\n=====================================================\n");
        sb.append("                 FIM DO DIAGNÓSTICO                  \n");
        sb.append("=====================================================\n");

        return sb.toString();
    }

    private static void conferirArquivo(StringBuilder sb, String rotulo, String caminho) {
        File f = new File(caminho);
        conferirArquivo(sb, rotulo, f);
    }

    private static void conferirArquivo(StringBuilder sb, String rotulo, File f) {
        sb.append(rotulo).append(": ");
        if (f.isFile()) {
            long kb = f.length() >> 10;
            sb.append("OK (").append(kb > 1024 ? (kb / 1024 + " MB") : (kb + " KB")).append(")\n");
        } else if (f.isDirectory()) {
            sb.append("OK (diretório)\n");
        } else if (f.exists()) {
            sb.append("OK (dispositivo especial)\n");
        } else {
            sb.append("NÃO ENCONTRADO\n");
        }
    }

    private static void conferirPastaShaders(StringBuilder sb, File pasta) {
        sb.append("  shaders: ");
        if (!pasta.isDirectory()) {
            sb.append("NÃO ENCONTRADO (pasta ausente)\n");
            return;
        }
        File[] spvs = pasta.listFiles((dir, name) -> name.endsWith(".spv"));
        File pac = new File(pasta, "_pacote.bin");
        int count = spvs != null ? spvs.length : 0;
        sb.append("OK (").append(count).append(" shaders .spv");
        if (pac.isFile()) sb.append(", pacote pré-compilado presente: ").append(pac.length() >> 10).append(" KB");
        sb.append(")\n");
    }

    private static String ultimasLinhas(File f, int maxLinhas) {
        try {
            List<String> linhas = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            int inicio = Math.max(0, linhas.size() - maxLinhas);
            StringBuilder sb = new StringBuilder();
            for (int i = inicio; i < linhas.size(); i++) {
                sb.append(linhas.get(i)).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Erro ao ler arquivo: " + e.getMessage() + "\n";
        }
    }

    private static String obterLogcatRecente(int maxLinhas) {
        StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"logcat", "-d", "-v", "time", "-t", String.valueOf(maxLinhas * 3)});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String linha;
                List<String> relevantes = new ArrayList<>();
                while ((linha = r.readLine()) != null) {
                    if (linha.contains("vitart") || linha.contains("vitart-vk") || linha.contains("vulkan")
                            || linha.contains("Adreno") || linha.contains("Mali") || linha.contains("Xclipse")
                            || linha.contains("DEBUG") || linha.contains("AndroidRuntime") || linha.contains("CRASH")
                            || linha.contains("Fatal") || linha.contains("gowcollection") || linha.contains("gralloc")) {
                        relevantes.add(linha);
                    }
                }
                int inicio = Math.max(0, relevantes.size() - maxLinhas);
                for (int i = inicio; i < relevantes.size(); i++) {
                    sb.append(relevantes.get(i)).append("\n");
                }
                if (relevantes.isEmpty()) {
                    sb.append("(Nenhuma entrada relevante encontrada no logcat recente)\n");
                }
            }
        } catch (Exception e) {
            sb.append("Falha ao coletar logcat: ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }

    private static boolean temPermissaoArmazenamento(Context c) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return c.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private static String prop(String chave) {
        try {
            @SuppressWarnings("rawtypes")
            Class sp = Class.forName("android.os.SystemProperties");
            @SuppressWarnings("unchecked")
            Method get = sp.getMethod("get", String.class);
            Object v = get.invoke(null, chave);
            return v != null ? v.toString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    // Salva o relatório nos locais padrão
    public static File salvar(Context context, String relatorio) {
        File arqInterno = new File(context.getFilesDir(), "diagnostico.txt");
        try {
            Files.write(arqInterno.toPath(), relatorio.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.w(TAG, "Falha ao gravar diagnostico interno", e);
        }

        try {
            File raiz = Pastas.raiz();
            if (raiz.isDirectory()) {
                File logsDir = new File(raiz, "logs");
                //noinspection ResultOfMethodCallIgnored
                logsDir.mkdirs();
                File arqExterno = new File(logsDir, "diagnostico.txt");
                Files.write(arqExterno.toPath(), relatorio.getBytes(StandardCharsets.UTF_8));
                return arqExterno;
            }
        } catch (Exception e) {
            Log.w(TAG, "Falha ao gravar diagnostico na pasta do jogo", e);
        }
        return arqInterno;
    }

    public static void copiar(Context context, String texto) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Diagnóstico God of War", texto));
            Toast.makeText(context, Tr.t("Diagnóstico copiado para a área de transferência!"), Toast.LENGTH_SHORT).show();
        }
    }

    public static void compartilhar(Context context, String texto) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, texto);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico God of War Collection - Android");
        sendIntent.setType("text/plain");
        Intent shareIntent = Intent.createChooser(sendIntent, Tr.t("Compartilhar diagnóstico"));
        if (!(context instanceof Activity)) {
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(shareIntent);
    }

    // Abre o diálogo interativo de diagnóstico
    public static void exibirDialogo(Activity activity) {
        activity.runOnUiThread(() -> {
            String relatorio = gerarRelatorio(activity, "Abertura pelo usuário");
            File salvo = salvar(activity, relatorio);

            AlertDialog.Builder b = new AlertDialog.Builder(activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
            b.setTitle(Tr.t("Diagnóstico do Sistema"));

            LinearLayout raiz = new LinearLayout(activity);
            raiz.setOrientation(LinearLayout.VERTICAL);
            raiz.setPadding(30, 20, 30, 10);
            raiz.setBackgroundColor(0xFF141316);

            TextView status = new TextView(activity);
            status.setText(String.format(Locale.getDefault(), "Aparelho: %s %s · Salvo em: %s", Build.MANUFACTURER, Build.MODEL, salvo.getName()));
            status.setTextColor(0xFFE0C475);
            status.setTextSize(12f);
            status.setPadding(0, 0, 0, 15);
            raiz.addView(status);

            ScrollView sv = new ScrollView(activity);
            LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
            sv.setLayoutParams(svLp);

            TextView tv = new TextView(activity);
            tv.setText(relatorio);
            tv.setTextColor(0xFFE8E8EC);
            tv.setTextSize(11f);
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextIsSelectable(true);
            tv.setPadding(10, 10, 10, 10);
            tv.setBackgroundColor(0xFF1E1D22);
            sv.addView(tv);
            raiz.addView(sv);

            LinearLayout botoes = new LinearLayout(activity);
            botoes.setOrientation(LinearLayout.HORIZONTAL);
            botoes.setGravity(Gravity.END);
            botoes.setPadding(0, 15, 0, 5);

            Button btnCopiar = new Button(activity);
            btnCopiar.setText(Tr.t("Copiar"));
            btnCopiar.setOnClickListener(v -> copiar(activity, tv.getText().toString()));
            botoes.addView(btnCopiar);

            Button btnCompartilhar = new Button(activity);
            btnCompartilhar.setText(Tr.t("Compartilhar"));
            btnCompartilhar.setOnClickListener(v -> compartilhar(activity, tv.getText().toString()));
            botoes.addView(btnCompartilhar);

            raiz.addView(botoes);

            b.setView(raiz);
            b.setPositiveButton(Tr.t("Fechar"), (d, w) -> d.dismiss());
            b.show();
        });
    }
}
