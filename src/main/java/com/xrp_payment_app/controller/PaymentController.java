package com.xrp_payment_app.controller;

import com.xrp_payment_app.dto.PaymentRequest;
import com.xrp_payment_app.dto.PaymentResponse;
import com.xrp_payment_app.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/initiate")
    public PaymentResponse sendPayment(@RequestBody PaymentRequest request) throws Exception {
        return paymentService.sendXrpWithFee(request.destinationAddress(), request.amount());
    }
}