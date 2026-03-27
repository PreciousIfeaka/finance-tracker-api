package com.precious.finance_tracker.proxies;

import com.precious.finance_tracker.dtos.gemini.GeminiRequest;
import com.precious.finance_tracker.dtos.gemini.GeminiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "gemini-api-proxy",
        url = "${gemini.api.base_url}"
)
public interface GeminiClientProxy {
    @PostMapping(
            value = "${gemini.ai.model}" + ":generateContent",
            consumes = "application/json"
    )
    GeminiResponse analyzeStatement(
            @RequestParam("key") String apiKey,
            @RequestBody() GeminiRequest request
    );
}
