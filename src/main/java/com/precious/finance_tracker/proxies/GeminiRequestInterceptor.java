package com.precious.finance_tracker.proxies;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class GeminiRequestInterceptor implements RequestInterceptor {
    private final AtomicLong requestCounter = new AtomicLong(0);

    @Override
    public void apply(RequestTemplate template) {
        long count = requestCounter.incrementAndGet();
        log.info(">>> FEIGN REQUEST #{} | Method: {} | URL: {}",
                count, template.method(), template.url());
    }
}
