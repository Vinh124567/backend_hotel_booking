package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Configuration cho RestTemplate để gọi MoMo API
 * Cần thiết cho việc tích hợp payment gateway
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Timeout settings cho MoMo API calls
        factory.setConnectTimeout(10000); // 10 seconds để connect
        factory.setReadTimeout(30000);    // 30 seconds để đọc response

        return new RestTemplate(factory);
    }
}