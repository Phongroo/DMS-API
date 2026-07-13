package com.base.controller;




import com.base.model.Res.BaseResponse;
import com.base.service.GeminiClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatBoxAIController {
    @Autowired
    private GeminiClientService client;

    @PostMapping
    public BaseResponse chat(@RequestBody String message) {
        return client.generate(
                message
        );
    }

}
