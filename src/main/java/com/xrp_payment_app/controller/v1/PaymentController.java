package com.xrp_payment_app.controller.v1;

import com.xrp_payment_app.constants.PathConstants;
import com.xrp_payment_app.dto.PaymentRequest;
import com.xrp_payment_app.dto.PaymentResponse;
import com.xrp_payment_app.service.PaymentService;

import jakarta.servlet.http.HttpServletRequest;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("paymentControllerV1")
@RequestMapping(PathConstants.API_V1_PAYMENT)
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/initiate")
    public PaymentResponse sendPayment(@RequestBody @NotNull PaymentRequest request, HttpServletRequest httpRequest) throws Exception {
        return paymentService.sendCustodialXrpWithFee(request.userId(), request.destinationAddress(), request.amount());
    }

}