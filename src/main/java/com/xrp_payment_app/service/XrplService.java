package com.xrp_payment_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

// import com.fasterxml.jackson.databind.ObjectMapper;
import com.xrp_payment_app.dto.SubmitResultResponse;

@Service
public class XrplService {

    // private static final Logger logger = LoggerFactory.getLogger(XrplService.class);

    private final WebClient xrplWebClient;

    public XrplService(WebClient xrplWebClient) {
        this.xrplWebClient = xrplWebClient;
    }

    public WebClient getXrplWebClient() {
        try {
            return xrplWebClient;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch XRPL WebClient", e);
        }
    }

    public SubmitResultResponse submitSignedBlob(String signedTxBlob) {

        String requestBody = """
            {
                "method": "submit",
                "params": [{
                    "tx_blob": "%s"
                }]
            }
            """.formatted(signedTxBlob);

        // // Blocking call
        // String responseBody = webClient.post()
        //     .uri(rippledUrl)
        //     .contentType(MediaType.APPLICATION_JSON)
        //     .bodyValue(requestJson)
        //     .retrieve()
        //     .bodyToMono(String.class)
        //     .block();  // This makes it fully blocking/synchronous

        // // Parse the rippled response
        // try {
        //     JsonNode resultNode = objectMapper.readTree(responseBody).path("result");

        //     String engineResult = resultNode.path("engine_result").asText();
        //     String message = resultNode.path("engine_result_message").asText("");
        //     String txHash = resultNode.path("tx_json").path("hash").asText();

        //     return new SubmitBlobResult(engineResult, message, txHash);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to parse submit response", e);
        // }

        try {
            JsonNode responseJson = xrplWebClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            // Parse the response
            JsonNode resultNode = responseJson.path("result");

            String engineResult = resultNode.path("engine_result").asText();
            String message = resultNode.path("engine_result_message").asText("");
            String txHash = resultNode.path("tx_json").path("hash").asText();

            if (txHash.isBlank()) {
                throw new RuntimeException("Transaction hash not found in response. Engine result: " + engineResult);
            }

            boolean tentativelyAccepted = engineResult.startsWith("tes");

            return new SubmitResultResponse(engineResult, message, txHash, tentativelyAccepted);

        } catch (WebClientResponseException e) {
            throw new RuntimeException("XRPL node returned error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit transaction or parse response", e);
        }
    }
    
}


