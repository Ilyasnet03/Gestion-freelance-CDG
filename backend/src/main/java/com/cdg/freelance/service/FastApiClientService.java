package com.cdg.freelance.service;

import com.cdg.freelance.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class FastApiClientService {

    @Value("${application.analytics.fastapi-url:http://localhost:8000}")
    private String fastApiUrl;

    @Value("${application.security.internal-api-key:cdg-internal-secret-api-key-2026}")
    private String internalApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> executeAnalyses() {
        String url = fastApiUrl + "/analyses/run";
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            throw new BusinessException("Impossible de contacter le microservice d'analyse FastAPI (" + fastApiUrl + ") : " + e.getMessage());
        }
    }

    public byte[] generateMonthlyReportPdf(String mois) {
        String url = fastApiUrl + "/rapports/mensuel?mois=" + (mois != null ? mois : "");
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            throw new BusinessException("Impossible de générer le rapport mensuel auprès de FastAPI : " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-API-Key", internalApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
