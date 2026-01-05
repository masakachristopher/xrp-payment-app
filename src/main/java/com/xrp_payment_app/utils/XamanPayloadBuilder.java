package com.xrp_payment_app.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class XamanPayloadBuilder {

    @Value("${xaman.api.callback-url}")
    private String callbackUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    public ObjectNode buildPaymentPayload(
            String destinationAddress,
            long totalAmountDrops,
            long feeDrops,
            long sequence,
            boolean submit
    ) {
        
        ObjectNode payload = mapper.createObjectNode();

        ObjectNode txJson = mapper.createObjectNode();
        txJson.put("TransactionType", "Payment");
        txJson.put("Destination", destinationAddress);
        txJson.put("Amount", String.valueOf(totalAmountDrops));
        txJson.put("Fee", String.valueOf(feeDrops));
        txJson.put("Sequence", sequence);

        payload.set("txjson", txJson);

        ObjectNode options = mapper.createObjectNode();
        ObjectNode returnUrl = mapper.createObjectNode();
        returnUrl.put("app", callbackUrl);
        returnUrl.put("web", callbackUrl);
        options.set("return_url", returnUrl);
        options.put("force_network", "TESTNET");
        options.put("submit", submit);

        payload.set("options", options);
        
        return payload;

    }

    // public Map<String, Object> buildMultiPaymentPayload(
    //         String toAddress,
    //         String platformAddress,
    //         long userPaymentAmountDrops,
    //         long feePaymentAmountDrops,
    //         long feeDrops,
    //         long userPaymentSequence,
    //         long feePaymentSequence,
    //         // String callbackUrl,
    //         boolean submit
    // ) {

    //     Map<String, Object> payload = new HashMap<>();

    //     // Build txjson array
    //     ArrayNode txJsonArray = mapper.createArrayNode();

    //     ObjectNode tx1 = mapper.createObjectNode();
    //     tx1.put("TransactionType", "Payment");
    //     tx1.put("Destination", toAddress);
    //     tx1.put("Amount", String.valueOf(userPaymentAmountDrops));
    //     tx1.put("Fee", String.valueOf(feeDrops));
    //     tx1.put("Sequence", userPaymentSequence);

    //     ObjectNode tx2 = mapper.createObjectNode();
    //     tx2.put("TransactionType", "Payment");
    //     tx2.put("Destination", platformAddress);
    //     tx2.put("Amount", String.valueOf(feePaymentAmountDrops));
    //     tx2.put("Fee", String.valueOf(feeDrops));
    //     tx2.put("Sequence", feePaymentSequence);

    //     txJsonArray.add(tx1);
    //     txJsonArray.add(tx2);

    //     payload.put("txjson", txJsonArray);

    //     // Build options
    //     Map<String, Object> options = new HashMap<>();
    //     Map<String, String> returnUrl = new HashMap<>();
    //     returnUrl.put("app", callbackUrl);
    //     returnUrl.put("web", callbackUrl);
    //     options.put("return_url", returnUrl);
    //     options.put("force_network", "TESTNET");
    //     options.put("submit", submit);

    //     payload.put("options", options);

    //     return payload;
    // }

}


