// service/common/pdf/PdfDocumentWriter.java
package com.company.hrpayroll.service.common.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Minimal PDFBox wrapper for simple documents — logo, title,
 * label/value rows, tables, dividers. Not a general-purpose PDF library, just
 * enough to avoid repeating low-level PDFBox calls three times over.
 */
public class PdfDocumentWriter implements AutoCloseable {

    private static final float MARGIN = 50f;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LOGO_MAX_WIDTH = 140f;
    private static final float LOGO_MAX_HEIGHT = 56f;
    private static final float TABLE_FONT_SIZE = 8.5f;

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

    // The company logo, scaled to fit without stretching. A file PDFBox cannot read is skipped rather
    // than costing the user their document.
    public void logo(byte[] image) throws IOException {
        PDImageXObject picture;
        try {
            picture = PDImageXObject.createFromByteArray(document, image, "logo");
        } catch (IOException | IllegalArgumentException e) {
            return;
        }
        float scale = Math.min(LOGO_MAX_WIDTH / picture.getWidth(), LOGO_MAX_HEIGHT / picture.getHeight());
        float height = picture.getHeight() * scale;
        ensureSpace(height + 8f);
        cursorY -= height;
        contentStream.drawImage(picture, MARGIN, cursorY, picture.getWidth() * scale, height);
        cursorY -= 12f;
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
        contentStream.showText(printable(label, font));
        contentStream.endText();

        String printableValue = printable(value, font);
        float valueWidth = font.getStringWidth(printableValue) / 1000 * 10.5f;
        contentStream.beginText();
        contentStream.setFont(font, 10.5f);
        contentStream.newLineAtOffset(PAGE_WIDTH - MARGIN - valueWidth, cursorY);
        contentStream.showText(printableValue);
        contentStream.endText();

        cursorY -= 16f;
    }

    // One row of a table. Text too wide for its column is cut short rather than running into the next.
    public void tableRow(float[] columnWidths, List<String> cells, boolean bold) throws IOException {
        PDFont font = bold ? boldFont : regularFont;
        ensureSpace(14f);

        float x = MARGIN;
        for (int index = 0; index < columnWidths.length; index++) {
            String text = fit(printable(cells.get(index), font), font, columnWidths[index] - 4f);
            contentStream.beginText();
            contentStream.setFont(font, TABLE_FONT_SIZE);
            contentStream.newLineAtOffset(x, cursorY);
            contentStream.showText(text);
            contentStream.endText();
            x += columnWidths[index];
        }

        cursorY -= 13f;
    }

    private String fit(String text, PDFont font, float width) throws IOException {
        String fitted = text;
        while (!fitted.isEmpty() && font.getStringWidth(fitted) / 1000 * TABLE_FONT_SIZE > width) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        return fitted;
    }

    // The standard PDF fonts cover Western characters only, and showText throws on anything else, so
    // one unusual letter in a name would otherwise fail the whole document.
    private String printable(String text, PDFont font) throws IOException {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            String character = new String(Character.toChars(codePoint));
            try {
                font.encode(character);
                result.append(character);
            } catch (IllegalArgumentException e) {
                result.append('?');
            }
            index += Character.charCount(codePoint);
        }
        return result.toString();
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
        contentStream.showText(printable(text, font));
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