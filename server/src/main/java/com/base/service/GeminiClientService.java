package com.base.service;

import com.base.model.Res.BaseResponse;

public interface GeminiClientService {
    BaseResponse generate(String prompt);
    BaseResponse generateWithFile(String prompt, String mimeType, byte[] fileBytes);
}
