package com.base.service.impl;

import com.base.service.DocumentTemplateService;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;
import fr.opensagres.xdocreport.itext.extension.font.IFontProvider;
import fr.opensagres.xdocreport.itext.extension.font.ITextFontRegistry;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentTemplateServiceImpl implements DocumentTemplateService {

    private static final Logger log = LoggerFactory.getLogger(DocumentTemplateServiceImpl.class);
    private static final String TEMPLATE_DIR = "uploads/templates/";
    private byte[] fontBytes;

    @PostConstruct
    @Override
    public void bootstrapDefaultTemplates() {
        try {
            File dir = new File(TEMPLATE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String[] templates = {"CONTRACT.docx", "PROPOSAL.docx", "RECEIPT.docx"};
            for (String templateName : templates) {
                File templateFile = new File(dir, templateName);
                if (!templateFile.exists()) {
                    copyTemplateFromResources(templateName, templateFile);
                }
            }

            // Load Arial font from resources to ensure portability (Docker / Kubernetes)
            ClassPathResource fontResource = new ClassPathResource("fonts/arial.ttf");
            if (fontResource.exists()) {
                try (InputStream in = fontResource.getInputStream()) {
                    this.fontBytes = org.apache.commons.io.IOUtils.toByteArray(in);
                    log.info("Successfully loaded Arial font from resources.");
                }
            } else {
                log.warn("Arial font not found in resources, conversion might fail in non-Windows environments.");
            }
        } catch (Exception e) {
            log.error("Failed to bootstrap default document templates: {}", e.getMessage(), e);
        }
    }

    private void copyTemplateFromResources(String templateName, File destination) throws Exception {
        ClassPathResource resource = new ClassPathResource("templates/" + templateName);
        if (resource.exists()) {
            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                log.info("Bootstrapped default {} template from resources at: {}", templateName, destination.getAbsolutePath());
            }
        } else {
            log.warn("Default template resource not found in classpath: templates/{}", templateName);
        }
    }

    @Override
    public byte[] generateDocument(String docType, Map<String, Object> data) {
        String templatePath = TEMPLATE_DIR + docType.toUpperCase() + ".docx";
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            throw new IllegalArgumentException("Template for " + docType + " not found.");
        }

        try {
            // Configure poi-tl to use single curly braces `{` and `}` like Carbone.io tags
            Configure config = Configure.builder().buildGramer("{", "}").build();

            // Wrap the data in a nested map key 'd' so `{d.myField}` path is resolved
            Map<String, Object> renderData = new HashMap<>();
            renderData.put("d", data != null ? data : new HashMap<>());

            XWPFTemplate template = XWPFTemplate.compile(templateFile, config).render(renderData);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            template.write(out);
            template.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate document for type {}: {}", docType, e.getMessage(), e);
            throw new RuntimeException("Error rendering template: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getAvailableTemplates() {
        List<String> templates = new ArrayList<>();
        File dir = new File(TEMPLATE_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".docx"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    templates.add(name.substring(0, name.lastIndexOf('.')).toUpperCase());
                }
            }
        }
        return templates;
    }

    @Override
    public byte[] convertDocxToPdf(byte[] docxBytes) {
        try (java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(docxBytes);
             XWPFDocument document = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Ensure document section properties (sectPr) and its key elements (pgSz, pgMar) are initialized
            // to prevent NullPointerException inside the PDF converter (StylableDocument.applySectPr).
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            if (sectPr == null) {
                sectPr = document.getDocument().getBody().addNewSectPr();
            }
            if (sectPr.getPgSz() == null) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz pgSz = sectPr.addNewPgSz();
                // Default to A4 size in twips (21cm x 29.7cm)
                pgSz.setW(java.math.BigInteger.valueOf(11906));
                pgSz.setH(java.math.BigInteger.valueOf(16838));
            }
            if (sectPr.getPgMar() == null) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pgMar = sectPr.addNewPgMar();
                // Default to standard 1 inch margins (1440 twips)
                pgMar.setLeft(java.math.BigInteger.valueOf(1440));
                pgMar.setRight(java.math.BigInteger.valueOf(1440));
                pgMar.setTop(java.math.BigInteger.valueOf(1440));
                pgMar.setBottom(java.math.BigInteger.valueOf(1440));
            }

            fr.opensagres.poi.xwpf.converter.pdf.PdfOptions options = fr.opensagres.poi.xwpf.converter.pdf.PdfOptions.create();
            
            // Register standard OS font directories (e.g. C:/Windows/Fonts or /usr/share/fonts)
            FontFactory.registerDirectories();
            
            options.fontProvider(new IFontProvider() {
                @Override
                public Font getFont(String familyName, String encoding, float size, int style, java.awt.Color color) {
                    try {
                        if (fontBytes != null) {
                            // Create base font using the embedded byte array to ensure portability (Docker/K8s)
                            BaseFont baseFont = BaseFont.createFont("arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
                            return new Font(baseFont, size, style, color);
                        }
                        String fontName = familyName;
                        // Default to Arial if font name is null or not registered
                        if (fontName == null || !FontFactory.isRegistered(fontName)) {
                            fontName = "Arial";
                        }
                        // Use IDENTITY_H encoding to support Vietnamese Unicode characters properly and embed fonts in PDF
                        return FontFactory.getFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style, color);
                    } catch (Exception e) {
                        log.warn("Failed to load font: {}, falling back to default registry. Error: {}", familyName, e.getMessage());
                        return ITextFontRegistry.getRegistry().getFont(familyName, encoding, size, style, color);
                    }
                }
            });

            fr.opensagres.poi.xwpf.converter.pdf.PdfConverter.getInstance().convert(document, out, options);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to convert DOCX to PDF: {}", e.getMessage(), e);
            throw new RuntimeException("DOCX to PDF conversion failed: " + e.getMessage(), e);
        }
    }}
