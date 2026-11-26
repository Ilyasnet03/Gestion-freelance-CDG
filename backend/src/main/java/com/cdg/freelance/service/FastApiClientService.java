package com.cdg.freelance.service;

import com.cdg.freelance.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class FastApiClientService {

    @Value("${application.analytics.fastapi-url:http://localhost:8000}")
    private String fastApiUrl;

    @Value("${application.security.internal-api-key:cdg-internal-secret-api-key-2026}")
    private String internalApiKey;

    private RestClient restClient = RestClient.create();

    public Map<String, Object> executeAnalyses() {
        String url = fastApiUrl + "/analyses/run";
        try {
            return restClient.post()
                    .uri(url)
                    .header("X-Internal-API-Key", internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new BusinessException("Impossible de contacter le microservice d'analyse FastAPI (" + fastApiUrl + ") : " + e.getMessage());
        }
    }

    public byte[] generateMonthlyReportPdf(String mois) {
        String url = fastApiUrl + "/rapports/mensuel?mois=" + (mois != null ? mois : "");
        try {
            return restClient.post()
                    .uri(url)
                    .header("X-Internal-API-Key", internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            throw new BusinessException("Impossible de générer le rapport mensuel auprès de FastAPI : " + e.getMessage());
        }
    }
}
