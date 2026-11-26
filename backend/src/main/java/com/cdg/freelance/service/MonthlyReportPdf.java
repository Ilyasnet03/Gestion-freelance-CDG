package com.cdg.freelance.service;

import com.cdg.freelance.dto.alerte.AlerteDto;
import com.cdg.freelance.dto.projet.ProjetDto;
import com.cdg.freelance.exception.BusinessException;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Rendu graphique du rapport mensuel de pilotage. */
final class MonthlyReportPdf {

    private static final Locale FR = Locale.FRANCE;

    private MonthlyReportPdf() {}

    static byte[] render(YearMonth month, String profile, List<ProjetDto> projects,
                         List<AlerteDto> alerts, BigDecimal totalBudget, BigDecimal consumedBudget) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40, 40, PdfBranding.MARGIN_TOP, PdfBranding.MARGIN_BOTTOM);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PdfBranding("Rapport mensuel de pilotage"));
            document.open();

            Font label = new Font(Font.HELVETICA, 8, Font.BOLD, PdfBranding.GREY);
            Font body = new Font(Font.HELVETICA, 9, Font.NORMAL, PdfBranding.NAVY);
            Font bold = new Font(Font.HELVETICA, 9, Font.BOLD, PdfBranding.NAVY);
            Font section = new Font(Font.HELVETICA, 12, Font.BOLD, PdfBranding.NAVY);
            Font white = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);

            document.add(banner(month, profile));
            document.add(kpis(projects, totalBudget, consumedBudget, alerts.size()));

            document.add(sectionTitle("Détail des projets", section));
            if (projects.isEmpty()) {
                document.add(new Paragraph("Aucun projet visible pour ce profil.", body));
            } else {
                document.add(projectTable(projects, label, body, bold, white));
            }

            document.add(sectionTitle("Alertes du mois", section));
            if (alerts.isEmpty()) {
                PdfPTable ok = new PdfPTable(1);
                ok.setWidthPercentage(100);
                PdfPCell cell = new PdfPCell(new Phrase("Aucune alerte émise durant cette période.",
                        new Font(Font.HELVETICA, 9, Font.BOLD, PdfBranding.GREEN)));
                cell.setBorderColor(PdfBranding.GREEN);
                cell.setBackgroundColor(new Color(231, 246, 240));
                cell.setPadding(10);
                ok.addCell(cell);
                document.add(ok);
            } else {
                document.add(alertTable(alerts, body, bold, white));
            }

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException("Impossible de générer le rapport PDF : " + exception.getMessage());
        }
    }

    private static PdfPTable banner(YearMonth month, String profile) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(PdfBranding.NAVY);
        cell.setPadding(16);
        String period = month.getMonth().getDisplayName(TextStyle.FULL, FR) + " " + month.getYear();
        cell.addElement(new Paragraph("RAPPORT MENSUEL DE PILOTAGE",
                new Font(Font.HELVETICA, 19, Font.BOLD, Color.WHITE)));
        cell.addElement(new Paragraph(capitalize(period) + "  ·  Profil " + profile
                + "  ·  Généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(190, 225, 228))));
        table.addCell(cell);
        return table;
    }

    private static PdfPTable kpis(List<ProjetDto> projects, BigDecimal total, BigDecimal consumed, int alertCount) {
        int rate = total.signum() > 0
                ? consumed.multiply(BigDecimal.valueOf(100)).divide(total, 0, java.math.RoundingMode.HALF_UP).intValue()
                : 0;
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        addKpi(table, "PROJETS", String.valueOf(projects.size()), PdfBranding.NAVY);
        addKpi(table, "BUDGET TOTAL", money(total), PdfBranding.TEAL);
        addKpi(table, "BUDGET CONSOMMÉ", money(consumed) + "  (" + rate + " %)",
                rate > 100 ? PdfBranding.RED : PdfBranding.GREEN);
        addKpi(table, "ALERTES", String.valueOf(alertCount),
                alertCount > 0 ? PdfBranding.ORANGE : PdfBranding.GREEN);
        return table;
    }

    private static void addKpi(PdfPTable table, String name, String value, Color accent) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(PdfBranding.BORDER);
        cell.setBorderWidthTop(3);
        cell.setBorderColorTop(accent);
        cell.setBackgroundColor(PdfBranding.LIGHT);
        cell.setPadding(9);
        cell.addElement(new Paragraph(name, new Font(Font.HELVETICA, 7, Font.BOLD, PdfBranding.GREY)));
        Paragraph paragraph = new Paragraph(value, new Font(Font.HELVETICA, 11, Font.BOLD, accent));
        paragraph.setSpacingBefore(3);
        cell.addElement(paragraph);
        table.addCell(cell);
    }

    private static Paragraph sectionTitle(String text, Font font) {
        Paragraph paragraph = new Paragraph(text.toUpperCase(FR), font);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    private static PdfPTable projectTable(List<ProjetDto> projects, Font label, Font body, Font bold, Font white)
            throws Exception {
        PdfPTable table = new PdfPTable(new float[]{2.9f, 1.2f, 1.9f, 2.2f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : List.of("PROJET", "STATUT", "AVANCEMENT", "BUDGET CONSOMMÉ")) {
            table.addCell(headerCell(header, white));
        }
        boolean zebra = false;
        for (ProjetDto project : projects) {
            Color background = zebra ? PdfBranding.LIGHT : Color.WHITE;
            zebra = !zebra;

            PdfPCell name = cell(background);
            name.addElement(new Paragraph(nullToDash(project.getNom()), bold));
            name.addElement(new Paragraph(nullToDash(project.getCode()), label));
            table.addCell(name);

            PdfPCell status = cell(background);
            status.addElement(new Paragraph(status(project.getStatut()), body));
            table.addCell(status);

            int progress = Math.max(0, Math.min(100, project.getAvancementGlobal() == null ? 0 : project.getAvancementGlobal()));
            PdfPCell progressCell = cell(background);
            progressCell.addElement(new Paragraph(progress + " %", bold));
            progressCell.addElement(bar(progress, PdfBranding.TEAL));
            table.addCell(progressCell);

            BigDecimal budget = project.getBudget() == null ? BigDecimal.ZERO : project.getBudget();
            BigDecimal consumed = project.getBudgetConsomme() == null ? BigDecimal.ZERO : project.getBudgetConsomme();
            int usage = budget.signum() > 0
                    ? consumed.multiply(BigDecimal.valueOf(100)).divide(budget, 0, java.math.RoundingMode.HALF_UP).intValue()
                    : 0;
            PdfPCell budgetCell = cell(background);
            budgetCell.addElement(new Paragraph(money(consumed) + " / " + money(budget), body));
            budgetCell.addElement(bar(Math.min(usage, 100),
                    usage > 100 ? PdfBranding.RED : usage > 85 ? PdfBranding.ORANGE : PdfBranding.GREEN));
            table.addCell(budgetCell);
        }
        return table;
    }

    private static PdfPTable alertTable(List<AlerteDto> alerts, Font body, Font bold, Font white) {
        PdfPTable table = new PdfPTable(new float[]{1.2f, 1.6f, 5f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : List.of("NIVEAU", "PROJET", "MESSAGE")) {
            table.addCell(headerCell(header, white));
        }
        for (AlerteDto alert : alerts) {
            String level = alert.getNiveau() == null ? "INFO" : alert.getNiveau().name();
            Color color = level.equals("CRITIQUE") ? PdfBranding.RED
                    : level.equals("ATTENTION") ? PdfBranding.ORANGE : PdfBranding.TEAL;
            PdfPCell badge = cell(Color.WHITE);
            badge.addElement(new Paragraph(level, new Font(Font.HELVETICA, 8, Font.BOLD, color)));
            table.addCell(badge);
            PdfPCell project = cell(Color.WHITE);
            project.addElement(new Paragraph(nullToDash(alert.getProjetCode()), bold));
            table.addCell(project);
            PdfPCell message = cell(Color.WHITE);
            message.addElement(new Paragraph(nullToDash(alert.getMessage()), body));
            table.addCell(message);
        }
        return table;
    }

    private static PdfPTable bar(int percent, Color color) {
        PdfPTable bar = new PdfPTable(percent <= 0 || percent >= 100 ? 1 : 2);
        bar.setWidthPercentage(100);
        bar.setSpacingBefore(4);
        try {
            if (percent > 0 && percent < 100) {
                bar.setWidths(new float[]{percent, 100 - percent});
            }
        } catch (Exception ignored) {
            // largeurs par défaut
        }
        if (percent > 0) bar.addCell(barCell(color));
        if (percent < 100) bar.addCell(barCell(PdfBranding.BORDER));
        return bar;
    }

    private static PdfPCell barCell(Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(" ", new Font(Font.HELVETICA, 3)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(color);
        cell.setFixedHeight(5);
        return cell;
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PdfBranding.NAVY);
        cell.setBorderColor(PdfBranding.NAVY);
        cell.setPadding(7);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private static PdfPCell cell(Color background) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(PdfBranding.BORDER);
        cell.setBackgroundColor(background);
        cell.setPadding(7);
        return cell;
    }

    private static String status(Object value) {
        if (value == null) return "—";
        String text = value.toString().replace('_', ' ').toLowerCase(FR);
        return capitalize(text);
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getIntegerInstance(FR);
        return format.format(value == null ? BigDecimal.ZERO : value).replace('\u202f', ' ').replace('\u00a0', ' ') + " MAD";
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : value.substring(0, 1).toUpperCase(FR) + value.substring(1);
    }
}
