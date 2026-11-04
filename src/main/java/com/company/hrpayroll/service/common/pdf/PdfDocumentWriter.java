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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The company's document layout, in black and grey: an angled header with the title on the left and
 * the logo and contact details on the right, money tables, a net amount bar and signature lines.
 * The logo is read when a document is generated, so a new upload shows on the next download.
 */
public class PdfDocumentWriter implements AutoCloseable {

    /** What the header prints for the company. Any field may be null. */
    public record Letterhead(String name, byte[] logo, String address, String phone, String email) {}

    /** A label and its value in the header. */
    public record Field(String label, String value) {}

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 50f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * LEFT;
    private static final float BOTTOM_MARGIN = 50f;

    private static final Color BLACK = new Color(0x11, 0x11, 0x11);
    private static final Color GREY = new Color(0x59, 0x59, 0x59);
    private static final Color LIGHT_GREY = new Color(0x9A, 0x9A, 0x9A);
    private static final Color ROW_GREY = new Color(0xEF, 0xEF, 0xEF);
    private static final Color WHITE = Color.WHITE;

    // Header shape. The black panel's left edge runs from the top of the page down to its bottom.
    private static final float EDGE_TOP_X = 272f;
    private static final float EDGE_BOTTOM_X = 378f;
    private static final float GREY_PANEL_HEIGHT = 118f;
    private static final float BLACK_PANEL_HEIGHT = 217f;

    private static final float TITLE_MAX_WIDTH = 245f;
    private static final float DETAIL_VALUE_X = 140f;
    private static final float COMPANY_CENTER_X = 485f;
    private static final float COMPANY_WIDTH = 196f;
    private static final float LOGO_TILE_WIDTH = 130f;
    private static final float LOGO_TILE_HEIGHT = 54f;
    private static final float LOGO_PADDING = 6f;

    // Money tables: number, description, amount.
    private static final float NUMBER_CENTER = LEFT + 45f;
    private static final float INFO_CENTER = LEFT + 215f;
    private static final float AMOUNT_CENTER = LEFT + 417f;
    private static final float TOTAL_LEFT = LEFT + 165f;
    private static final float TOTAL_LABEL_CENTER = LEFT + 252f;
    private static final float MONEY_ROW_HEIGHT = 23f;
    private static final float MONEY_FONT_SIZE = 9.5f;

    private static final float TABLE_ROW_HEIGHT = 18f;
    private static final float TABLE_FONT_SIZE = 8.5f;
    private static final float TABLE_CELL_PADDING = 4f;

    private final PDDocument document = new PDDocument();
    private PDPageContentStream contentStream;
    private float cursorY;
    private int zebraRow;

    private final PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final PDFont italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    private final PDFont boldItalicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE);

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
        cursorY = PAGE_HEIGHT - BOTTOM_MARGIN;
    }

    // ---- header ----

    /** Goes first on a document. Up to 3 meta rows sit under the title and up to 5 details under the panel. */
    public void documentHeader(String title, List<Field> meta, Letterhead company, List<Field> details) throws IOException {
        float top = PAGE_HEIGHT;
        float greyBottom = top - GREY_PANEL_HEIGHT;
        float blackBottom = top - BLACK_PANEL_HEIGHT;
        float greyCornerX = edgeX(greyBottom);

        polygon(GREY, 0, top, EDGE_TOP_X, top, greyCornerX, greyBottom, 0, greyBottom);
        polygon(LIGHT_GREY,
                greyCornerX - 4f, greyBottom,
                EDGE_BOTTOM_X - 6f, blackBottom - 7f,
                PAGE_WIDTH, blackBottom - 7f,
                PAGE_WIDTH, blackBottom,
                EDGE_BOTTOM_X, blackBottom,
                greyCornerX, greyBottom);
        polygon(BLACK, EDGE_TOP_X, top, PAGE_WIDTH, top, PAGE_WIDTH, blackBottom, EDGE_BOTTOM_X, blackBottom);

        String heading = printable(title.toUpperCase(), boldFont);
        float headingSize = 30f;
        while (headingSize > 14f && width(heading, boldFont, headingSize) > TITLE_MAX_WIDTH) {
            headingSize -= 1f;
        }
        text(heading, boldFont, headingSize, LEFT, top - 58f, WHITE);

        float metaY = top - 80f;
        for (Field field : meta) {
            text(field.label(), boldFont, 9.5f, LEFT, metaY, WHITE);
            float valueX = LEFT + 78f;
            text(": " + field.value(), boldFont, 9.5f, valueX, metaY, WHITE, greyCornerX - valueX - 8f);
            metaY -= 15f;
        }

        writeCompany(company, blackBottom, top);

        float detailY = greyBottom - 22f;
        for (Field field : details) {
            text(field.label(), boldFont, 10f, LEFT, detailY, GREY);
            text(": " + field.value(), boldFont, 10f, DETAIL_VALUE_X, detailY, BLACK, edgeX(detailY) - DETAIL_VALUE_X - 14f);
            detailY -= 16f;
        }

        cursorY = Math.min(blackBottom - 30f, detailY - 8f);
    }

    private float edgeX(float y) {
        return EDGE_TOP_X + (PAGE_HEIGHT - y) / BLACK_PANEL_HEIGHT * (EDGE_BOTTOM_X - EDGE_TOP_X);
    }

    // Logo, name and contact lines, centred as one block inside the black panel whatever is present.
    private void writeCompany(Letterhead company, float panelBottom, float panelTop) throws IOException {
        if (company == null) {
            return;
        }

        PDImageXObject picture = readLogo(company.logo());
        List<String> nameLines = company.name() == null ? List.of() : wrap(company.name(), boldFont, 13f, COMPANY_WIDTH, 2);
        List<String> contactLines = new ArrayList<>();
        if (company.address() != null && !company.address().isBlank()) {
            contactLines.addAll(wrap(company.address(), regularFont, 9f, COMPANY_WIDTH, 2));
        }
        if (company.phone() != null && !company.phone().isBlank()) {
            contactLines.add(printable(company.phone(), regularFont));
        }
        if (company.email() != null && !company.email().isBlank()) {
            contactLines.add(printable(company.email(), regularFont));
        }

        float blockHeight = (picture != null ? LOGO_TILE_HEIGHT + 12f : 0f)
                + nameLines.size() * 16f
                + (contactLines.isEmpty() ? 0f : 6f + contactLines.size() * 14f);
        float y = panelBottom + (panelTop - panelBottom + blockHeight) / 2f;

        if (picture != null) {
            drawLogo(picture, y);
            y -= LOGO_TILE_HEIGHT + 12f;
        }
        for (String line : nameLines) {
            centered(line, boldFont, 13f, COMPANY_CENTER_X, y - 11f, WHITE);
            y -= 16f;
        }
        if (!contactLines.isEmpty()) {
            y -= 6f;
        }
        for (String line : contactLines) {
            centered(fit(line, regularFont, 9f, COMPANY_WIDTH), regularFont, 9f, COMPANY_CENTER_X, y - 10f, WHITE);
            y -= 14f;
        }
    }

    // A file PDFBox cannot read is skipped rather than costing the user their document.
    private PDImageXObject readLogo(byte[] image) {
        if (image == null || image.length == 0) {
            return null;
        }
        try {
            return PDImageXObject.createFromByteArray(document, image, "logo");
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    // On a white tile, so a dark logo stays visible on the black panel. Scaled without stretching.
    private void drawLogo(PDImageXObject picture, float tileTop) throws IOException {
        float tileLeft = COMPANY_CENTER_X - LOGO_TILE_WIDTH / 2f;
        float tileBottom = tileTop - LOGO_TILE_HEIGHT;
        rectangle(WHITE, tileLeft, tileBottom, LOGO_TILE_WIDTH, LOGO_TILE_HEIGHT);

        float scale = Math.min(
                (LOGO_TILE_WIDTH - 2 * LOGO_PADDING) / picture.getWidth(),
                (LOGO_TILE_HEIGHT - 2 * LOGO_PADDING) / picture.getHeight());
        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;
        contentStream.drawImage(
                picture,
                COMPANY_CENTER_X - width / 2f,
                tileBottom + (LOGO_TILE_HEIGHT - height) / 2f,
                width,
                height);
    }

    // ---- money tables ----

    public void sectionTitle(String text) throws IOException {
        ensureSpace(66f);
        cursorY -= 22f;
        text(text.toUpperCase(), boldFont, 14f, LEFT, cursorY, BLACK);
        cursorY -= 8f;
        zebraRow = 0;
    }

    public void moneyHeader(String amountHeading) throws IOException {
        ensureSpace(MONEY_ROW_HEIGHT * 2);
        cursorY -= MONEY_ROW_HEIGHT;
        rectangle(GREY, LEFT, cursorY, CONTENT_WIDTH, MONEY_ROW_HEIGHT);
        float baseline = baseline(cursorY, MONEY_ROW_HEIGHT, MONEY_FONT_SIZE);
        centered("NO", boldFont, MONEY_FONT_SIZE, NUMBER_CENTER, baseline, WHITE);
        centered("INFORMATION", boldFont, MONEY_FONT_SIZE, INFO_CENTER, baseline, WHITE);
        centered(amountHeading.toUpperCase(), boldFont, MONEY_FONT_SIZE, AMOUNT_CENTER, baseline, WHITE);
    }

    public void moneyRow(int number, String information, String amount) throws IOException {
        ensureSpace(MONEY_ROW_HEIGHT);
        cursorY -= MONEY_ROW_HEIGHT;
        if (zebraRow++ % 2 == 1) {
            rectangle(ROW_GREY, LEFT, cursorY, CONTENT_WIDTH, MONEY_ROW_HEIGHT);
        }
        float baseline = baseline(cursorY, MONEY_ROW_HEIGHT, MONEY_FONT_SIZE);
        centered(String.valueOf(number), boldFont, MONEY_FONT_SIZE, NUMBER_CENTER, baseline, BLACK);
        centered(fit(printable(information, boldFont), boldFont, MONEY_FONT_SIZE, 240f), boldFont, MONEY_FONT_SIZE, INFO_CENTER, baseline, BLACK);
        centered(fit(printable(amount, boldFont), boldFont, MONEY_FONT_SIZE, 145f), boldFont, MONEY_FONT_SIZE, AMOUNT_CENTER, baseline, BLACK);
    }

    public void moneyTotal(String label, String amount) throws IOException {
        ensureSpace(MONEY_ROW_HEIGHT);
        cursorY -= MONEY_ROW_HEIGHT;
        rectangle(GREY, TOTAL_LEFT, cursorY, LEFT + CONTENT_WIDTH - TOTAL_LEFT, MONEY_ROW_HEIGHT);
        float baseline = baseline(cursorY, MONEY_ROW_HEIGHT, MONEY_FONT_SIZE);
        centered(fit(printable(label.toUpperCase(), boldFont), boldFont, MONEY_FONT_SIZE, 165f), boldFont, MONEY_FONT_SIZE, TOTAL_LABEL_CENTER, baseline, WHITE);
        centered(fit(printable(amount, boldFont), boldFont, MONEY_FONT_SIZE, 145f), boldFont, MONEY_FONT_SIZE, AMOUNT_CENTER, baseline, WHITE);
    }

    /** The amount the document exists to state. */
    public void netBar(String label, String amount) throws IOException {
        float height = 36f;
        float gap = 6f;
        float amountWidth = 155f;
        float labelWidth = CONTENT_WIDTH - amountWidth - gap;
        ensureSpace(height + 12f);
        cursorY -= height + 10f;

        rectangle(BLACK, LEFT, cursorY, labelWidth, height);
        rectangle(BLACK, LEFT + labelWidth + gap, cursorY, amountWidth, height);
        float baseline = baseline(cursorY, height, 12f);
        text(label.toUpperCase(), boldFont, 12f, LEFT + 10f, baseline, WHITE, labelWidth - 20f);
        centered(fit(printable(amount, boldFont), boldFont, 12f, amountWidth - 12f), boldFont, 12f, LEFT + labelWidth + gap + amountWidth / 2f, baseline, WHITE);
    }

    // ---- other content ----

    /** Small grey text, wrapped to the page. */
    public void note(String text) throws IOException {
        for (String line : wrap(text, regularFont, 8.5f, CONTENT_WIDTH, 6)) {
            ensureSpace(13f);
            cursorY -= 12f;
            text(line, regularFont, 8.5f, LEFT, cursorY, GREY);
        }
    }

    public void spacer(float amount) {
        cursorY -= amount;
    }

    /** Payment details on the left, then "approved" and "accepted" signature lines. */
    public void signatureBlock(String detailsTitle, List<String> details, String approvedBy, String acceptedBy) throws IOException {
        ensureSpace(112f);
        cursorY -= 30f;
        float top = cursorY;

        text(detailsTitle, boldItalicFont, 10.5f, LEFT, top, BLACK);
        float lineY = top - 18f;
        for (String line : details) {
            text(line, italicFont, 10f, LEFT, lineY, GREY, 180f);
            lineY -= 16f;
        }

        signature("Approved by:", approvedBy, 318f, top);
        signature("Accepted by:", acceptedBy, 468f, top);
        cursorY = top - 80f;
    }

    private void signature(String heading, String name, float centerX, float top) throws IOException {
        float halfWidth = 62f;
        centered(heading, boldItalicFont, 10.5f, centerX, top, BLACK);
        float lineY = top - 50f;
        contentStream.setStrokingColor(BLACK);
        contentStream.setLineWidth(0.8f);
        contentStream.setLineDashPattern(new float[] {1f, 2f}, 0f);
        contentStream.moveTo(centerX - halfWidth, lineY);
        contentStream.lineTo(centerX + halfWidth, lineY);
        contentStream.stroke();
        contentStream.setLineDashPattern(new float[] {}, 0f);
        String label = "(" + fit(printable(name, boldItalicFont), boldItalicFont, 10f, halfWidth * 2 - 10f) + ")";
        centered(label, boldItalicFont, 10f, centerX, lineY - 15f, GREY);
    }

    /** A grouping heading above a list table. */
    public void sectionHeading(String text) throws IOException {
        ensureSpace(46f);
        cursorY -= 18f;
        text(printable(text, boldFont), boldFont, 11f, LEFT, cursorY, BLACK);
        cursorY -= 4f;
        zebraRow = 0;
    }

    // One row of a list table. A bold row is its heading. Text too wide for its column is cut short
    // rather than running into the next.
    public void tableRow(float[] columnWidths, List<String> cells, boolean bold) throws IOException {
        ensureSpace(TABLE_ROW_HEIGHT);
        cursorY -= TABLE_ROW_HEIGHT;

        float tableWidth = 0f;
        for (float width : columnWidths) {
            tableWidth += width;
        }

        if (bold) {
            rectangle(GREY, LEFT, cursorY, tableWidth, TABLE_ROW_HEIGHT);
            zebraRow = 0;
        } else if (zebraRow++ % 2 == 1) {
            rectangle(ROW_GREY, LEFT, cursorY, tableWidth, TABLE_ROW_HEIGHT);
        }

        PDFont font = bold ? boldFont : regularFont;
        Color color = bold ? WHITE : BLACK;
        float baseline = baseline(cursorY, TABLE_ROW_HEIGHT, TABLE_FONT_SIZE);
        float x = LEFT;
        for (int index = 0; index < columnWidths.length; index++) {
            text(cells.get(index), font, TABLE_FONT_SIZE, x + TABLE_CELL_PADDING, baseline, color, columnWidths[index] - 2 * TABLE_CELL_PADDING);
            x += columnWidths[index];
        }
    }

    // ---- drawing ----

    private float baseline(float bottom, float height, float fontSize) {
        return bottom + (height - fontSize * 0.7f) / 2f;
    }

    private void polygon(Color color, float... points) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.moveTo(points[0], points[1]);
        for (int index = 2; index < points.length; index += 2) {
            contentStream.lineTo(points[index], points[index + 1]);
        }
        contentStream.closePath();
        contentStream.fill();
    }

    private void rectangle(Color color, float x, float y, float width, float height) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
    }

    private void text(String text, PDFont font, float size, float x, float y, Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.beginText();
        contentStream.setFont(font, size);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(printable(text, font));
        contentStream.endText();
    }

    private void text(String text, PDFont font, float size, float x, float y, Color color, float maxWidth) throws IOException {
        text(fit(printable(text, font), font, size, maxWidth), font, size, x, y, color);
    }

    private void centered(String text, PDFont font, float size, float centerX, float y, Color color) throws IOException {
        String printable = printable(text, font);
        text(printable, font, size, centerX - width(printable, font, size) / 2f, y, color);
    }

    private float width(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000 * size;
    }

    private String fit(String text, PDFont font, float size, float maxWidth) throws IOException {
        if (width(text, font, size) <= maxWidth) {
            return text;
        }
        String fitted = text;
        while (!fitted.isEmpty() && width(fitted + "...", font, size) > maxWidth) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        return fitted.stripTrailing() + "...";
    }

    private List<String> wrap(String text, PDFont font, float size, float maxWidth, int maxLines) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String[] words = printable(text, font).trim().split("\\s+");

        for (int index = 0; index < words.length; index++) {
            String candidate = line.isEmpty() ? words[index] : line + " " + words[index];
            if (line.isEmpty() || width(candidate, font, size) <= maxWidth) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (lines.size() == maxLines - 1) {
                // The last line allowed takes everything left, cut short.
                StringBuilder rest = new StringBuilder(line);
                for (int remaining = index; remaining < words.length; remaining++) {
                    rest.append(' ').append(words[remaining]);
                }
                lines.add(fit(rest.toString(), font, size, maxWidth));
                return lines;
            }
            lines.add(fit(line.toString(), font, size, maxWidth));
            line = new StringBuilder(words[index]);
        }
        if (!line.isEmpty()) {
            lines.add(fit(line.toString(), font, size, maxWidth));
        }
        return lines;
    }

    // The standard PDF fonts cover Western characters only, and showText throws on anything else, so
    // one unusual letter in a name would otherwise fail the whole document.
    private String printable(String text, PDFont font) throws IOException {
        if (text == null) {
            return "-";
        }
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

    private void ensureSpace(float needed) throws IOException {
        if (cursorY - needed < BOTTOM_MARGIN) {
            newPage();
        }
    }

    // ---- finishing ----

    // The band at the foot of every page, and page numbers once there is more than one.
    private void writeFooters() throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int index = 0; index < pageCount; index++) {
            try (PDPageContentStream footer = new PDPageContentStream(
                    document, document.getPage(index), PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream = footer;
                polygon(LIGHT_GREY, 0, 30f, 178f, 30f, 200f, 0, 0, 0);
                polygon(BLACK, 0, 22f, 168f, 22f, 186f, 0, 0, 0);
                polygon(BLACK, 180f, 22f, 250f, 22f, 266f, 0, 198f, 0);
                if (pageCount > 1) {
                    String number = "Page " + (index + 1) + " of " + pageCount;
                    text(number, regularFont, 8f, PAGE_WIDTH - LEFT - width(number, regularFont, 8f), 12f, GREY);
                }
            }
        }
    }

    public byte[] build() throws IOException {
        contentStream.close();
        writeFooters();
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
