package com.luisxl.gowcollection;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// O jogo: SurfaceView com o Vulkan do vitart e os controles por cima. Roda no processo ":jogo" porque o LoadExec
// do Vita (menu da coletânea -> GoW1/GoW2) troca o processo inteiro, como no console.
public final class GameActivity extends Activity implements SurfaceHolder.Callback, ControlesView.Saida {
    static final String EXTRA_PRINCIPAL = "principal"; // biblioteca do módulo principal (libvt_eboot.so...)
    static final String PREVIA = "previa"; // no lugar da biblioteca: só os controles, para arrumar antes de jogar

    static native void nativeSetenv(String k, String v);

    // o vitart apresentou ~10 s de jogo: o driver em teste serve (vt::droid::session_ready, thread do jogo)
    @SuppressWarnings("unused")
    static void sessaoPronta(String nada) {
        GameActivity a = ativa;
        if (a != null) Drivers.sessaoPronta(a.getApplicationContext());
    }
    static native void nativeStart(String[] args, String cwd);
    static native void nativeSurface(Object surface);
    static native void nativeSurfaceSize(int w, int h);
    static native void nativeVisible(boolean on);
    static native void nativeRefreshRate(int hz);
    static native void nativePad(int buttons, int lx, int ly, int rx, int ry, int active);
    static native void nativeTouch(boolean down, float nx, float ny);
    static native void nativeKey(int vk, boolean down);
    static native boolean nativeTrophyTouch(int x, int y);
    static native void nativeTrophyTop(float frac);

    // onde acabam os controles do alto (fração da altura): o aviso de troféu desce para baixo deles
    static void topoLivreTrofeu(float frac) {
        try {
            nativeTrophyTop(frac);
        } catch (UnsatisfiedLinkError e) {
            // prévia dos controles: sem o jogo carregado
        }
    }

    static boolean tocarTrofeu(float x, float y) {
        try {
            return nativeTrophyTouch(Math.round(x), Math.round(y));
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static GameActivity ativa;
    private static long inicioTempoJogo = 0;
    private ControlesView controles;
    private final Handler ui = new Handler(Looper.getMainLooper());

    // o que a tela e o controle físico pedem, somados em enviar()
    private int telaBotoes, telaLx = 128, telaLy = 128, telaRx = 128, telaRy = 128;
    private int fisBotoes;
    private float fisLx, fisLy, fisRx, fisRy, fisL2, fisR2;
    private int hatBotoes;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ativa = this;
        Diagnostico.instalarTratadorExcecao(this);
        // driver Vulkan: o libvulkan.so do APK (runtime/vkshim) lê isto no próprio carregamento, que é o do vitart
        // (critérios do DroidDeck: Drivers.paraJogo; a abertura conta para o teste de um driver novo)
        try {
            Drivers.Driver d = Drivers.paraJogo(this);
            if (d != null) Drivers.jogoIniciando(this, d.pasta);
            android.system.Os.setenv("VITART_VK_LIBDIR", getApplicationInfo().nativeLibraryDir + "/", true);
            android.system.Os.setenv("VITART_VK_DRIVER_DIR", d != null ? new File(Drivers.base(this), d.pasta).getPath() + "/" : "", true);
            android.system.Os.setenv("VITART_VK_DRIVER", d != null ? d.biblioteca : "", true);
        } catch (Exception ignorada) {
            // sem as variáveis o shim usa o driver do aparelho
        }
        System.loadLibrary("vitart");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
        FrameLayout raiz = new FrameLayout(this);
        String principal = getIntent().getStringExtra(EXTRA_PRINCIPAL);
        boolean previa = PREVIA.equals(principal);
        if (Build.VERSION.SDK_INT >= 33) // o voltar pelo sistema novo: mesmo tratamento do onBackPressed
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::voltarDoSistema);
        Pastas.carregar(this);
        File pasta = Pastas.raiz();
        Arte.carregar(pasta);
        Arte.carregarBases(getAssets());
        if (previa) raiz.addView(new Fundo(this));
        else {
            SurfaceView sv = new SurfaceView(this);
            sv.getHolder().addCallback(this);
            raiz.addView(sv);
        }
        controles = new ControlesView(this, this);
        controles.previa(previa);
        raiz.addView(controles);
        setContentView(raiz);
        telaCheia();
        nativeRefreshRate(Math.round(getWindowManager().getDefaultDisplay().getRefreshRate()));
        if (!previa) iniciar(pasta, principal);
        else Toast.makeText(this, Tr.t("Prévia dos controles: o ômega no alto abre as opções"), Toast.LENGTH_LONG).show();
    }

    // a mesma linha de comando do vitart.exe do PC, com a pasta copiada do PC no armazenamento do celular
    private void iniciar(File raiz, String principal) {
        inicioTempoJogo = android.os.SystemClock.elapsedRealtime();
        Diagnostico.gravarInicioJogo(this, raiz, principal);
        Diagnostico.salvar(this, Diagnostico.gerarRelatorio(this, "Ao abrir o jogo")); // o log para o blackscreen do app
        String libs = getApplicationInfo().nativeLibraryDir;
        if (principal == null || principal.isEmpty()) principal = libs + "/libvt_eboot.so";
        else if (!principal.startsWith("/")) principal = libs + "/" + principal;
        File logs = new File(raiz, "logs");
        //noinspection ResultOfMethodCallIgnored
        logs.mkdirs();
        nativeSetenv("VITART_VULKAN", "1");
        nativeSetenv("VITART_LANG", String.valueOf(Idioma.atual(this))); // o idioma do lançador (jogo e troféus)
        // o language= do ini manda no jogo (e nos menus do vitart, que vêm da tradução): segue o do lançador a cada
        // abertura, senão trocar o idioma do Android ou do lançador deixava o jogo no idioma da instalação
        Opcoes.gravar(raiz, "language", Idioma.traducao(Idioma.atual(this)));
        nativeSetenv("VITART_SPV_DIR", new File(raiz, "shaders").getPath());
        limitesDoPack(); // antes do vitart_env.txt: ele pode mudar
        // Aparelhos Samsung (Exynos/Mali/Xclipse) e outras GPUs não-Adreno: o blit direto na swapchain
        // comprimida (AFBC) produz tela preta. Ativa apresentação por render pass por padrão.
        try {
            GpuInfo gpu = GpuInfo.detectar(this);
            boolean samsung = "samsung".equalsIgnoreCase(Build.MANUFACTURER);
            if (samsung || !gpu.familia.adreno()) {
                nativeSetenv("VITART_PRESENT_RP", "1");
            }
        } catch (Exception ignorada) {
            // segue padrão
        }
        // diagnóstico: <pasta>/vitart_env.txt com CHAVE=valor por linha (as variáveis VITART_* do PC)
        File env = new File(raiz, "vitart_env.txt");
        if (env.isFile()) {
            try {
                for (String l : java.nio.file.Files.readAllLines(env.toPath())) {
                    int eq = l.indexOf('=');
                    if (eq > 0 && !l.startsWith("#")) nativeSetenv(l.substring(0, eq).trim(), l.substring(eq + 1).trim());
                }
            } catch (Exception ignorada) {
                // arquivo ilegível: segue sem
            }
        }
        List<String> a = new ArrayList<>();
        a.add("--app"); a.add(new File(raiz, "game/PCSA00126").getPath());
        a.add("--data"); a.add(new File(raiz, "data").getPath());
        a.add("--hle-vars"); a.add(extrair("link.hlevars.txt").getPath());
        a.add("--log"); a.add(new File(logs, "vitart.log").getPath());
        a.add(principal);
        a.add(libs + "/libvt_libc.so");
        a.add(libs + "/libvt_libfios2.so");
        nativeStart(a.toArray(new String[0]), getFilesDir().getPath());
    }

    // o link.hlevars.txt vai no APK (casa com as bibliotecas desta versão)
    private File extrair(String nome) {
        File f = new File(getFilesDir(), nome);
        try (InputStream in = getAssets().open(nome); FileOutputStream out = new FileOutputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Toast.makeText(this, Tr.t("Falta ") + nome + Tr.t(" no APK"), Toast.LENGTH_LONG).show();
        }
        return f;
    }

    private void telaCheia() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean foco) {
        super.onWindowFocusChanged(foco);
        if (foco) telaCheia();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nativeVisible(true);
    }

    @Override
    protected void onPause() {
        nativeVisible(false);
        super.onPause();
    }

    // ---------------------------------------------------------------- superfície
    @Override
    public void surfaceCreated(SurfaceHolder h) { nativeSurface(h.getSurface()); }

    @Override
    public void surfaceChanged(SurfaceHolder h, int f, int w, int hh) { nativeSurfaceSize(w, hh); }

    @Override
    public void surfaceDestroyed(SurfaceHolder h) { nativeSurface(null); } // espera o Vulkan soltar

    // ---------------------------------------------------------------- controles da tela
    @Override
    public void controles(int botoes, int lx, int ly, int rx, int ry) {
        telaBotoes = botoes; telaLx = lx; telaLy = ly; telaRx = rx; telaRy = ry;
        enviar(0);
    }

    @Override
    public void toque(boolean dedo, float nx, float ny) { nativeTouch(dedo, nx, ny); }

    // O pack de texturas HD na medida do aparelho (hle_gxm.cpp: VITART_TEXPACK_MAX, VITART_TEXPACK_MB). Cada PNG do
    // pack vira RGBA8 na CPU e na GPU: até 4 GB de RAM, no máximo 1024 px e 384 MB; até 6 GB, 2048 px e 640 MB; até 8 GB,
    // 1024 MB; acima, o do PC (1,5 GB). Mali e PowerVR (menos banda de memória) ficam em no máximo 2048 px.
    private void limitesDoPack() {
        long ram = 0;
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            ram = mi.totalMem;
        } catch (Exception ignorada) {
            // sem a conta: os limites do meio
        }
        long gb = ram >> 30;
        int lado, mb;
        if (ram == 0 || gb < 5) { lado = 1024; mb = 384; }
        else if (gb < 7) { lado = 2048; mb = 640; }
        else if (gb < 9) { lado = 0; mb = 1024; }
        else { lado = 0; mb = 1536; }
        try {
            GpuInfo g = GpuInfo.detectar(this);
            if ((g.familia.mali() || g.familia == GpuInfo.Familia.POWERVR) && (lado == 0 || lado > 2048)) lado = 2048;
            // Adreno 6xx e as 7xx de entrada (Snapdragon 8xx antigos, 7 Gen 1...): banda de memória menor que a dos
            // tops atuais; as texturas HD de 4096 px pesavam no FPS em 2x
            if ((g.familia == GpuInfo.Familia.A6XX || g.familia == GpuInfo.Familia.A7XX_BAIXO) && (lado == 0 || lado > 2048)) lado = 2048;
        } catch (Exception ignorada) {
            // GPU desconhecida: fica o da RAM
        }
        nativeSetenv("VITART_TEXPACK_MAX", String.valueOf(lado));
        nativeSetenv("VITART_TEXPACK_MB", String.valueOf(mb));
        android.util.Log.i("vitart", "pack HD: " + gb + " GB de RAM -> lado máximo " + (lado == 0 ? "o do PNG" : lado + " px") + ", " + mb + " MB");
    }

    // chamado pelo vitart (jni.cpp, "Quit to Desktop" do menu de pausa): volta ao lançador
    @SuppressWarnings("unused")
    static void sairDoJogo(String ignorado) {
        GameActivity a = ativa;
        if (a != null) a.ui.post(a::sair);
    }

    @Override
    public void sair() {
        // o jogo e a prévia abrem numa tarefa nova (que fecha a do lançador): sair reabre o lançador em vez de cair
        // na tela inicial do Android
        try {
            Intent l = new Intent(this, LauncherActivity.class);
            l.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(l);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Exception ignorada) {
            // sem o lançador: só fecha
        }
        finish();
        Process.killProcess(Process.myPid());
    }

    @Override
    public void onBackPressed() {
        voltarDoSistema();
    }

    // o voltar do sistema sai do modo de mudar posições e fecha o menu do Ω; fora disso, no jogo não fecha nada
    // sem querer (use o menu do ômega); na prévia, volta ao lançador
    private void voltarDoSistema() {
        if (controles != null && controles.voltar()) return;
        if (PREVIA.equals(getIntent().getStringExtra(EXTRA_PRINCIPAL))) sair();
    }

    // fundo da prévia: a arte-chave do jogo escurecida, ou pedra
    private static final class Fundo extends View {
        private Bitmap img;

        Fundo(android.content.Context c) {
            super(c);
            File f = new File(Pastas.raiz(), "data/ui-arte/chave.jpg");
            if (f.isFile()) {
                BitmapFactory.Options op = new BitmapFactory.Options();
                op.inSampleSize = 2;
                img = BitmapFactory.decodeFile(f.getPath(), op);
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
            if (img != null) {
                c.drawBitmap(img, null, new RectF(0, 0, getWidth(), getHeight()), p);
            } else {
                Arte.pedra(p, getWidth() / 2f, getHeight() / 2f, getWidth());
                c.drawRect(0, 0, getWidth(), getHeight(), p);
            }
            Paint v = new Paint();
            v.setColor(0x66000000);
            c.drawRect(0, 0, getWidth(), getHeight(), v);
        }
    }

    // botões somados; analógico: o mais inclinado entre a tela e o controle
    private void enviar(int ativo) {
        int b = telaBotoes | fisBotoes | hatBotoes;
        if (fisL2 > 0.5f) b |= ControlesView.L2;
        if (fisR2 > 0.5f) b |= ControlesView.R2;
        int lx = telaLx, ly = telaLy, rx = telaRx, ry = telaRy;
        if (Math.hypot(fisLx, fisLy) * 127 > Math.hypot(lx - 128, ly - 128)) {
            lx = eixo(fisLx); ly = eixo(fisLy);
        }
        if (Math.hypot(fisRx, fisRy) * 127 > Math.hypot(rx - 128, ry - 128)) {
            rx = eixo(fisRx); ry = eixo(fisRy);
        }
        nativePad(b, lx, ly, rx, ry, ativo);
    }

    private static int eixo(float v) { return Math.max(0, Math.min(255, Math.round(128 + v * 127))); }

    // ---------------------------------------------------------------- controle físico
    private static boolean doControle(InputDevice d) {
        if (d == null) return false;
        int s = d.getSources();
        return (s & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (s & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    private static int botaoDoControle(int k) {
        switch (k) {
            case KeyEvent.KEYCODE_BUTTON_A: return ControlesView.CRUZ;
            case KeyEvent.KEYCODE_BUTTON_B: return ControlesView.CIRCULO;
            case KeyEvent.KEYCODE_BUTTON_X: return ControlesView.QUADRADO;
            case KeyEvent.KEYCODE_BUTTON_Y: return ControlesView.TRIANGULO;
            case KeyEvent.KEYCODE_BUTTON_L1: return ControlesView.L1;
            case KeyEvent.KEYCODE_BUTTON_R1: return ControlesView.R1;
            case KeyEvent.KEYCODE_BUTTON_L2: return ControlesView.L2;
            case KeyEvent.KEYCODE_BUTTON_R2: return ControlesView.R2;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: return ControlesView.L3;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: return ControlesView.R3;
            case KeyEvent.KEYCODE_BUTTON_START: return ControlesView.START;
            case KeyEvent.KEYCODE_BUTTON_SELECT: return ControlesView.SELECT;
            case KeyEvent.KEYCODE_DPAD_UP: return ControlesView.CIMA;
            case KeyEvent.KEYCODE_DPAD_DOWN: return ControlesView.BAIXO;
            case KeyEvent.KEYCODE_DPAD_LEFT: return ControlesView.ESQUERDA;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return ControlesView.DIREITA;
            default: return 0;
        }
    }

    // teclado físico nos códigos VK_ do Windows, que é como o mapa de teclado do jogo está escrito
    private static int tecla(int k) {
        if (k >= KeyEvent.KEYCODE_A && k <= KeyEvent.KEYCODE_Z) return 'A' + (k - KeyEvent.KEYCODE_A);
        if (k >= KeyEvent.KEYCODE_0 && k <= KeyEvent.KEYCODE_9) return '0' + (k - KeyEvent.KEYCODE_0);
        switch (k) {
            case KeyEvent.KEYCODE_SPACE: return 0x20;
            case KeyEvent.KEYCODE_ENTER: return 0x0D;
            case KeyEvent.KEYCODE_ESCAPE: return 0x1B;
            case KeyEvent.KEYCODE_DEL: return 0x08;
            case KeyEvent.KEYCODE_TAB: return 0x09;
            case KeyEvent.KEYCODE_SHIFT_LEFT: case KeyEvent.KEYCODE_SHIFT_RIGHT: return 0x10;
            case KeyEvent.KEYCODE_CTRL_LEFT: return 0xA2;
            case KeyEvent.KEYCODE_CTRL_RIGHT: return 0xA3;
            case KeyEvent.KEYCODE_DPAD_UP: return 0x26;
            case KeyEvent.KEYCODE_DPAD_DOWN: return 0x28;
            case KeyEvent.KEYCODE_DPAD_LEFT: return 0x25;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return 0x27;
            default: return 0;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.KEYCODE_VOLUME_UP || k == KeyEvent.KEYCODE_VOLUME_DOWN || k == KeyEvent.KEYCODE_VOLUME_MUTE)
            return super.dispatchKeyEvent(e);
        boolean desce = e.getAction() == KeyEvent.ACTION_DOWN;
        if (e.getAction() != KeyEvent.ACTION_DOWN && e.getAction() != KeyEvent.ACTION_UP) return super.dispatchKeyEvent(e);
        if (doControle(e.getDevice()) || KeyEvent.isGamepadButton(k)) {
            int bit = k == KeyEvent.KEYCODE_BACK ? ControlesView.SELECT : botaoDoControle(k);
            if (bit != 0) {
                fisBotoes = desce ? fisBotoes | bit : fisBotoes & ~bit;
                controles.controleFisico();
                enviar(2);
                return true;
            }
        }
        int vk = tecla(k);
        if (vk != 0) {
            nativeKey(vk, desce);
            if (vk == 0xA2 || vk == 0xA3) nativeKey(0x11, desce);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent e) {
        if (doControle(e.getDevice()) && e.getAction() == MotionEvent.ACTION_MOVE) {
            InputDevice d = e.getDevice();
            fisLx = zona(e, d, MotionEvent.AXIS_X);
            fisLy = zona(e, d, MotionEvent.AXIS_Y);
            fisRx = zona(e, d, MotionEvent.AXIS_Z);
            fisRy = zona(e, d, MotionEvent.AXIS_RZ);
            fisL2 = Math.max(e.getAxisValue(MotionEvent.AXIS_LTRIGGER), e.getAxisValue(MotionEvent.AXIS_BRAKE));
            fisR2 = Math.max(e.getAxisValue(MotionEvent.AXIS_RTRIGGER), e.getAxisValue(MotionEvent.AXIS_GAS));
            float hx = e.getAxisValue(MotionEvent.AXIS_HAT_X), hy = e.getAxisValue(MotionEvent.AXIS_HAT_Y);
            hatBotoes = (hx < -0.5f ? ControlesView.ESQUERDA : 0) | (hx > 0.5f ? ControlesView.DIREITA : 0)
                    | (hy < -0.5f ? ControlesView.CIMA : 0) | (hy > 0.5f ? ControlesView.BAIXO : 0);
            if (Math.abs(fisLx) + Math.abs(fisLy) + Math.abs(fisRx) + Math.abs(fisRy) + fisL2 + fisR2 > 0.2f || hatBotoes != 0)
                controles.controleFisico();
            enviar(2);
            return true;
        }
        if (e.isFromSource(InputDevice.SOURCE_MOUSE)) { // botões do mouse para o mapa de teclado/mouse do jogo
            int bs = e.getButtonState();
            nativeKey(0x01, (bs & MotionEvent.BUTTON_PRIMARY) != 0);
            nativeKey(0x02, (bs & MotionEvent.BUTTON_SECONDARY) != 0);
            nativeKey(0x04, (bs & MotionEvent.BUTTON_TERTIARY) != 0);
        }
        return super.dispatchGenericMotionEvent(e);
    }

    private static float zona(MotionEvent e, InputDevice d, int eixo) {
        float v = e.getAxisValue(eixo);
        InputDevice.MotionRange r = d.getMotionRange(eixo, e.getSource());
        float z = r != null ? Math.max(r.getFlat(), 0.12f) : 0.12f;
        if (Math.abs(v) < z) return 0;
        return Math.signum(v) * (Math.abs(v) - z) / (1 - z);
    }

    // ---------------------------------------------------------------- chamado pelo vitart (jni.cpp)
    @SuppressWarnings("unused")
    static void playSound(String wav) {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build());
            if (wav != null && !wav.isEmpty() && new File(wav).isFile()) mp.setDataSource(wav);
            else if (ativa != null)
                mp.setDataSource(ativa, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            mp.setOnCompletionListener(MediaPlayer::release);
            mp.prepare();
            mp.start();
        } catch (Exception ignorada) {
            // sem som de troféu não é motivo para cair
        }
    }

    @SuppressWarnings("unused")
    static void notifyText(String s) {
        GameActivity a = ativa;
        if (a != null && s != null && !s.isEmpty())
            a.ui.post(() -> Toast.makeText(a, s.replace('\n', ' '), Toast.LENGTH_LONG).show());
    }

    // O aviso de troféu do PS5 (faixa escura, ícone, "Você ganhou um troféu" na cor do nível, nome), do mesmo
    // tamanho do aviso do PC. Volta [largura, altura, raio, pixels ARGB...].
    @SuppressWarnings("unused")
    static int[] renderTrophy(byte[] icone, char nivel, String nome, String detalhe, int alturaTela) {
        float k = Math.max(0.75f, alturaTela / 544f);
        int margem = Math.round(12 * k), ic = Math.round(48 * k), w = Math.round(400 * k), h = ic + margem;
        // aberto (tocaram no aviso): a descrição embaixo, em até 3 linhas
        java.util.List<String> linhas = new java.util.ArrayList<>();
        Paint pd = new Paint(Paint.ANTI_ALIAS_FLAG);
        pd.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));
        pd.setTextSize(12.5f * k);
        if (detalhe != null && !detalhe.isEmpty()) {
            w = Math.round(460 * k);
            float max = w - 2 * margem;
            String resto = detalhe.trim();
            while (!resto.isEmpty() && linhas.size() < 3) {
                int n = pd.breakText(resto, true, max, null);
                if (n < resto.length()) { int sp = resto.lastIndexOf(' ', n); if (sp > 0) n = sp; }
                String l = resto.substring(0, n).trim();
                resto = resto.substring(n).trim();
                if (linhas.size() == 2 && !resto.isEmpty()) l = l + "…";
                linhas.add(l);
            }
            h += Math.round(linhas.size() * 17 * k + 6 * k);
        }
        int raio = Math.round(15 * k);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setColor(0xFF1E1F22);
        c.drawRoundRect(new RectF(0, 0, w, h), raio, raio, p);
        int x = margem / 2;
        int topo = ic + margem; // altura da faixa sem a descrição
        if (icone != null && icone.length > 0) {
            Bitmap ib = BitmapFactory.decodeByteArray(icone, 0, icone.length);
            if (ib != null) {
                c.drawBitmap(ib, null, new RectF(x, margem / 2f, x + ic, margem / 2f + ic), p);
                ib.recycle();
            }
        }
        int cor;
        switch (nivel) {
            case 'P': cor = 0xFF9FC6F0; break;
            case 'G': cor = 0xFFE8C45C; break;
            case 'S': cor = 0xFFC6CBD2; break;
            default: cor = 0xFFD08B4C;
        }
        float tx = x + ic + margem;
        p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        p.setTextSize(13 * k);
        p.setColor(cor);
        c.drawText(Tr.t("Você ganhou um troféu"), tx, topo * 0.42f, p);
        p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        p.setTextSize(15 * k);
        p.setColor(0xFFF2F2F2);
        String n = nome == null ? "" : nome;
        float max = w - tx - margem;
        while (n.length() > 1 && p.measureText(n) > max) n = n.substring(0, n.length() - 2) + "…";
        c.drawText(n, tx, topo * 0.78f, p);
        if (!linhas.isEmpty()) { // descrição, com um filete separando
            Paint f = new Paint();
            f.setColor(0x33FFFFFF);
            c.drawRect(margem, topo, w - margem, topo + Math.max(1, Math.round(k)), f);
            pd.setColor(0xFFD8D8DC);
            for (int li = 0; li < linhas.size(); li++)
                c.drawText(linhas.get(li), margem, topo + (li + 1) * 17 * k, pd);
        }
        int[] out = new int[3 + w * h];
        out[0] = w; out[1] = h; out[2] = raio;
        bmp.getPixels(out, 3, w, 0, 0, w, h);
        bmp.recycle();
        // o renderizador copia as linhas sem misturar: dentro da faixa tudo opaco
        for (int i = 3; i < out.length; i++) if ((out[i] >>> 24) >= 128) out[i] |= 0xFF000000; else out[i] = 0;
        return out;
    }

    // LoadExec: processo novo com outro módulo principal. O RelaunchActivity espera este morrer e abre o jogo de novo.
    @SuppressWarnings("unused")
    static void relaunch(String principal) {
        GameActivity a = ativa;
        if (a == null) return;
        Intent i = new Intent(a, RelaunchActivity.class);
        i.putExtra(EXTRA_PRINCIPAL, principal);
        i.putExtra(RelaunchActivity.EXTRA_PID, Process.myPid());
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        a.startActivity(i);
        a.overridePendingTransition(0, 0);
        Process.killProcess(Process.myPid());
    }

    @SuppressWarnings("unused")
    static void onGameExit(String ignorado) {
        GameActivity a = ativa;
        if (a != null) {
            long duracao = android.os.SystemClock.elapsedRealtime() - inicioTempoJogo;
            if (duracao < 5000 && inicioTempoJogo > 0) {
                Diagnostico.registrarSaidaPrecoce(a, duracao);
                Diagnostico.salvar(a, Diagnostico.gerarRelatorio(a, "Encerramento precoce (< 5s)"));
            }
            a.ui.post(a::sair);
        }
    }
}
