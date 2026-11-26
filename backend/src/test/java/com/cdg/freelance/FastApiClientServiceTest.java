package com.cdg.freelance;

import com.cdg.freelance.service.FastApiClientService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FastApiClientServiceTest {

    @Test
    void parsesJsonResponseFromFastApi() {
        FastApiClientService service = new FastApiClientService();
        ReflectionTestUtils.setField(service, "fastApiUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(service, "internalApiKey", "test-key");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "restClient", builder.build());
        server.expect(requestTo("http://localhost:8000/analyses/run"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-API-Key", "test-key"))
                .andRespond(withSuccess("{\"status\":\"ok\",\"count\":2}", MediaType.APPLICATION_JSON));

        assertEquals("ok", service.executeAnalyses().get("status"));
        server.verify();
    }
}
