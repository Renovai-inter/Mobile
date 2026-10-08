package com.example.renovai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.core.content.FileProvider;

import com.example.renovai.dto.response.ColetaResponse;
import com.example.renovai.dto.response.GestorResponses;
import com.example.renovai.dto.response.TriagemResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Relatórios do Gestor. A API não tem endpoint de relatórios, então eles são gerados AQUI (PDF
 * nativo, android.graphics.pdf) a partir de dados reais (negociações, triagens, coletas) e
 * guardados no armazenamento do app; a lista fica em SharedPreferences.
 */
public final class GestorRelatorios {

    public static final String NEGOCIACOES = "NEGOCIACOES",
            IMPACTO = "IMPACTO",
            TRIAGEM = "TRIAGEM";

    public static class Meta {
        public String id, nome, tipo, inicio, fim, arquivo, criadoEm;
    }

    private static final Gson GSON = new Gson();
    private static final Map<String, Bitmap> MINIATURAS = new HashMap<>();

    private GestorRelatorios() {}

    // ── armazenamento ──
    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext()
                .getSharedPreferences(
                        "renovai_relatorios_" + String.valueOf(CooperadoSession.getCooperativaId()),
                        Context.MODE_PRIVATE);
    }

    public static List<Meta> listar(Context c) {
        String json = prefs(c).getString("lista", "[]");
        List<Meta> l = GSON.fromJson(json, new TypeToken<List<Meta>>() {}.getType());
        List<Meta> ok = new ArrayList<>();
        if (l != null)
            for (Meta m : l) if (m.arquivo != null && new File(m.arquivo).exists()) ok.add(m);
        ok.sort((a, b) -> String.valueOf(b.criadoEm).compareTo(String.valueOf(a.criadoEm)));
        return ok;
    }

    private static void guardar(Context c, Meta m) {
        List<Meta> l = listar(c);
        l.add(m);
        prefs(c).edit().putString("lista", GSON.toJson(l)).apply();
    }

    public static void excluir(Context c, Meta m) {
        List<Meta> l = listar(c);
        l.removeIf(x -> x.id.equals(m.id));
        prefs(c).edit().putString("lista", GSON.toJson(l)).apply();
        new File(m.arquivo).delete();
        MINIATURAS.remove(m.arquivo);
    }

    public static void abrir(Activity a, Meta m) {
        Uri uri =
                FileProvider.getUriForFile(
                        a, a.getPackageName() + ".fileprovider", new File(m.arquivo));
        Intent i =
                new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/pdf")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(i);
        } catch (Exception e) {
            compartilhar(a, m);
        }
    }

    public static void compartilhar(Activity a, Meta m) {
        Uri uri =
                FileProvider.getUriForFile(
                        a, a.getPackageName() + ".fileprovider", new File(m.arquivo));
        Intent i =
                new Intent(Intent.ACTION_SEND)
                        .setType("application/pdf")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(Intent.createChooser(i, "Compartilhar relatório"));
    }

    public static Bitmap miniatura(Meta m) {
        Bitmap b = MINIATURAS.get(m.arquivo);
        if (b != null) return b;
        try (ParcelFileDescriptor fd =
                        ParcelFileDescriptor.open(
                                new File(m.arquivo), ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer r = new PdfRenderer(fd);
                PdfRenderer.Page p = r.openPage(0)) {
            b = Bitmap.createBitmap(220, 311, Bitmap.Config.ARGB_8888);
            b.eraseColor(Color.WHITE);
            p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            MINIATURAS.put(m.arquivo, b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    // ── PDF ──
    private static class Pdf {
        final PdfDocument doc = new PdfDocument();
        PdfDocument.Page pagina;
        Canvas canvas;
        float y;
        int n = 0;
        final Paint normal = new Paint(Paint.ANTI_ALIAS_FLAG),
                negrito = new Paint(Paint.ANTI_ALIAS_FLAG),
                grande = new Paint(Paint.ANTI_ALIAS_FLAG),
                cinza = new Paint(Paint.ANTI_ALIAS_FLAG),
                linha = new Paint();

        Pdf() {
            normal.setTextSize(11);
            normal.setColor(Color.parseColor("#222222"));
            negrito.setTextSize(12);
            negrito.setTypeface(Typeface.DEFAULT_BOLD);
            negrito.setColor(Color.parseColor("#882B4E"));
            grande.setTextSize(20);
            grande.setTypeface(Typeface.DEFAULT_BOLD);
            grande.setColor(Color.parseColor("#519059"));
            cinza.setTextSize(10);
            cinza.setColor(Color.parseColor("#6B7280"));
            linha.setColor(Color.parseColor("#DDDDDD"));
            linha.setStrokeWidth(1);
            nova();
        }

        void nova() {
            if (pagina != null) doc.finishPage(pagina);
            pagina = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, ++n).create());
            canvas = pagina.getCanvas();
            y = 50;
        }

        void garantir(float h) {
            if (y + h > 800) nova();
        }

        void titulo(String t) {
            garantir(30);
            canvas.drawText(t, 40, y, grande);
            y += 28;
        }

        void secao(String t) {
            garantir(26);
            y += 8;
            canvas.drawText(t, 40, y, negrito);
            y += 6;
            canvas.drawLine(40, y, 555, y, linha);
            y += 16;
        }

        void texto(String t) {
            garantir(16);
            canvas.drawText(cortar(t, normal, 515), 40, y, normal);
            y += 15;
        }

        void nota(String t) {
            garantir(14);
            canvas.drawText(cortar(t, cinza, 515), 40, y, cinza);
            y += 13;
        }

        void colunas(String a, String b, String c, String d, boolean cab) {
            garantir(16);
            Paint p = cab ? negrito : normal;
            p.setTextSize(cab ? 10 : 10);
            canvas.drawText(cortar(a, p, 190), 40, y, p);
            canvas.drawText(cortar(b, p, 100), 240, y, p);
            canvas.drawText(cortar(c, p, 100), 345, y, p);
            canvas.drawText(cortar(d, p, 100), 450, y, p);
            p.setTextSize(cab ? 12 : 11);
            y += 15;
            if (cab) {
                canvas.drawLine(40, y - 10, 555, y - 10, linha);
            }
        }

        String cortar(String s, Paint p, float larg) {
            if (s == null) return "";
            if (p.measureText(s) <= larg) return s;
            while (s.length() > 1 && p.measureText(s + "…") > larg)
                s = s.substring(0, s.length() - 1);
            return s + "…";
        }

        File salvar(Context c, String prefixo) throws Exception {
            doc.finishPage(pagina);
            File dir = new File(c.getFilesDir(), "relatorios");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, prefixo + "_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream os = new FileOutputStream(f)) {
                doc.writeTo(os);
            }
            doc.close();
            return f;
        }
    }

    private static Meta registrar(
            Context c, File f, String nome, String tipo, LocalDate ini, LocalDate fim) {
        Meta m = new Meta();
        m.id = UUID.randomUUID().toString();
        m.nome = nome;
        m.tipo = tipo;
        m.arquivo = f.getAbsolutePath();
        m.inicio = ini.toString();
        m.fim = fim.toString();
        m.criadoEm = LocalDateTime.now().toString();
        guardar(c, m);
        return m;
    }

    private static String periodo(LocalDate i, LocalDate f) {
        java.time.format.DateTimeFormatter d =
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return i.format(d) + " - " + f.format(d);
    }

    private static boolean dentro(String iso, LocalDate ini, LocalDate fim) {
        LocalDateTime d = GestorUi.parse(iso);
        if (d == null) return false;
        LocalDate x = d.toLocalDate();
        return !x.isBefore(ini) && !x.isAfter(fim);
    }

    public static Meta gerarNegociacoes(
            Context c,
            String nome,
            LocalDate ini,
            LocalDate fim,
            List<GestorResponses.Negociacao> todas)
            throws Exception {
        List<GestorResponses.Negociacao> l = new ArrayList<>();
        for (GestorResponses.Negociacao n : todas) if (dentro(n.dataInicio, ini, fim)) l.add(n);
        int conc = 0, rec = 0, emNeg = 0;
        double valor = 0, peso = 0;
        for (GestorResponses.Negociacao n : l) {
            int e = GestorUi.estadoNeg(n);
            if (e == GestorUi.CONCLUIDO) {
                conc++;
                valor += n.valorTotal == null ? 0 : n.valorTotal;
                peso += GestorUi.pesoNegociacao(n);
            } else if (e == GestorUi.RECUSADO) rec++;
            else emNeg++;
        }
        double taxa = l.isEmpty() ? 0 : conc * 100.0 / l.size();

        Pdf p = new Pdf();
        p.titulo(nome);
        p.texto("Cooperativa: " + nullSafe(CooperadoSession.getCooperativaNome()));
        p.texto("Período: " + periodo(ini, fim));
        p.nota(
                "Gerado em "
                        + LocalDateTime.now()
                                .format(
                                        java.time.format.DateTimeFormatter.ofPattern(
                                                "dd/MM/yyyy HH:mm"))
                        + " • Relatório de Negociações");
        p.secao("Resumo — conversão de pedidos");
        p.texto("Pedidos/negociações no período: " + l.size());
        p.texto("Concluídas: " + conc + "   Recusadas: " + rec + "   Em negociação: " + emNeg);
        p.texto(
                "Taxa de conversão: "
                        + String.format(GestorUi.BR, "%.1f%%", taxa)
                        + "  (concluídas ÷ total)");
        p.texto("Total arrecadado (concluídas): " + GestorUi.dinheiro(valor));
        p.texto("Peso negociado (concluídas): " + GestorUi.kg(peso));
        p.secao("Negociações do período");
        p.colunas("Empresa", "Data", "Status", "Valor", true);
        for (GestorResponses.Negociacao n : l) {
            p.colunas(
                    nullSafe(n.empresaNome),
                    GestorUi.data(n.dataInicio),
                    GestorUi.rotuloNeg(GestorUi.estadoNeg(n)),
                    GestorUi.dinheiro(n.valorTotal),
                    false);
        }
        if (l.isEmpty()) p.nota("Nenhuma negociação encontrada no período.");
        return registrar(c, p.salvar(c, "negociacoes"), nome, NEGOCIACOES, ini, fim);
    }

    /**
     * Fatores médios de referência por kg reciclado: {CO2 kg, água L, energia kWh}. São
     * ESTIMATIVAS.
     */
    private static double[] fator(String material) {
        if (GestorUi.tem(material, "PAPEL", "PAPELAO", "CARTAO"))
            return new double[] {0.9, 25, 3.5};
        if (GestorUi.tem(material, "PLASTIC", "PET")) return new double[] {1.5, 10, 5.0};
        if (GestorUi.tem(material, "METAL", "ALUMIN", "LATA", "FERRO", "ACO"))
            return new double[] {8.0, 40, 12.0};
        if (GestorUi.tem(material, "VIDRO")) return new double[] {0.3, 2, 0.5};
        return new double[] {0.5, 5, 1.0};
    }

    public static Meta gerarImpacto(
            Context c,
            String nome,
            LocalDate ini,
            LocalDate fim,
            List<TriagemResponse> triagens,
            List<ColetaResponse> coletas)
            throws Exception {
        Map<String, Double> porMaterial = new LinkedHashMap<>();
        for (TriagemResponse t : triagens) {
            if (!dentro(t.getDataTriagem(), ini, fim) || t.getQuantidadeKg() == null) continue;
            double liquido =
                    t.getQuantidadeKg().doubleValue()
                            - (t.getQuantidadeRejeitoKg() == null
                                    ? 0
                                    : t.getQuantidadeRejeitoKg().doubleValue());
            if (liquido <= 0) continue;
            String mat = t.getMaterialCategoria() == null ? "Outros" : t.getMaterialCategoria();
            porMaterial.merge(mat, liquido, Double::sum);
        }
        boolean viaColetas = false;
        if (porMaterial.isEmpty()) {
            double kgColetas = 0;
            for (ColetaResponse col : coletas)
                if (dentro(col.getDataColeta(), ini, fim) && col.getQuantidadeKg() != null)
                    kgColetas += col.getQuantidadeKg().doubleValue();
            if (kgColetas > 0) {
                porMaterial.put("Coletado (sem triagem)", kgColetas);
                viaColetas = true;
            }
        }
        double tKg = 0, tCo2 = 0, tAgua = 0, tEn = 0;
        for (Map.Entry<String, Double> e : porMaterial.entrySet()) {
            double[] f = fator(e.getKey());
            tKg += e.getValue();
            tCo2 += e.getValue() * f[0];
            tAgua += e.getValue() * f[1];
            tEn += e.getValue() * f[2];
        }

        Pdf p = new Pdf();
        p.titulo(nome);
        p.texto("Cooperativa: " + nullSafe(CooperadoSession.getCooperativaNome()));
        p.texto("Período: " + periodo(ini, fim));
        p.nota(
                "Gerado em "
                        + LocalDateTime.now()
                                .format(
                                        java.time.format.DateTimeFormatter.ofPattern(
                                                "dd/MM/yyyy HH:mm"))
                        + " • Relatório de Impacto Ambiental");
        p.secao("Resumo — economia estimada");
        p.texto("Material reciclado no período: " + GestorUi.kg(tKg));
        p.texto("CO2 evitado: " + String.format(GestorUi.BR, "%,.1f kg", tCo2));
        p.texto("Água economizada: " + String.format(GestorUi.BR, "%,.0f litros", tAgua));
        p.texto("Energia economizada: " + String.format(GestorUi.BR, "%,.1f kWh", tEn));
        p.secao("Detalhe por material");
        p.colunas("Material", "Peso", "CO2 (kg)", "Água (L) / kWh", true);
        for (Map.Entry<String, Double> e : porMaterial.entrySet()) {
            double[] f = fator(e.getKey());
            p.colunas(
                    e.getKey(),
                    GestorUi.kg(e.getValue()),
                    String.format(GestorUi.BR, "%,.1f", e.getValue() * f[0]),
                    String.format(
                            GestorUi.BR, "%,.0f / %,.1f", e.getValue() * f[1], e.getValue() * f[2]),
                    false);
        }
        if (porMaterial.isEmpty()) p.nota("Nenhuma coleta ou triagem encontrada no período.");
        p.secao("Como foi calculado");
        p.nota(
                "Estimativa: peso líquido triado (descontado o rejeito) × fator médio de referência"
                        + " por kg reciclado.");
        p.nota(
                "Fatores usados (CO2 kg / água L / energia kWh por kg): papel 0,9/25/3,5 • plástico"
                        + " 1,5/10/5,0 •");
        p.nota(
                "metal 8,0/40/12,0 • vidro 0,3/2/0,5 • outros 0,5/5/1,0. São médias de literatura,"
                        + " não medições da cooperativa.");
        if (viaColetas)
            p.nota(
                    "Não havia triagens no período: foi usado o peso total das coletas, com o fator"
                            + " de \"outros\".");
        return registrar(c, p.salvar(c, "impacto"), nome, IMPACTO, ini, fim);
    }

    public static Meta gerarTriagem(
            Context c, String coletaId, List<TriagemResponse> linhas, String feitoPor)
            throws Exception {
        Pdf p = new Pdf();
        String nome = "Triagem " + GestorUi.idCurto(coletaId);
        p.titulo(nome);
        p.texto("Cooperativa: " + nullSafe(CooperadoSession.getCooperativaNome()));
        p.texto(
                "Coleta de origem: "
                        + GestorUi.idCurto(coletaId)
                        + (feitoPor != null ? "  (feita por " + feitoPor + ")" : ""));
        p.nota(
                "Gerado em "
                        + LocalDateTime.now()
                                .format(
                                        java.time.format.DateTimeFormatter.ofPattern(
                                                "dd/MM/yyyy HH:mm")));
        double total = 0, rej = 0;
        java.util.LinkedHashSet<String> equipe = new java.util.LinkedHashSet<>();
        for (TriagemResponse t : linhas) {
            total += com.example.renovai.controller.TriagemController.pesoReal(t).doubleValue();
            rej +=
                    t.getQuantidadeRejeitoKg() == null
                            ? 0
                            : t.getQuantidadeRejeitoKg().doubleValue();
            if (t.getCooperadosNomes() != null) equipe.addAll(t.getCooperadosNomes());
        }
        p.secao("Resumo");
        p.texto("Total triado: " + GestorUi.kg(total) + "   Rejeito: " + GestorUi.kg(rej));
        p.texto("Materiais: " + linhas.size() + "   Funcionários: " + equipe.size());
        p.secao("Funcionários participantes");
        for (String n : equipe) p.texto("• " + n);
        p.secao("Materiais separados");
        p.colunas("Material", "Peso", "Rejeito", "Status", true);
        for (TriagemResponse t : linhas) {
            p.colunas(
                    nullSafe(t.getMaterialCategoria()),
                    GestorUi.kg(
                            com.example.renovai.controller.TriagemController.pesoReal(t)
                                    .doubleValue()),
                    GestorUi.kg(
                            t.getQuantidadeRejeitoKg() == null
                                    ? 0d
                                    : t.getQuantidadeRejeitoKg().doubleValue()),
                    nullSafe(t.getStatusAtual()),
                    false);
        }
        LocalDate hoje = LocalDate.now();
        return registrar(c, p.salvar(c, "triagem"), nome, TRIAGEM, hoje, hoje);
    }

    private static String nullSafe(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }
}
