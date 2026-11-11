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
 * The company's document layout, modelled on a classic invoice: a white page with dark navy corner
 * bands, the recipient on the left, the logo and company name in the middle, the document title and its
 * reference details on the right, rounded table cells, a total box, signature lines and a contact strip
 * along the foot. Text is dark throughout; the logo is printed as uploaded, in its own colours, on white.
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
    private static final float RIGHT = PAGE_WIDTH - LEFT;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * LEFT;
    private static final float TOP_MARGIN = 60f;
    private static final float BOTTOM_MARGIN = 82f;

    private static final Color NAVY = new Color(0x0F, 0x23, 0x3A);
    private static final Color SLATE = new Color(0x2E, 0x4A, 0x66);
    private static final Color INK = new Color(0x11, 0x18, 0x27);
    private static final Color MUTED = new Color(0x37, 0x41, 0x51);
    private static final Color RULE = new Color(0xC9, 0xD2, 0xDC);
    private static final Color CELL = new Color(0xE6, 0xEB, 0xF1);
    private static final Color CELL_ALT = new Color(0xF2, 0xF5, 0xF8);
    private static final Color WHITE = Color.WHITE;

    // Header columns: recipient | logo and company | title and references.
    private static final float LEFT_COLUMN_WIDTH = 170f;
    private static final float CENTER_X = PAGE_WIDTH / 2f;
    private static final float CENTER_WIDTH = 150f;
    private static final float RIGHT_COLUMN_X = 378f;
    private static final float LOGO_MAX_WIDTH = 120f;
    private static final float LOGO_MAX_HEIGHT = 52f;

    // Money tables: number, description, amount, as separate rounded cells.
    private static final float CELL_GAP = 4f;
    private static final float NUMBER_WIDTH = 46f;
    private static final float AMOUNT_WIDTH = 150f;
    private static final float INFO_WIDTH = CONTENT_WIDTH - NUMBER_WIDTH - AMOUNT_WIDTH - 2 * CELL_GAP;
    private static final float INFO_X = LEFT + NUMBER_WIDTH + CELL_GAP;
    private static final float AMOUNT_X = RIGHT - AMOUNT_WIDTH;
    private static final float MONEY_ROW_HEIGHT = 21f;
    private static final float MONEY_ROW_STEP = MONEY_ROW_HEIGHT + 3f;
    private static final float MONEY_FONT_SIZE = 9.5f;
    private static final float RADIUS = 4f;

    private static final float TABLE_ROW_HEIGHT = 18f;
    private static final float TABLE_FONT_SIZE = 8.5f;
    private static final float TABLE_CELL_PADDING = 5f;

    private final PDDocument document = new PDDocument();
    private PDPageContentStream contentStream;
    private float cursorY;
    private int zebraRow;
    private Letterhead letterhead;

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
        cursorY = PAGE_HEIGHT - TOP_MARGIN;
    }

    // ---- header ----

    /**
     * Goes first on a document. The first detail is the recipient, printed large on the left; the rest
     * are listed under "Details". Up to 3 meta rows sit under the title on the right.
     */
    public void documentHeader(String title, List<Field> meta, Letterhead company, List<Field> details) throws IOException {
        this.letterhead = company;
        float top = PAGE_HEIGHT - 44f;

        // Centre: the logo in its own colours, the company name and its address.
        float centerBottom = writeCompany(company, top);

        // Right: the title, then the references.
        String heading = printable(title.toUpperCase(), boldFont);
        float rightWidth = RIGHT - RIGHT_COLUMN_X;
        float headingSize = 26f;
        while (headingSize > 13f && width(heading, boldFont, headingSize) > rightWidth) {
            headingSize -= 1f;
        }
        rightText(heading, boldFont, headingSize, RIGHT, top - headingSize * 0.75f, NAVY);
        float metaY = top - headingSize * 0.75f - 22f;
        float labelWidth = 0f;
        for (Field field : meta) {
            labelWidth = Math.max(labelWidth, width(printable(field.label(), regularFont), regularFont, 8.5f) + 4f);
        }
        labelWidth = Math.min(labelWidth, 70f);
        for (Field field : meta) {
            text(field.label(), regularFont, 8.5f, RIGHT_COLUMN_X, metaY, MUTED, labelWidth);
            text(": " + field.value(), boldFont, 8.5f, RIGHT_COLUMN_X + labelWidth, metaY, INK, rightWidth - labelWidth);
            metaY -= 13f;
        }

        // Left: who the document is for.
        float leftY = top - 8f;
        if (!details.isEmpty()) {
            Field recipient = details.get(0);
            text(recipient.label().toUpperCase() + ":", boldFont, 8.5f, LEFT, leftY, SLATE, LEFT_COLUMN_WIDTH);
            leftY -= 17f;
            for (String line : wrap(recipient.value(), boldFont, 14f, LEFT_COLUMN_WIDTH, 2)) {
                text(line, boldFont, 14f, LEFT, leftY, INK);
                leftY -= 16f;
            }
        }

        float bandTop = Math.min(Math.min(centerBottom, metaY + 4f), leftY) - 14f;

        // Second band: the remaining details on the left, the company's contacts on the right.
        float detailY = bandTop;
        if (details.size() > 1) {
            text("DETAILS:", boldFont, 8.5f, LEFT, detailY, SLATE);
            detailY -= 15f;
            for (Field field : details.subList(1, details.size())) {
                float detailLabelWidth = 72f;
                text(field.label(), regularFont, 9f, LEFT, detailY, MUTED, detailLabelWidth);
                text(": " + field.value(), boldFont, 9f, LEFT + detailLabelWidth, detailY, INK, 230f - detailLabelWidth);
                detailY -= 13f;
            }
        }

        float contactY = bandTop;
        List<String> contacts = contactLines(company);
        if (!contacts.isEmpty()) {
            text("ISSUED BY:", boldFont, 8.5f, RIGHT_COLUMN_X, contactY, SLATE);
            contactY -= 15f;
            if (company.name() != null && !company.name().isBlank()) {
                text(company.name(), boldFont, 10f, RIGHT_COLUMN_X, contactY, INK, rightWidth);
                contactY -= 13f;
            }
            for (String line : contacts) {
                text(line, regularFont, 8.5f, RIGHT_COLUMN_X, contactY, MUTED, rightWidth);
                contactY -= 12f;
            }
        }

        cursorY = Math.min(detailY, contactY) - 4f;
    }

    // Logo, name and address centred under one another; returns where the block ends.
    private float writeCompany(Letterhead company, float top) throws IOException {
        if (company == null) {
            return top;
        }
        float y = top;
        PDImageXObject picture = readLogo(company.logo());
        if (picture != null) {
            float scale = Math.min(LOGO_MAX_WIDTH / picture.getWidth(), LOGO_MAX_HEIGHT / picture.getHeight());
            float width = picture.getWidth() * scale;
            float height = picture.getHeight() * scale;
            contentStream.drawImage(picture, CENTER_X - width / 2f, y - height, width, height);
            y -= height + 14f;
        }
        if (company.name() != null && !company.name().isBlank()) {
            for (String line : wrap(company.name().toUpperCase(), boldFont, 11f, CENTER_WIDTH, 2)) {
                centered(line, boldFont, 11f, CENTER_X, y - 2f, NAVY);
                y -= 13f;
            }
        }
        if (company.address() != null && !company.address().isBlank()) {
            for (String line : wrap(company.address(), regularFont, 8f, CENTER_WIDTH, 2)) {
                centered(line, regularFont, 8f, CENTER_X, y - 3f, MUTED);
                y -= 10f;
            }
        }
        return y;
    }

    private List<String> contactLines(Letterhead company) {
        List<String> lines = new ArrayList<>();
        if (company == null) {
            return lines;
        }
        if (company.phone() != null && !company.phone().isBlank()) {
            lines.add("Tel: " + company.phone());
        }
        if (company.email() != null && !company.email().isBlank()) {
            lines.add("Email: " + company.email());
        }
        if (company.address() != null && !company.address().isBlank()) {
            lines.add(company.address());
        }
        return lines;
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

    // ---- money tables ----

    public void sectionTitle(String text) throws IOException {
        ensureSpace(70f);
        cursorY -= 24f;
        text(text.toUpperCase(), boldFont, 11f, LEFT, cursorY, NAVY);
        rectangle(NAVY, LEFT, cursorY - 6f, 28f, 2f);
        cursorY -= 10f;
        zebraRow = 0;
    }

    public void moneyHeader(String amountHeading) throws IOException {
        ensureSpace(MONEY_ROW_STEP * 2);
        cursorY -= MONEY_ROW_STEP;
        float baseline = baseline(cursorY, MONEY_ROW_HEIGHT, MONEY_FONT_SIZE);
        roundedRect(NAVY, LEFT, cursorY, NUMBER_WIDTH, MONEY_ROW_HEIGHT);
        roundedRect(NAVY, INFO_X, cursorY, INFO_WIDTH, MONEY_ROW_HEIGHT);
        roundedRect(NAVY, AMOUNT_X, cursorY, AMOUNT_WIDTH, MONEY_ROW_HEIGHT);
        centered("NO", boldFont, MONEY_FONT_SIZE, LEFT + NUMBER_WIDTH / 2f, baseline, WHITE);
        centered("DESCRIPTION", boldFont, MONEY_FONT_SIZE, INFO_X + INFO_WIDTH / 2f, baseline, WHITE);
        centered(amountHeading.toUpperCase(), boldFont, MONEY_FONT_SIZE, AMOUNT_X + AMOUNT_WIDTH / 2f, baseline, WHITE);
    }

    public void moneyRow(int number, String information, String amount) throws IOException {
        ensureSpace(MONEY_ROW_STEP);
        cursorY -= MONEY_ROW_STEP;
        Color fill = zebraRow++ % 2 == 0 ? CELL : CELL_ALT;
        roundedRect(fill, LEFT, cursorY, NUMBER_WIDTH, MONEY_ROW_HEIGHT);
        roundedRect(fill, INFO_X, cursorY, INFO_WIDTH, MONEY_ROW_HEIGHT);
        roundedRect(fill, AMOUNT_X, cursorY, AMOUNT_WIDTH, MONEY_ROW_HEIGHT);
        float baseline = baseline(cursorY, MONEY_ROW_HEIGHT, MONEY_FONT_SIZE);
        centered(String.format("%02d", number), regularFont, MONEY_FONT_SIZE, LEFT + NUMBER_WIDTH / 2f, baseline, INK);
        text(information, regularFont, MONEY_FONT_SIZE, INFO_X + 10f, baseline, INK, INFO_WIDTH - 20f);
        rightText(fit(printable(amount, boldFont), boldFont, MONEY_FONT_SIZE, AMOUNT_WIDTH - 20f), boldFont, MONEY_FONT_SIZE, RIGHT - 10f, baseline, INK);
    }

    /** A subtotal under a money table: the label beside a dark box holding the amount. */
    public void moneyTotal(String label, String amount) throws IOException {
        ensureSpace(MONEY_ROW_STEP + 4f);
        cursorY -= MONEY_ROW_STEP + 2f;
        float baseline = baseline(cursorY, MONEY_ROW_HEIGHT, MONEY_FONT_SIZE);
        rightText(fit(printable(label + ":", boldFont), boldFont, MONEY_FONT_SIZE, INFO_WIDTH), boldFont, MONEY_FONT_SIZE, AMOUNT_X - 12f, baseline, INK);
        roundedRect(NAVY, AMOUNT_X, cursorY, AMOUNT_WIDTH, MONEY_ROW_HEIGHT);
        rightText(fit(printable(amount, boldFont), boldFont, MONEY_FONT_SIZE, AMOUNT_WIDTH - 20f), boldFont, MONEY_FONT_SIZE, RIGHT - 10f, baseline, WHITE);
    }

    /** The amount the document exists to state. */
    public void netBar(String label, String amount) throws IOException {
        float height = 34f;
        ensureSpace(height + 16f);
        cursorY -= height + 14f;
        roundedRect(CELL, LEFT, cursorY, CONTENT_WIDTH, height);
        roundedRect(NAVY, AMOUNT_X, cursorY, AMOUNT_WIDTH, height);
        float baseline = baseline(cursorY, height, 12f);
        text(label.toUpperCase(), boldFont, 12f, LEFT + 14f, baseline, NAVY, AMOUNT_X - LEFT - 28f);
        rightText(fit(printable(amount, boldFont), boldFont, 12.5f, AMOUNT_WIDTH - 20f), boldFont, 12.5f, RIGHT - 10f, baseline(cursorY, height, 12.5f), WHITE);
    }

    // ---- other content ----

    /** Small dark grey text, wrapped to the page. */
    public void note(String text) throws IOException {
        for (String line : wrap(text, regularFont, 8.5f, CONTENT_WIDTH, 6)) {
            ensureSpace(13f);
            cursorY -= 12f;
            text(line, regularFont, 8.5f, LEFT, cursorY, MUTED);
        }
    }

    public void spacer(float amount) {
        cursorY -= amount;
    }

    /** Payment details on the left, then "approved" and "accepted" signature lines. */
    public void signatureBlock(String detailsTitle, List<String> details, String approvedBy, String acceptedBy) throws IOException {
        ensureSpace(100f);
        cursorY -= 30f;
        float top = cursorY;

        text(detailsTitle, boldFont, 10f, LEFT, top, NAVY);
        float lineY = top - 16f;
        for (String line : details) {
            text(line, regularFont, 9f, LEFT, lineY, MUTED, 190f);
            lineY -= 13f;
        }

        signature("Approved by", approvedBy, 335f, top);
        signature("Accepted by", acceptedBy, 470f, top);
        cursorY = Math.min(top - 72f, lineY);
    }

    private void signature(String heading, String name, float centerX, float top) throws IOException {
        float halfWidth = 58f;
        float lineY = top - 40f;
        contentStream.setStrokingColor(INK);
        contentStream.setLineWidth(0.8f);
        contentStream.moveTo(centerX - halfWidth, lineY);
        contentStream.lineTo(centerX + halfWidth, lineY);
        contentStream.stroke();
        centered(fit(printable(name, boldFont), boldFont, 9.5f, halfWidth * 2), boldFont, 9.5f, centerX, lineY - 13f, INK);
        centered(heading, regularFont, 8.5f, centerX, lineY - 25f, MUTED);
    }

    /** A grouping heading above a list table. */
    public void sectionHeading(String text) throws IOException {
        ensureSpace(46f);
        cursorY -= 20f;
        text(printable(text, boldFont), boldFont, 10.5f, LEFT, cursorY, NAVY);
        cursorY -= 5f;
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
            roundedRect(NAVY, LEFT, cursorY, tableWidth, TABLE_ROW_HEIGHT);
            zebraRow = 0;
        } else if (zebraRow++ % 2 == 1) {
            rectangle(CELL_ALT, LEFT, cursorY, tableWidth, TABLE_ROW_HEIGHT);
        }
        if (!bold) {
            rectangle(RULE, LEFT, cursorY, tableWidth, 0.5f);
        }

        PDFont font = bold ? boldFont : regularFont;
        Color color = bold ? WHITE : INK;
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

    private void roundedRect(Color color, float x, float y, float width, float height) throws IOException {
        float r = Math.min(RADIUS, Math.min(width, height) / 2f);
        float k = 0.5523f * r;
        contentStream.setNonStrokingColor(color);
        contentStream.moveTo(x + r, y);
        contentStream.lineTo(x + width - r, y);
        contentStream.curveTo(x + width - r + k, y, x + width, y + r - k, x + width, y + r);
        contentStream.lineTo(x + width, y + height - r);
        contentStream.curveTo(x + width, y + height - r + k, x + width - r + k, y + height, x + width - r, y + height);
        contentStream.lineTo(x + r, y + height);
        contentStream.curveTo(x + r - k, y + height, x, y + height - r + k, x, y + height - r);
        contentStream.lineTo(x, y + r);
        contentStream.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        contentStream.closePath();
        contentStream.fill();
    }

    private void circle(Color color, float centerX, float centerY, float radius) throws IOException {
        roundedRectRadius(color, centerX - radius, centerY - radius, radius * 2, radius);
    }

    private void roundedRectRadius(Color color, float x, float y, float size, float r) throws IOException {
        float k = 0.5523f * r;
        contentStream.setNonStrokingColor(color);
        contentStream.moveTo(x + r, y);
        contentStream.curveTo(x + r + k, y, x + size, y + r - k, x + size, y + r);
        contentStream.curveTo(x + size, y + r + k, x + r + k, y + size, x + r, y + size);
        contentStream.curveTo(x + r - k, y + size, x, y + r + k, x, y + r);
        contentStream.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        contentStream.closePath();
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

    private void rightText(String text, PDFont font, float size, float rightX, float y, Color color) throws IOException {
        String printable = printable(text, font);
        text(printable, font, size, rightX - width(printable, font, size), y, color);
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

    // The navy corner bands, the contact strip at the foot of every page, and page numbers once there
    // is more than one.
    private void writePageFrames() throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int index = 0; index < pageCount; index++) {
            try (PDPageContentStream frame = new PDPageContentStream(
                    document, document.getPage(index), PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream = frame;
                float top = PAGE_HEIGHT;

                // Top left: a navy corner with a slate stripe beside it.
                polygon(NAVY, 0, top, 96f, top, 0, top - 62f);
                polygon(SLATE, 106f, top, 118f, top, 0, top - 76f, 0, top - 68.5f);
                // Top right: a navy band down the edge.
                polygon(NAVY, PAGE_WIDTH - 60f, top, PAGE_WIDTH, top, PAGE_WIDTH, top - 210f, PAGE_WIDTH - 16f, top - 228f, PAGE_WIDTH - 16f, top - 26f);
                // Bottom corners, mirroring the top.
                polygon(NAVY, 0, 0, 0, 40f, 62f, 0);
                polygon(SLATE, 0, 47f, 0, 53f, 82f, 0, 73f, 0);
                polygon(NAVY, PAGE_WIDTH, 0, PAGE_WIDTH, 30f, PAGE_WIDTH - 66f, 0);

                writeContactStrip();
                if (pageCount > 1) {
                    String number = "Page " + (index + 1) + " of " + pageCount;
                    rightText(number, regularFont, 8f, RIGHT, 62f, MUTED);
                }
            }
        }
    }

    // Phone, email and address in a row along the foot, each after a small navy dot.
    private void writeContactStrip() throws IOException {
        List<String> items = new ArrayList<>();
        if (letterhead != null) {
            if (letterhead.phone() != null && !letterhead.phone().isBlank()) items.add(letterhead.phone());
            if (letterhead.email() != null && !letterhead.email().isBlank()) items.add(letterhead.email());
            if (letterhead.address() != null && !letterhead.address().isBlank()) items.add(letterhead.address());
        }
        if (items.isEmpty()) {
            return;
        }
        rectangle(RULE, LEFT, 50f, CONTENT_WIDTH, 0.6f);
        float slot = CONTENT_WIDTH / items.size();
        for (int index = 0; index < items.size(); index++) {
            String item = fit(printable(items.get(index), regularFont), regularFont, 8f, slot - 22f);
            float itemWidth = 10f + width(item, regularFont, 8f);
            float x = LEFT + slot * index + (slot - itemWidth) / 2f;
            circle(NAVY, x + 3f, 33f, 3f);
            text(item, regularFont, 8f, x + 10f, 30.5f, INK);
        }
    }

    public byte[] build() throws IOException {
        contentStream.close();
        writePageFrames();
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
