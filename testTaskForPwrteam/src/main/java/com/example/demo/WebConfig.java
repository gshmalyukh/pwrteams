package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class WebConfig implements WebFluxConfigurer {
    @Bean
    public WebFilter contentNegotiationFilter() {
        return (exchange, chain) -> {

            String acceptHeader = exchange.getRequest().getHeaders().getFirst("Accept");

            String path = exchange.getRequest().getPath().toString();
            String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");


            boolean isOpenApiRequest = path.contains("/swagger") || path.contains("/v3/api-docs")
                    || (userAgent != null && userAgent.contains("Swagger"));

            if (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_XML_VALUE) && !isOpenApiRequest) {

                exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", HttpStatus.NOT_ACCEPTABLE.value());
                errorResponse.put("message", "Application response only in application/json");
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                byte[] bytes = serializeToJson(errorResponse).getBytes(StandardCharsets.UTF_8);
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }
            return chain.filter(exchange);
        };
    }

    private String serializeToJson(Map<String, Object> errorResponse) {
        try {
            return new ObjectMapper().writeValueAsString(errorResponse);
        } catch (JsonProcessingException e) {
            log.error("Error serializing response", e);
            // Fallback to a simple message if serialization fails
            return "{\"error\": \"Internal Server Error\"}";
        }
    }
}
