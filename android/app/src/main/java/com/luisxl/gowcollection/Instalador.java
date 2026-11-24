package com.luisxl.gowcollection;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

// A primeira instalação, nos moldes do instalador do PC (vita-recomp-lab: apps/instalador/motor.js e
// tools/empacotar_instalador.js): o app do Vita sai do PKG americano de quem instala (libpfs_extrair.so) e o resto
// (shaders, patches, traduções, áudio do PS2...) vem das releases do GitHub, cifrado com uma chave que só sai do work.bin
// e do PKG dessa pessoa:
//   K = SHA-256("vitart-gow-instalador|" + chave do conteúdo do work.bin (+0x50) + SHA-256 do PKG)
// manifesto.bin = "VTPK1\n" + u32 LE tamanho + cabeçalho JSON em claro + manifesto JSON (deflate cru, AES-256-CTR com
// IV de "manifesto"); cada arquivo: AES-256-CTR com IV = SHA-256(K + "|iv|" + caminho)[0..16), deflate cru se c = 1,
// nas partes gow-NN.bin (o deslocamento o é do fluxo lógico).
final class Instalador {
    static final String CONTENT_ID = "UP9000-PCSA00126_00-GOWCUS030100SHIP";
    static final long PKG_TAMANHO = 3336914560L;
    // de onde vêm os DADOS (sempre o repositório original, que tem as releases cifradas)
    static final String REPO = "luisxl15/GOD-OF-WAR-HD-COLLECTION-ANDROID-PORT";
    // de onde vem o APK da atualização (este fork)
    static final String REPO_APP = "nizarrk/GOD-OF-WAR-HD-COLLECTION-ANDROID-PORT";

    interface Aviso {
        // etapa (texto para a tela), feito/total (bytes; total 0 = sem barra)
        void andou(String etapa, long feito, long total);
    }

    static final class Erro extends Exception {
        Erro(String m) { super(m); }
    }

    // ------------------------------------------------------------------ achar os arquivos
    static File acharPkg() {
        for (File d : lugares()) {
            File[] fs = d.listFiles((dir, n) -> n.toLowerCase().endsWith(".pkg"));
            if (fs != null) for (File f : fs) if (f.length() == PKG_TAMANHO) return f;
        }
        return null;
    }

    static File acharLicenca() {
        for (File d : lugares()) {
            File[] fs = d.listFiles((dir, n) -> n.toLowerCase().endsWith(".bin") || n.toLowerCase().endsWith(".rif"));
            if (fs != null)
                for (File f : fs)
                    if (f.length() == 512)
                        try {
                            lerLicenca(f);
                            return f;
                        } catch (Exception ignorada) {
                            // outro arquivo de 512 bytes
                        }
        }
        return null;
    }

    private static List<File> lugares() {
        List<File> l = new ArrayList<>();
        File raiz = Environment.getExternalStorageDirectory();
        l.add(new File(raiz, Pastas.NOME));
        l.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        l.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
        l.add(raiz);
        return l;
    }

    // ------------------------------------------------------------------ chaves
    static byte[] lerLicenca(File rif) throws Exception {
        byte[] b = new byte[(int) rif.length()];
        try (FileInputStream in = new FileInputStream(rif)) {
            if (in.read(b) != b.length) throw new Erro(Tr.t("work.bin incompleto"));
        }
        if (b.length != 512) throw new Erro(Tr.t("O work.bin precisa ter 512 bytes"));
        String cid = new String(b, 0x10, 0x30, StandardCharsets.ISO_8859_1);
        int z = cid.indexOf('\0');
        if (z >= 0) cid = cid.substring(0, z);
        if (!CONTENT_ID.equals(cid)) throw new Erro(Tr.t("work.bin de outro jogo: ") + cid);
        byte[] k = new byte[16];
        System.arraycopy(b, 0x50, k, 0, 16);
        return k;
    }

    static void conferirPkg(File pkg) throws Exception {
        if (pkg.length() != PKG_TAMANHO) throw new Erro(Tr.t("O PKG não é o americano (PCSA00126) ou está incompleto"));
        byte[] h = new byte[0x60];
        try (FileInputStream in = new FileInputStream(pkg)) {
            if (in.read(h) != h.length) throw new Erro(Tr.t("PKG ilegível"));
        }
        if (h[0] != 0x7f || h[1] != 'P' || h[2] != 'K' || h[3] != 'G') throw new Erro(Tr.t("Não é um PKG"));
        String cid = new String(h, 0x30, 36, StandardCharsets.ISO_8859_1);
        if (!CONTENT_ID.equals(cid)) throw new Erro(Tr.t("PKG de outro jogo: ") + cid);
    }

    static byte[] sha256Pkg(File pkg, Aviso av, Cancelar c) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] buf = new byte[8 << 20];
        long feito = 0, total = pkg.length();
        try (FileInputStream in = new FileInputStream(pkg)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                c.conferir();
                md.update(buf, 0, n);
                feito += n;
                av.andou(Tr.t("Conferindo o PKG"), feito, total);
            }
        }
        return md.digest();
    }

    static byte[] derivar(byte[] klic, byte[] shaPkg) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update("vitart-gow-instalador|".getBytes(StandardCharsets.US_ASCII));
        md.update(klic);
        md.update(shaPkg);
        return md.digest();
    }

    static String verificaChave(byte[] K) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(K, "HmacSHA256"));
        return hex(m.doFinal("verifica".getBytes(StandardCharsets.US_ASCII)));
    }

    static byte[] ivDe(byte[] K, String nome) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(K);
        md.update(("|iv|" + nome).getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[16];
        System.arraycopy(md.digest(), 0, iv, 0, 16);
        return iv;
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b) s.append(String.format("%02x", x & 0xff));
        return s.toString();
    }

    // a chave fica guardada no app (files/chave): as atualizações de dados das releases são cifradas com ela
    static void guardarChave(Context c, byte[] K) throws Exception {
        try (FileOutputStream o = new FileOutputStream(new File(c.getFilesDir(), "chave"))) { o.write(K); }
    }

    static byte[] chaveGuardada(Context c) {
        File f = new File(c.getFilesDir(), "chave");
        if (f.length() != 32) return null;
        byte[] k = new byte[32];
        try (FileInputStream in = new FileInputStream(f)) {
            return in.read(k) == 32 ? k : null;
        } catch (Exception e) {
            return null;
        }
    }

    interface Cancelar {
        void conferir() throws Erro;
    }

    // ------------------------------------------------------------------ pacote (manifesto + partes)
    static final class Pacote {
        JSONObject cab, man;
        File dir;
        String grupo; // prefixo dos arquivos na release ("dados", "opcional-texturas_hd", "atualizacao")
    }

    static Pacote abrir(File dir, String grupo, byte[] K) throws Exception {
        File mf = arquivo(dir, grupo, "manifesto.bin");
        byte[] b = ler(mf);
        if (b.length < 10 || !new String(b, 0, 6, StandardCharsets.US_ASCII).equals("VTPK1\n")) throw new Erro(Tr.t("manifesto.bin ruim"));
        int n = (b[6] & 0xff) | (b[7] & 0xff) << 8 | (b[8] & 0xff) << 16 | (b[9] & 0xff) << 24;
        Pacote p = new Pacote();
        p.dir = dir;
        p.grupo = grupo;
        p.cab = new JSONObject(new String(b, 10, n, StandardCharsets.UTF_8));
        if (!verificaChave(K).equals(p.cab.optString("verifica_chave")))
            throw new Erro(Tr.t("A chave não confere: o PKG ou o work.bin não são os do jogo americano"));
        Cipher c = ctr(K, ivDe(K, "manifesto"));
        byte[] z = c.doFinal(b, 10 + n, b.length - 10 - n);
        p.man = new JSONObject(new String(inflar(z), StandardCharsets.UTF_8));
        return p;
    }

    static File arquivo(File dir, String grupo, String nome) {
        File f = new File(dir, grupo + "__" + nome); // como na release: <grupo>__<arquivo>
        return f.isFile() ? f : new File(dir, nome);
    }

    static Cipher ctr(byte[] K, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(K, "AES"), new IvParameterSpec(iv));
        return c;
    }

    static byte[] inflar(byte[] z) throws Exception {
        Inflater inf = new Inflater(true);
        inf.setInput(z);
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(z.length * 4);
        byte[] buf = new byte[1 << 16];
        while (!inf.finished()) {
            int n = inf.inflate(buf);
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
            o.write(buf, 0, n);
        }
        inf.end();
        return o.toByteArray();
    }

    static byte[] ler(File f) throws Exception {
        byte[] b = new byte[(int) f.length()];
        try (FileInputStream in = new FileInputStream(f)) {
            int o = 0, n;
            while (o < b.length && (n = in.read(b, o, b.length - o)) > 0) o += n;
        }
        return b;
    }

    // lê o fluxo lógico (as partes gow-NN.bin, uma depois da outra)
    static final class Leitor implements AutoCloseable {
        final RandomAccessFile[] partes;
        final long[] inicio;

        Leitor(Pacote p) throws Exception {
            JSONArray t = p.man.getJSONArray("partes");
            partes = new RandomAccessFile[t.length()];
            inicio = new long[t.length() + 1];
            for (int i = 0; i < t.length(); i++) {
                File f = arquivo(p.dir, p.grupo, String.format("gow-%02d.bin", i + 1));
                if (f.length() != t.getLong(i)) throw new Erro(Tr.t("Parte incompleta: ") + f.getName());
                partes[i] = new RandomAccessFile(f, "r");
                inicio[i + 1] = inicio[i] + t.getLong(i);
            }
        }

        int ler(long pos, byte[] buf, int n) throws Exception {
            int feito = 0;
            while (feito < n) {
                int i = 0;
                while (i + 1 < inicio.length && pos >= inicio[i + 1]) i++;
                if (i >= partes.length) break;
                RandomAccessFile f = partes[i];
                f.seek(pos - inicio[i]);
                int k = f.read(buf, feito, (int) Math.min(n - feito, inicio[i + 1] - pos));
                if (k <= 0) break;
                feito += k;
                pos += k;
            }
            return feito;
        }

        @Override
        public void close() {
            for (RandomAccessFile f : partes) try { f.close(); } catch (Exception ignorada) { /* nada */ }
        }
    }

    // decifra e grava cada arquivo do pacote em destino, conferindo o SHA-256 de cada um
    static void instalar(Pacote p, byte[] K, File destino, String etapa, Aviso av, Cancelar c) throws Exception {
        JSONArray arqs = p.man.getJSONArray("arquivos");
        long total = p.man.optLong("original", 0), feito = 0;
        byte[] buf = new byte[4 << 20], out = new byte[8 << 20];
        try (Leitor L = new Leitor(p)) {
            for (int i = 0; i < arqs.length(); i++) {
                c.conferir();
                JSONObject a = arqs.getJSONObject(i);
                String rel = a.getString("p");
                File alvo = new File(destino, rel);
                //noinspection ResultOfMethodCallIgnored
                alvo.getParentFile().mkdirs();
                Cipher dec = ctr(K, ivDe(K, rel));
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                Inflater inf = a.optInt("c") == 1 ? new Inflater(true) : null;
                long z = a.getLong("z"), o = a.getLong("o");
                try (OutputStream os = new FileOutputStream(alvo)) {
                    for (long pos = 0; pos < z; ) {
                        int n = L.ler(o + pos, buf, (int) Math.min(buf.length, z - pos));
                        if (n <= 0) throw new Erro(Tr.t("Pacote cortado em ") + rel);
                        byte[] claro = dec.update(buf, 0, n);
                        pos += n;
                        if (inf == null) {
                            md.update(claro);
                            os.write(claro);
                            feito += claro.length;
                        } else {
                            inf.setInput(claro);
                            int k;
                            while ((k = inf.inflate(out)) > 0) {
                                md.update(out, 0, k);
                                os.write(out, 0, k);
                                feito += k;
                            }
                        }
                        av.andou(etapa, feito, total);
                    }
                }
                if (inf != null) inf.end();
                if (!hex(md.digest()).equals(a.getString("h"))) throw new Erro(Tr.t("Arquivo corrompido: ") + rel);
                //noinspection ResultOfMethodCallIgnored
                alvo.setLastModified(a.optLong("m", System.currentTimeMillis()));
            }
        }
        JSONArray vaz = p.man.optJSONArray("vazias");
        if (vaz != null) for (int i = 0; i < vaz.length(); i++) //noinspection ResultOfMethodCallIgnored
            new File(destino, vaz.getString(i)).mkdirs();
    }

    // ------------------------------------------------------------------ app do Vita (PKG + work.bin)
    static void extrairApp(Context ctx, File pkg, File rif, File destinoGame, Aviso av, Cancelar c) throws Exception {
        //noinspection ResultOfMethodCallIgnored
        destinoGame.mkdirs();
        String exe = ctx.getApplicationInfo().nativeLibraryDir + "/libpfs_extrair.so";
        Process pr = new ProcessBuilder(exe, pkg.getPath(), rif.getPath(), destinoGame.getPath()).redirectErrorStream(true).start();
        String etapa = Tr.t("Tirando o jogo do PKG"), erro = null, pronto = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8))) {
            String l;
            while ((l = r.readLine()) != null) {
                try {
                    c.conferir();
                } catch (Erro e) {
                    pr.destroy();
                    throw e;
                }
                if (l.startsWith("etapa ")) etapa = l.endsWith("pfs") ? Tr.t("Decifrando o jogo (PFS)") : Tr.t("Tirando o jogo do PKG");
                else if (l.startsWith("progresso ")) {
                    String[] v = l.substring(10).trim().split(" ");
                    if (v.length == 2) av.andou(etapa, Long.parseLong(v[0]), Long.parseLong(v[1]));
                } else if (l.startsWith("pronto ")) pronto = l.substring(7);
                else if (l.startsWith("erro ")) erro = l.substring(5);
            }
        }
        int code = pr.waitFor();
        if (code != 0 || pronto == null) throw new Erro(Tr.t("Não deu para tirar o jogo do PKG: ") + (erro != null ? erro : "código " + code));
    }

    // ------------------------------------------------------------------ releases do GitHub
    static final class Asset {
        String nome, url;
        long tamanho;
    }

    static final class Release {
        String tag, notas;
        List<Asset> assets = new ArrayList<>();
    }

    static Release ultimaRelease() throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO_APP + "/releases/latest").openConnection();
        h.setRequestProperty("Accept", "application/vnd.github+json");
        h.setRequestProperty("User-Agent", "GowCollection-android");
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        if (h.getResponseCode() == 404) return null; // ainda sem release
        if (h.getResponseCode() != 200) throw new Erro("GitHub: " + h.getResponseCode());
        JSONObject j;
        try (InputStream in = h.getInputStream()) {
            j = new JSONObject(new String(lerTudo(in), StandardCharsets.UTF_8));
        }
        return release(j);
    }

    // A release mais nova que tem os pacotes do grupo ("dados", "opcional-texturas_hd", "atualizacao"). Uma release
    // só com o APK novo não leva os 3,8 GB de dados outra vez: o instalador e a atualização buscam cada grupo na mais
    // nova que o tenha (e quem pula versões ainda recebe o pacote de atualização da release de antes).
    static Release releaseComGrupo(String grupo) throws Exception {
        HttpURLConnection h = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases?per_page=20").openConnection();
        h.setRequestProperty("Accept", "application/vnd.github+json");
        h.setRequestProperty("User-Agent", "GowCollection-android");
        h.setConnectTimeout(15000);
        h.setReadTimeout(30000);
        if (h.getResponseCode() == 404) return null;
        if (h.getResponseCode() != 200) throw new Erro("GitHub: " + h.getResponseCode());
        JSONArray rs;
        try (InputStream in = h.getInputStream()) {
            rs = new JSONArray(new String(lerTudo(in), StandardCharsets.UTF_8));
        }
        for (int i = 0; i < rs.length(); i++) { // a API devolve da mais nova para a mais antiga
            JSONObject j = rs.getJSONObject(i);
            if (j.optBoolean("draft") || j.optBoolean("prerelease")) continue;
            Release r = release(j);
            for (Asset a : r.assets) if (a.nome.startsWith(grupo + "__")) return r;
        }
        return null;
    }

    private static Release release(JSONObject j) throws Exception {
        Release r = new Release();
        r.tag = j.optString("tag_name");
        r.notas = j.optString("body");
        JSONArray as = j.optJSONArray("assets");
        if (as != null)
            for (int i = 0; i < as.length(); i++) {
                JSONObject a = as.getJSONObject(i);
                Asset x = new Asset();
                x.nome = a.optString("name");
                x.url = a.optString("browser_download_url");
                x.tamanho = a.optLong("size");
                r.assets.add(x);
            }
        return r;
    }

    static byte[] lerTudo(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        byte[] b = new byte[1 << 16];
        int n;
        while ((n = in.read(b)) > 0) o.write(b, 0, n);
        return o.toByteArray();
    }

    // baixa os arquivos do grupo ("dados__manifesto.bin", "dados__gow-01.bin"...) para dir, continuando de onde parou
    static void baixarGrupo(Release r, String grupo, File dir, Aviso av, Cancelar c) throws Exception {
        List<Asset> l = new ArrayList<>();
        long total = 0;
        for (Asset a : r.assets) if (a.nome.startsWith(grupo + "__")) { l.add(a); total += a.tamanho; }
        if (l.isEmpty()) throw new Erro(Tr.t("A release não tem o pacote ") + grupo);
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        long feito = 0;
        for (Asset a : l) {
            File f = new File(dir, a.nome);
            feito += baixar(a.url, f, a.tamanho, feito, total, Tr.t("Baixando os dados do jogo"), av, c);
        }
    }

    // devolve quantos bytes o arquivo tem no fim
    static long baixar(String url, File f, long tamanho, long antes, long total, String etapa, Aviso av, Cancelar c) throws Exception {
        long tem = f.isFile() ? f.length() : 0;
        if (tamanho > 0 && tem == tamanho) { av.andou(etapa, antes + tem, total); return tem; }
        if (tamanho > 0 && tem > tamanho) { //noinspection ResultOfMethodCallIgnored
            f.delete(); tem = 0; }
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setRequestProperty("User-Agent", "GowCollection-android");
        h.setInstanceFollowRedirects(true);
        h.setConnectTimeout(20000);
        h.setReadTimeout(60000);
        if (tem > 0) h.setRequestProperty("Range", "bytes=" + tem + "-");
        int code = h.getResponseCode();
        boolean continua = code == 206;
        if (code != 200 && code != 206) throw new Erro(Tr.t("Download falhou: ") + code + " " + f.getName());
        if (!continua) tem = 0;
        try (InputStream in = h.getInputStream(); FileOutputStream o = new FileOutputStream(f, continua)) {
            byte[] b = new byte[1 << 20];
            int n;
            while ((n = in.read(b)) > 0) {
                c.conferir();
                o.write(b, 0, n);
                tem += n;
                av.andou(etapa, antes + tem, total);
            }
        }
        if (tamanho > 0 && tem != tamanho) throw new Erro(Tr.t("Download incompleto: ") + f.getName());
        return tem;
    }

    private Instalador() {}
}
