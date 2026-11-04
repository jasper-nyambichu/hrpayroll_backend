package com.company.hrpayroll.service.settlement.impl;

import com.company.hrpayroll.dto.response.settlement.PayslipResponse;
import com.company.hrpayroll.service.common.pdf.PdfDocumentWriter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayslipPdfTest {

    private static PayslipResponse payslip(String name, List<PayslipResponse.RecordedPayment> payments) {
        return new PayslipResponse(
                1L, 1L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 1L, "EMP-0009", name, "Kisii Matt Main Branch", "Test Agent Ltd",
                26, new BigDecimal("1000.00"), new BigDecimal("26000.00"), new BigDecimal("1200.00"), new BigDecimal("1560.00"),
                new BigDecimal("150.00"), new BigDecimal("2000.00"), new BigDecimal("500.00"), new BigDecimal("26410.00"),
                new BigDecimal("10000.00"), new BigDecimal("16410.00"), payments);
    }

    private static byte[] logo(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(0x0B, 0x4F, 0x8A));
        graphics.fillOval(width / 2 - height / 3, height / 6, height * 2 / 3, height * 2 / 3);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] render(PayslipResponse payslip, PdfDocumentWriter.Letterhead company) throws IOException {
        try (PdfDocumentWriter writer = new PdfDocumentWriter()) {
            PayslipServiceImpl.writePayslip(writer, payslip, company);
            return writer.build();
        }
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static int pages(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return document.getNumberOfPages();
        }
    }

    @Test
    void printsTheFiguresAndDatesDayFirst() throws IOException {
        byte[] pdf = render(
                payslip("Asha Wanjiru", List.of(new PayslipResponse.RecordedPayment("QAB123", new BigDecimal("10000.00"), LocalDate.of(2026, 12, 2)))),
                new PdfDocumentWriter.Letterhead("Dhwarsh Ltd", logo(400, 160), "Kisii", "+254 700 000000", "hr@example.com"));

        assertThat(text(pdf))
                .contains("PAYSLIP", "01 Nov 2026 to 30 Nov 2026", "02 Dec 2026", "QAB123", "Asha Wanjiru", "Dhwarsh Ltd", "KES 26,410.00")
                .doesNotContain("2026-11-01");
        assertThat(pages(pdf)).isEqualTo(1);
    }

    @Test
    void survivesNoCompanyAnUnreadableLogoAndLongValues() throws IOException {
        String longName = "Wanjiru ".repeat(30).trim();

        assertThat(text(render(payslip(longName, List.of()), null))).contains("PAYSLIP");
        assertThat(text(render(payslip("Asha", List.of()), new PdfDocumentWriter.Letterhead(longName, new byte[] {1, 2, 3}, longName, null, null))))
                .contains("PAYSLIP");
    }

    @Test
    void manyPaymentsRunOntoANumberedSecondPage() throws IOException {
        List<PayslipResponse.RecordedPayment> payments = java.util.stream.IntStream.range(0, 30)
                .mapToObj(index -> new PayslipResponse.RecordedPayment("CODE" + index, new BigDecimal("100.00"), LocalDate.of(2026, 12, 2)))
                .toList();

        byte[] pdf = render(payslip("Asha", payments), null);

        assertThat(pages(pdf)).isGreaterThan(1);
        assertThat(text(pdf)).contains("Page 1 of", "CODE29");
    }

    /** Set PDF_PREVIEW_DIR to a folder to get images of the layout. Does nothing otherwise. */
    @Test
    void writesPreviewsWhenAsked() throws IOException {
        String folder = System.getenv("PDF_PREVIEW_DIR");
        if (folder == null) {
            return;
        }
        List<PayslipResponse.RecordedPayment> paid = List.of(new PayslipResponse.RecordedPayment("QAB123XYZ", new BigDecimal("10000.00"), LocalDate.of(2026, 12, 2)));
        preview(render(payslip("Asha Wanjiru", paid), new PdfDocumentWriter.Letterhead("Dhwarsh Limited", logo(400, 160), "Kisii Town, Hospital Road", "+254 700 000 000", "hr@dhwarsh.example")), Path.of(folder, "with-logo.png"));
        preview(render(payslip("Asha Wanjiru", List.of()), new PdfDocumentWriter.Letterhead("Dhwarsh Limited", null, null, "+254 700 000 000", null)), Path.of(folder, "no-logo.png"));
        preview(render(payslip("Wanjiru ".repeat(30).trim(), List.of()), new PdfDocumentWriter.Letterhead("A Very Long Company Name Holdings And Partners Limited Of East Africa", logo(160, 400), "A very long address that keeps going past one line of the panel, Kisii, Kenya", "+254 700 000 000", "someone.with.a.long.address@dhwarsh.example")), Path.of(folder, "long-values.png"));
    }

    private static void preview(byte[] pdf, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (PDDocument document = Loader.loadPDF(pdf)) {
            ImageIO.write(new PDFRenderer(document).renderImageWithDPI(0, 90), "png", target.toFile());
        }
    }
}
