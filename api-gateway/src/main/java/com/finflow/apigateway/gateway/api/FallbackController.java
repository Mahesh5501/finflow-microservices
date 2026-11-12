package com.finflow.apigateway.gateway.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping(value = "/fallback/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "UPSTREAM_UNAVAILABLE");
        body.put("message", "Service temporarily unavailable: " + service);
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("service", service);
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
