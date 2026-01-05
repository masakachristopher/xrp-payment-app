package com.xrp_payment_app.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xrp_payment_app.dto.SubmitResultResponse;
import com.xrp_payment_app.dto.SuccessPaymentResponse;
import com.xrp_payment_app.entity.Transaction;
import com.xrp_payment_app.repository.TransactionRepository;

import jakarta.transaction.Transactional;

@Service
public class XamanClientService {

    private static final Logger logger = LoggerFactory.getLogger(XamanClientService.class);

    @Value("${xaman.api.key}")
    private String xamanApiKey;

    @Value("${xaman.api.secret}")
    private String xamanApiSecret;

    @Value("${xaman.api.baseUrl}")
    private String xamanApiBaseUrlV1;


    @Autowired
    private final TransactionRepository transactionRepository;
    
    @Autowired
    private XrplService xrplService;
    private final WebClient xamanWebClient;


    public XamanClientService(
        WebClient xamanWebClient, 
        TransactionRepository transactionRepository,
        XrplService xrplService
    ) {
        this.xamanWebClient = xamanWebClient;
        this.transactionRepository = transactionRepository; 
        this.xrplService = xrplService;
    }

    public JsonNode callXamanCreatePayload(ObjectNode payload) throws JsonProcessingException {
        
        // ObjectMapper mapper = new ObjectMapper();
        // String jsonPayload = mapper.writeValueAsString(payload);

        logger.info("Creating Xaman payload: {}", payload.toString());
        logger.info("xamanApiKey: {}", xamanApiKey);
        logger.info("xamanApiSecret: {}", xamanApiSecret);
        logger.info("xamanApiBaseUrlV1: {}", xamanApiBaseUrlV1);

        try {

            JsonNode response = xamanWebClient.post()
                .uri("/payload")
                .header("X-API-Key", xamanApiKey)
                .header("X-API-Secret", xamanApiSecret)
                .header("Content-Type", "application/json")
                .header("accept", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Xaman payload: " + e.getMessage(), e);
        }
        
    }

    public JsonNode getXamanPayload(String uuid) {
        
        // ObjectMapper mapper = new ObjectMapper();
        // String jsonPayload = mapper.writeValueAsString(payload);
        logger.info("Fetching Xaman payload for UUID: {}", uuid);
        logger.info("xamanApiKey: {}", xamanApiKey);
        logger.info("xamanApiSecret: {}", xamanApiSecret);
        logger.info("xamanApiBaseUrlV1: {}", xamanApiBaseUrlV1);

        try {

            JsonNode response = xamanWebClient.get()
                .uri("/payload/{uuid}", uuid)
                .header("X-API-Key", xamanApiKey)
                .header("X-API-Secret", xamanApiSecret)
                .header("Content-Type", "application/json")
                .header("accept", "application/json")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Xaman payload: " + e.getMessage(), e);
        }
        
    }

    @Transactional
    public void handleXamanCallback(String payloadUuid, String paymentTxHash, List<String> signedTxBlobs) throws Exception {

        // String HexEx = "1200002400D03939201B00D07DB26140000000000000646840000000000000C7321020B7628A262E9D16B2E2EFD744FBEBBCA3599C6CCEF1FB44D1289B34C6781A6474473F45F221F9EAFEA9AA7674C12FECFED845F5F5D5E5F5F5F5F5F5F5F5F5F5F5F5F";
    
        // Submit main payment
        SubmitResultResponse mainResult = xrplService.submitSignedBlob(signedTxBlobs.get(0));
        // Submit fee payment only if main succeeded
        if (mainResult.engineResult().equals("tesSUCCESS")) {
            Transaction mainTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuid, "USER_PAYMENT");
            mainTx.setTransactionHash(mainResult.transactionHash());
            mainTx.setStatus(mainResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(mainTx);

            SubmitResultResponse feeResult = xrplService.submitSignedBlob(signedTxBlobs.get(1));
            Transaction feeTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuid, "PLATFORM_FEE");
            feeTx.setTransactionHash(feeResult.transactionHash());
            feeTx.setStatus(feeResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(feeTx);
        }
    }

    @Transactional
    public SuccessPaymentResponse handleBatchSignCallback(List<String> payloadUuids, String signedTxStatus) throws Exception {
    
        // Submit main payment
        if(!signedTxStatus.equals("SIGNED")) {
            throw new RuntimeException("Transaction not signed");
        }
        JsonNode mainPaymentPayload = getXamanPayload(payloadUuids.get(0));
        String signedMainTxBlob = mainPaymentPayload.path("response").path("hex").asText();

        JsonNode feePaymentPayload = getXamanPayload(payloadUuids.get(1));
        String signedFeeTxBlob = feePaymentPayload.path("response").path("hex").asText();

        List<String> signedTxBlobs = List.of(signedMainTxBlob, signedFeeTxBlob);
        logger.info("User payload Hex: {}, User Uuid: {}", signedMainTxBlob, payloadUuids.get(0));
        logger.info("Fee payload Hex: {}, Fee Uuid: {}", signedFeeTxBlob, payloadUuids.get(1));

        SubmitResultResponse mainResult = xrplService.submitSignedBlob(signedTxBlobs.get(0));
        // Submit fee payment only if main succeeded
        if (mainResult.engineResult().equals("tesSUCCESS")) {
            Transaction mainTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuids.get(0), "USER_PAYMENT");
            mainTx.setTransactionHash(mainResult.transactionHash());
            mainTx.setStatus(mainResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(mainTx);

            SubmitResultResponse feeResult = xrplService.submitSignedBlob(signedTxBlobs.get(1));
            Transaction feeTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuids.get(1), "PLATFORM_FEE");
            feeTx.setTransactionHash(feeResult.transactionHash());
            feeTx.setStatus(feeResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(feeTx);

            // PaymentResponse responsePayload = getXamanPayload(payloadUuids.get(0));
            return new SuccessPaymentResponse("COMPLETED", mainResult.transactionHash(), feeResult.transactionHash(), "Payment successful");
        } else {
            Transaction mainTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuids.get(0), "USER_PAYMENT");
            mainTx.setTransactionHash(mainResult.transactionHash());
            mainTx.setStatus(mainResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(mainTx);
        }

        throw new RuntimeException("Main payment failed with engine result: " + mainResult.engineResult());
    }

    @Transactional
    public SuccessPaymentResponse handleSignCallback(String payloadUuid, String signedTxStatus) throws Exception {
    
        // Submit main payment
        if(!signedTxStatus.equals("SIGNED")) {
            throw new RuntimeException("Transaction not signed");
        }
        JsonNode mainPaymentPayload = getXamanPayload(payloadUuid);
        String signedMainTxBlob = mainPaymentPayload.path("response").path("hex").asText();
        logger.info("User payload Hex: {}, User Uuid: {}", signedMainTxBlob, payloadUuid);

        SubmitResultResponse mainResult = xrplService.submitSignedBlob(signedMainTxBlob);
        // Submit fee payment only if main succeeded
        if (mainResult.engineResult().equals("tesSUCCESS")) {
            Transaction mainTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuid, "USER_PAYMENT");
            mainTx.setTransactionHash(mainResult.transactionHash());
            mainTx.setStatus(mainResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(mainTx);

            return new SuccessPaymentResponse("COMPLETED", mainResult.transactionHash(), null, "Payment successful");
        } else {
            Transaction mainTx = transactionRepository.findByPaymentReferenceAndPaymentType(payloadUuid, "USER_PAYMENT");
            mainTx.setTransactionHash(mainResult.transactionHash());
            mainTx.setStatus(mainResult.engineResult().equals("tesSUCCESS") ? "CONFIRMED" : "FAILED");
            transactionRepository.save(mainTx);
        }

        throw new RuntimeException("Main payment failed with engine result: " + mainResult.engineResult());
    }

}