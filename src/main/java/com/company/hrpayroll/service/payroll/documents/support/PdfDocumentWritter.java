// service/payroll/documents/support/PdfDocumentWriter.java
package com.company.hrpayroll.service.payroll.documents.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Minimal PDFBox wrapper for simple one-page payroll documents — title,
 * label/value rows, dividers. Not a general-purpose PDF library, just
 * enough to avoid repeating low-level PDFBox calls three times over.
 */
public class PdfDocumentWriter implements AutoCloseable {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    private final PDDocument document = new PDDocument();
    private PDPageContentStream contentStream;
    private float cursorY;

    private final PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public PdfDocumentWriter() throws IOException {
        newPage();
    }

    private void newPage() throws IOException {
        if (contentStream != null) {
            contentStream.close();
        }
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        cursorY = PAGE_HEIGHT - MARGIN;
    }

    public void title(String text) throws IOException {
        writeLine(text, boldFont, 16f);
        cursorY -= 6f;
    }

    public void subtitle(String text) throws IOException {
        writeLine(text, regularFont, 11f);
    }

    public void spacer(float amount) {
        cursorY -= amount;
    }

    public void sectionHeading(String text) throws IOException {
        spacer(10f);
        writeLine(text, boldFont, 12f);
    }

    public void row(String label, String value) throws IOException {
        row(label, value, regularFont);
    }

    public void boldRow(String label, String value) throws IOException {
        row(label, value, boldFont);
    }

    private void row(String label, String value, PDFont font) throws IOException {
        ensureSpace(18f);
        contentStream.beginText();
        contentStream.setFont(font, 10.5f);
        contentStream.newLineAtOffset(MARGIN, cursorY);
        contentStream.showText(label);
        contentStream.endText();

        float valueWidth = font.getStringWidth(value) / 1000 * 10.5f;
        contentStream.beginText();
        contentStream.setFont(font, 10.5f);
        contentStream.newLineAtOffset(PAGE_WIDTH - MARGIN - valueWidth, cursorY);
        contentStream.showText(value);
        contentStream.endText();

        cursorY -= 16f;
    }

    public void divider() throws IOException {
        ensureSpace(10f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(MARGIN, cursorY);
        contentStream.lineTo(PAGE_WIDTH - MARGIN, cursorY);
        contentStream.stroke();
        cursorY -= 10f;
    }

    private void writeLine(String text, PDFont font, float fontSize) throws IOException {
        ensureSpace(fontSize + 6f);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(MARGIN, cursorY);
        contentStream.showText(text);
        contentStream.endText();
        cursorY -= (fontSize + 6f);
    }

    private void ensureSpace(float needed) throws IOException {
        if (cursorY - needed < MARGIN) {
            newPage();
        }
    }

    public byte[] build() throws IOException {
        contentStream.close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();
        return out.toByteArray();
    }

    @Override
    public void close() throws IOException {
        if (!document.getDocument().isClosed()) {
            document.close();
        }
    }
}