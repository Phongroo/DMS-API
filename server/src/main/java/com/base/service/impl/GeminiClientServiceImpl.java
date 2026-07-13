package com.base.service.impl;

import com.base.model.Res.BaseResponse;
import com.base.model.Res.MessageRes;
import com.base.service.GeminiClientService;
import com.base.worker.RetrieveDocumentAmountWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Service
public class GeminiClientServiceImpl implements GeminiClientService {
    @Value("${gemini.api.key}")
    private String apiKey;
    private static final Logger log = LoggerFactory.getLogger(GeminiClientServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();
    
    private final ObjectMapper mapper = new ObjectMapper();
    @Override
    public BaseResponse generate(String prompt) {
        try {

//            Client client = Client.builder().apiKey(apiKey).build();
//
//            GenerateContentResponse response =
//                    client.models.generateContent(
//                            "gemini-3.5-flash",
//                            "Explain how AI works in a few words",
//                            null);
//            System.out.println(response.text());

            String apiUrl =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key="
                    + apiKey;

            ObjectNode textNode = mapper.createObjectNode();
            textNode.put("text", prompt);

            ObjectNode partNode = mapper.createObjectNode();
            partNode.set("parts", mapper.createArrayNode().add(textNode));

            ObjectNode payloadNode = mapper.createObjectNode();
            payloadNode.set(
                    "contents",
                    mapper.createArrayNode().add(partNode)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request =
                    new HttpEntity<>(
                            mapper.writeValueAsString(payloadNode),
                            headers
                    );

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            apiUrl,
                            request,
                            String.class
                    );
            log.info("generativelanguage: {}", response.getBody());
            JsonNode root =
                    mapper.readTree(response.getBody());
            MessageRes messageRes = new MessageRes();
            messageRes.setMessage(root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText());
            return new BaseResponse(200, messageRes,"Success");

        } catch (Exception e) {
            return new BaseResponse(500, null,e.getMessage());
        }
    }

    @Override
    public BaseResponse generateWithFile(String prompt, String mimeType, byte[] fileBytes) {
        try {
            String apiUrl =
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                    + apiKey;

            ObjectNode promptPart = mapper.createObjectNode();
            promptPart.put("text", prompt);

            String base64Data = java.util.Base64.getEncoder().encodeToString(fileBytes);
            ObjectNode inlineDataNode = mapper.createObjectNode();
            inlineDataNode.put("mimeType", mimeType);
            inlineDataNode.put("data", base64Data);

            ObjectNode filePart = mapper.createObjectNode();
            filePart.set("inlineData", inlineDataNode);

            com.fasterxml.jackson.databind.node.ArrayNode partsArray = mapper.createArrayNode();
            partsArray.add(promptPart);
            partsArray.add(filePart);

            ObjectNode contentNode = mapper.createObjectNode();
            contentNode.set("parts", partsArray);

            ObjectNode payloadNode = mapper.createObjectNode();
            payloadNode.set(
                    "contents",
                    mapper.createArrayNode().add(contentNode)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request =
                    new HttpEntity<>(
                            mapper.writeValueAsString(payloadNode),
                            headers
                    );

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            apiUrl,
                            request,
                            String.class
                    );

            JsonNode root =
                    mapper.readTree(response.getBody());
            MessageRes messageRes = new MessageRes();
            messageRes.setMessage(root
                    .path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText());
            return new BaseResponse(200, messageRes, "Success");

        } catch (Exception e) {
            return new BaseResponse(500, null, e.getMessage());
        }
    }
}
