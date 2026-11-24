package com.luisxl.gowcollection;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

// Atualização pelas releases do GitHub (Instalador.REPO_APP, o fork): a release mais nova que a versão instalada traz o APK
// (qualquer asset .apk) e, se tiver, um pacote de dados "atualizacao__*" (cifrado com a mesma chave da instalação,
// guardada em files/chave). Baixa com barra de progresso e instala pelo PackageInstaller: do Android 12 em diante
// sem pedir confirmação quando este app foi quem instalou a versão atual; senão o Android mostra a tela de instalar.
final class Atualizador {
    static final String ACAO_INSTALOU = "com.luisxl.gowcollection.INSTALOU";

    // "v0.3.0", "android-v1.2" -> números para comparar
    static int[] versao(String s) {
        String[] p = s.replaceAll("[^0-9.]", "").replaceAll("^\\.+", "").split("\\.");
        int[] v = new int[3];
        for (int i = 0; i < 3 && i < p.length; i++) try { v[i] = Integer.parseInt(p[i]); } catch (Exception ignorada) { /* 0 */ }
        return v;
    }

    static boolean maisNova(String tag, String atual) {
        int[] a = versao(tag), b = versao(atual);
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return a[i] > b[i];
        return false;
    }

    static String versaoInstalada(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    static Instalador.Asset apkDe(Instalador.Release r) {
        for (Instalador.Asset a : r.assets) if (a.nome.toLowerCase().endsWith(".apk")) return a;
        return null;
    }

    // o Android só deixa o app instalar outro APK com a permissão de "instalar apps desconhecidos"
    static boolean podeInstalar(Activity a) {
        if (Build.VERSION.SDK_INT < 26 || a.getPackageManager().canRequestPackageInstalls()) return true;
        try {
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
        } catch (Exception ignorada) {
            // sem a tela: o Android pergunta na hora de instalar
        }
        return false;
    }

    static File baixarApk(Context c, Instalador.Asset a, Instalador.Aviso av, Instalador.Cancelar cn) throws Exception {
        File f = new File(c.getExternalCacheDir() != null ? c.getExternalCacheDir() : c.getCacheDir(), a.nome);
        Instalador.baixar(a.url, f, a.tamanho, 0, a.tamanho, Tr.t("Baixando a atualização"), av, cn);
        return f;
    }

    // entrega o APK ao PackageInstaller; o resultado volta à LauncherActivity (ACAO_INSTALOU)
    static void instalarApk(Activity act, File apk) throws Exception {
        PackageInstaller pi = act.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        if (Build.VERSION.SDK_INT >= 31) sp.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        sp.setAppPackageName(act.getPackageName());
        int id = pi.createSession(sp);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (InputStream in = new FileInputStream(apk); OutputStream o = s.openWrite("app.apk", 0, apk.length())) {
                byte[] b = new byte[1 << 20];
                int n;
                while ((n = in.read(b)) > 0) o.write(b, 0, n);
                s.fsync(o);
            }
            Intent i = new Intent(act, LauncherActivity.class).setAction(ACAO_INSTALOU).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int fl = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            s.commit(PendingIntent.getActivity(act, 7, i, fl).getIntentSender());
        }
    }

    // o resultado da sessão: pedir confirmação ao usuário quando o Android exige
    static boolean tratar(Activity act, Intent it) {
        if (it == null || !ACAO_INSTALOU.equals(it.getAction())) return false;
        int st = it.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent conf = it.getParcelableExtra(Intent.EXTRA_INTENT);
            if (conf != null) act.startActivity(conf.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (st != PackageInstaller.STATUS_SUCCESS) {
            String m = it.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            android.widget.Toast.makeText(act, Tr.t("A atualização não instalou: ") + (m != null ? m : st), android.widget.Toast.LENGTH_LONG).show();
        }
        return true;
    }

    // pacote de dados da release (atualizacao__*): aplicado uma vez por release, com a chave da instalação
    static boolean aplicarDados(Context c, Instalador.Release r, File raiz, Instalador.Aviso av, Instalador.Cancelar cn) throws Exception {
        boolean tem = false;
        for (Instalador.Asset a : r.assets) if (a.nome.startsWith("atualizacao__")) tem = true;
        if (!tem) return false;
        String feita = c.getSharedPreferences("atualizacao", Context.MODE_PRIVATE).getString("dados", "");
        if (r.tag.equals(feita)) return false;
        byte[] K = Instalador.chaveGuardada(c);
        if (K == null) return false; // instalado à mão (pasta copiada do PC): sem a chave, os dados vêm com a pasta
        File dir = new File(raiz, ".atualizacao");
        Instalador.baixarGrupo(r, "atualizacao", dir, av, cn);
        Instalador.Pacote p = Instalador.abrir(dir, "atualizacao", K);
        Instalador.instalar(p, K, raiz, Tr.t("Atualizando os dados do jogo"), av, cn);
        apagar(dir);
        c.getSharedPreferences("atualizacao", Context.MODE_PRIVATE).edit().putString("dados", r.tag).apply();
        return true;
    }

    static void apagar(File d) {
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) if (f.isDirectory()) apagar(f); else //noinspection ResultOfMethodCallIgnored
            f.delete();
        //noinspection ResultOfMethodCallIgnored
        d.delete();
    }

    private Atualizador() {}
}
