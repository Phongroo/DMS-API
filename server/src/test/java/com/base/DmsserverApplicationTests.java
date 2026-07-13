package com.base;

import com.base.service.DocumentTemplateService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class DmsserverApplicationTests {

	@Autowired
	private DocumentTemplateService documentTemplateService;

	@Test
	void contextLoads() {
	}

	@Test
	void testConvertDocxToPdfWithoutSectPr() throws Exception {
		byte[] docxBytes;
		try (XWPFDocument doc = new XWPFDocument();
			 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			doc.createStyles(); // Creates the styles.xml part required for PDF conversion
			// Verify that the new document body has null sectPr initially
			assertNull(doc.getDocument().getBody().getSectPr());
			doc.write(out);
			docxBytes = out.toByteArray();
		}
		byte[] pdfBytes = documentTemplateService.convertDocxToPdf(docxBytes);
		assertNotNull(pdfBytes);
	}

}
