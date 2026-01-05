package com.xrp_payment_app.controller.v2;

import com.xrp_payment_app.dto.BatchXummWebhookEvent;
import com.xrp_payment_app.dto.PaymentNonCustodialRequest;
import com.xrp_payment_app.dto.PaymentSignResponse;
import com.xrp_payment_app.dto.SuccessPaymentResponse;
import com.xrp_payment_app.dto.XummWebhookEvent;
import com.xrp_payment_app.exception.BadRequestException;
import com.xrp_payment_app.service.PaymentService;
import com.xrp_payment_app.service.XamanClientService;
import com.xrp_payment_app.constants.PathConstants;

import jakarta.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("paymentControllerV2")
@RequestMapping(PathConstants.API_V2_PAYMENT)
public class PaymentController {

    private final XamanClientService xamanClientService;

    @Autowired
    private PaymentService paymentService;

    PaymentController(XamanClientService xamanClientService) {
        this.xamanClientService = xamanClientService;
    }

    @PostMapping("/initiate")
    public PaymentSignResponse sendPayment(@RequestBody @NotNull PaymentNonCustodialRequest request, HttpServletRequest httpRequest) throws Exception {
        String requestId = httpRequest.getHeader("RequestId");
        if (requestId == null || requestId.isEmpty()) {
            throw new BadRequestException(
                "Missing RequestId header",
                "Please provide a RequestId header",
                "MISSING_REQUEST_ID"
            );
        }
        return paymentService.sendNonCustodialXrpWithFee(requestId, request.userName(), request.senderAddress(), request.destinationAddress(), request.amount());
    }

    @PostMapping("/initiate/batch")
    public PaymentSignResponse sendBatchPayment(@RequestBody @NotNull PaymentNonCustodialRequest request, HttpServletRequest httpRequest) throws Exception {
        String requestId = httpRequest.getHeader("RequestId");
        if (requestId == null || requestId.isEmpty()) {
            throw new BadRequestException(
                "Missing RequestId header",
                "Please provide a RequestId header",
                "MISSING_REQUEST_ID"
            );
        }
        return paymentService.sendNonCustodialXrpBatchWithFee(requestId, request.userName(), request.senderAddress(), request.destinationAddress(), request.amount());
    }

    @PostMapping("/callback")
    public SuccessPaymentResponse signCallback(@RequestBody XummWebhookEvent event) throws Exception {
        return xamanClientService.handleSignCallback(event.paymentUuid(), event.status());
    }

    @PostMapping("/callback/batch")
    public SuccessPaymentResponse batchSignCallback(@RequestBody BatchXummWebhookEvent event) throws Exception {
        return xamanClientService.handleBatchSignCallback(event.paymentUuids(), event.status());
    }

}