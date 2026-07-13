package com.base.service;

import java.util.List;
import java.util.Map;

public interface DocumentTemplateService {
    /**
     * Create default .docx templates in the templates directory if they don't exist.
     */
    void bootstrapDefaultTemplates();

    /**
     * Generate a .docx document from a template and return the merged document bytes.
     * @param docType the document type (e.g. CONTRACT, PROPOSAL, RECEIPT)
     * @param data the data model to merge into the template
     * @return the generated file bytes
     */
    byte[] generateDocument(String docType, Map<String, Object> data);

    /**
     * Get the list of all available document template types.
     */
    List<String> getAvailableTemplates();

    /**
     * Convert DOCX file bytes to PDF format bytes.
     */
    byte[] convertDocxToPdf(byte[] docxBytes);
}
