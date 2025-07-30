package com.xrp_payment_app.service;

import com.google.common.primitives.UnsignedInteger;
import com.xrp_payment_app.dto.PaymentResponse;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.transactions.*;


import java.math.BigDecimal;
import java.util.Objects;

import static com.xrp_payment_app.utils.SecureSigning.signWithSeed;

@Service
public class PaymentService {

    private final XrplClient xrplClient = new XrplClient(Objects.requireNonNull(HttpUrl.parse("${XRPL_TESTNET_HTTP_URL}")));
    // Testnet
//    private final String PLATFORM_SECRET = "sEdV..."; // Replace with your s-prefixed secret
//    private final String PLATFORM_ADDRESS = "rXXXX..."; // Your platform wallet address
    @Value("${PLATFORM_SECRET}")
    private final String PLATFORM_SECRET;

    @Value("${PLATFORM_ADDRESS}")
    private final String PLATFORM_ADDRESS;

    private final BigDecimal PLATFORM_FEE = new BigDecimal("0.2"); // Platform's XRP charge  keep

    public PaymentService(String platformSecret, String platformAddress) {
        PLATFORM_SECRET = platformSecret;
        PLATFORM_ADDRESS = platformAddress;
    }

    public PaymentResponse sendXrpWithFee(String toAddress, BigDecimal amountToSend) throws Exception {
        try{
            // Calculate total cost
            BigDecimal totalAmount = amountToSend.add(PLATFORM_FEE);

            // Load account info
            AccountInfoRequestParams requestParams = AccountInfoRequestParams.of(Address.of(PLATFORM_ADDRESS));
            AccountInfoResult accountInfo = xrplClient.accountInfo(requestParams);
            UnsignedInteger sequence = accountInfo.accountData().sequence();

            // Validate transaction amount
            BigDecimal platformWalletBalance = accountInfo.accountData().balance().toXrp();
            FeeResult feeResult = xrplClient.fee();
//            BigDecimal xrpFeeAmount = new BigDecimal("0.000012"); //Manual set to 000012 XRP
            BigDecimal xrpFeeAmount = feeResult.drops().baseFee().toXrp();

            BigDecimal totalCost = amountToSend.add(PLATFORM_FEE).add(xrpFeeAmount);

            if (platformWalletBalance.compareTo(totalCost) < 0) {
                throw new Exception("Platform wallet has insufficient XRP. Please contact support.");
            }

//            if (user.getBalance().compareTo(totalAmount) < 0) {
//                throw new Exception("Insufficient balance");
//            }
//
//            // Deduct immediately to prevent double spends
//            user.setBalance(user.getBalance().subtract(totalAmount));
//            userRepository.save(user);

            // Convert amounts to drops
            XrpCurrencyAmount amountInDrops = XrpCurrencyAmount.ofXrp(amountToSend);
            XrpCurrencyAmount feeInDrops = XrpCurrencyAmount.ofDrops(12); // Default fees

            // Create Payment object
            Payment payment = Payment.builder()
                    .account(Address.of(PLATFORM_ADDRESS))
                    .destination(Address.of(toAddress))
                    .amount(amountInDrops)
                    .fee(feeInDrops)
                    .sequence(sequence)
                    .signingPublicKey(null) // will be set during signing
                    .build();

            // Sign transaction
            SingleSignedTransaction<Payment> signedPayment = signWithSeed(PLATFORM_SECRET, payment);
            SubmitResult<Payment> result = xrplClient.submit(signedPayment);

            String txHash = result.transactionResult().hash().value();
            return new PaymentResponse("SUCCESS", txHash, "Payment of " + amountToSend + " XRP sent. Fee kept: " + PLATFORM_FEE + " XRP");
        } catch (Exception e) {
            return new PaymentResponse("FAILED", null, "Error: " + e.getMessage());
        }
    }
}

