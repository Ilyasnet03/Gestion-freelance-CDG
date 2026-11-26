package com.cdg.freelance.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.core.io.ClassPathResource;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/** En-tête avec logo, pied de page numéroté et palette communes aux documents PDF. */
final class PdfBranding extends PdfPageEventHelper {

    static final Color NAVY = new Color(18, 51, 84);
    static final Color TEAL = new Color(0, 145, 154);
    static final Color LIGHT = new Color(238, 246, 248);
    static final Color BORDER = new Color(218, 228, 232);
    static final Color GREY = new Color(102, 112, 123);
    static final Color GREEN = new Color(22, 122, 91);
    static final Color ORANGE = new Color(217, 119, 6);
    static final Color RED = new Color(197, 48, 48);

    static final float MARGIN_TOP = 110;
    static final float MARGIN_BOTTOM = 56;

    private final String documentLabel;
    private final Image logo;
    private BaseFont font;
    private BaseFont boldFont;

    PdfBranding(String documentLabel) {
        this.documentLabel = documentLabel;
        this.logo = loadLogo();
        try {
            this.font = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, false);
            this.boldFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false);
        } catch (Exception exception) {
            throw new IllegalStateException("Polices PDF indisponibles", exception);
        }
    }

    private static Image loadLogo() {
        try (InputStream stream = new ClassPathResource("logo-cdg.png").getInputStream()) {
            Image image = Image.getInstance(stream.readAllBytes());
            image.scaleToFit(92, 52);
            return image;
        } catch (Exception exception) {
            return null;
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        Rectangle page = document.getPageSize();
        PdfContentByte canvas = writer.getDirectContent();
        float left = document.leftMargin();
        float right = page.getWidth() - document.rightMargin();

        canvas.saveState();
        canvas.setColorFill(NAVY);
        canvas.rectangle(0, page.getHeight() - 10, page.getWidth(), 10);
        canvas.fill();
        canvas.setColorFill(TEAL);
        canvas.rectangle(0, page.getHeight() - 14, page.getWidth(), 4);
        canvas.fill();
        canvas.restoreState();

        if (logo != null) {
            try {
                logo.setAbsolutePosition(left, page.getHeight() - 24 - logo.getScaledHeight());
                canvas.addImage(logo);
            } catch (Exception ignored) {
                // Le document reste lisible sans logo.
            }
        }

        canvas.beginText();
        canvas.setColorFill(NAVY);
        canvas.setFontAndSize(boldFont, 9);
        canvas.showTextAligned(Element.ALIGN_RIGHT, "GROUPE CDG", right, page.getHeight() - 42, 0);
        canvas.setColorFill(GREY);
        canvas.setFontAndSize(font, 8);
        canvas.showTextAligned(Element.ALIGN_RIGHT, documentLabel, right, page.getHeight() - 54, 0);
        canvas.endText();

        canvas.saveState();
        canvas.setColorStroke(BORDER);
        canvas.setLineWidth(0.8f);
        canvas.moveTo(left, 44);
        canvas.lineTo(right, 44);
        canvas.stroke();
        canvas.restoreState();

        canvas.beginText();
        canvas.setColorFill(GREY);
        canvas.setFontAndSize(font, 8);
        canvas.showTextAligned(Element.ALIGN_LEFT,
                "Caisse de Dépôt et de Gestion · Plateforme Gestion des Freelances", left, 30, 0);
        canvas.showTextAligned(Element.ALIGN_RIGHT, "Page " + writer.getPageNumber(), right, 30, 0);
        canvas.endText();
    }
}
