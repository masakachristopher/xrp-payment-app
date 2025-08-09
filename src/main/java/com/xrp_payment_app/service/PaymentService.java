package com.xrp_payment_app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.primitives.UnsignedInteger;
import com.xrp_payment_app.dto.PaymentResponse;
import com.xrp_payment_app.entity.Transaction;
import com.xrp_payment_app.entity.User;
import com.xrp_payment_app.entity.XrpAccount;
import com.xrp_payment_app.exception.*;
import com.xrp_payment_app.repository.TransactionRepository;
import com.xrp_payment_app.repository.UserRepository;
import com.xrp_payment_app.repository.XrpAccountRepository;
import com.xrp_payment_app.utils.GlobalExceptionHandler;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.crypto.keys.*;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoRequestParams;
import org.xrpl.xrpl4j.model.client.accounts.AccountInfoResult;
import org.xrpl.xrpl4j.model.client.fees.FeeResult;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.transactions.*;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Objects;

import static com.xrp_payment_app.utils.SecureSigning.signWithSeed;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private XrpAccountRepository xrpAccountRepository;
    @Autowired
    private UserRepository userRepository;

    private final BigDecimal PLATFORM_FEE = new BigDecimal("0.2"); // Platform's XRP charge  keep
    private final String PLATFORM_SECRET;
    private final String PLATFORM_ADDRESS;
    private final String xrplHttpUrl;
    private XrplClient xrplClient;

    public PaymentService(
            @Value("${platform.secret}") String platformSecret,
            @Value("${platform.address}") String platformAddress,
            @Value("${xrpl.testnet.http-url}") String xrplHttpUrl
    ) {
        this.PLATFORM_SECRET = platformSecret;
        this.PLATFORM_ADDRESS = platformAddress;
        this.xrplHttpUrl = xrplHttpUrl;
    }

    @PostConstruct
    public void init() {
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(xrplHttpUrl), "XRPL HTTP URL must not be null");
        this.xrplClient = new XrplClient(httpUrl);
        Objects.requireNonNull(PLATFORM_SECRET, "Platform secret must not be null");
        Objects.requireNonNull(PLATFORM_ADDRESS, "Platform address must not be null");
    }

    public PaymentResponse sendXrpWithFee(String userId, String toAddress, BigDecimal amountToSend) throws BadRequestException, NotFoundException, UnprocessedException, JsonRpcClientErrorException {
        try{
            if (userId == null || userId.isEmpty()) {
                logger.error("Invalid User ID: {}", userId);
                throw new BadRequestException(
                        "User ID is required",
                        "Please provide a valid user ID",
                        "MISSING_MANDATORY_FIELD"
                );
            }

            if (amountToSend == null || amountToSend.toString().isEmpty()) {
                logger.error("Invalid Amount: {}", amountToSend);
                throw new BadRequestException(
                        "Amount is required",
                        "Please provide a valid amount",
                        "MISSING_MANDATORY_FIELD"
                );
            }

            if (toAddress == null || toAddress.isEmpty()) {
                logger.error("Invalid destination address: {}", toAddress);
                throw new BadRequestException(
                        "Destination address is required",
                        "Please provide a valid destination address",
                        "MISSING_MANDATORY_FIELD"
                );
            }

            // Todo: Input validations

            // Check user exists
            User user = userRepository.findById(Long.parseLong(userId))
                    .orElseThrow(() -> new NotFoundException(
                            "User not found",
                            "The specified user does not exist.",
                            "USER_NOT_FOUND"
                    ));

            logger.debug("Found User Email:: {}", user.getEmail());

            // Get XRP account
            XrpAccount xrpAccount = xrpAccountRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NotFoundException(
                            "XRP account not found",
                            null,
                            "No XRP account linked to this user",
                            "ACCOUNT_NOT_FOUND"
                    ));

            logger.debug("Calculating the total cost");
            // Calculate total cost
            BigDecimal totalAmount = amountToSend.add(PLATFORM_FEE);

            // Load account info
            logger.debug("Loading the account info");
            AccountInfoRequestParams requestParams = AccountInfoRequestParams.of(Address.of(PLATFORM_ADDRESS));
            AccountInfoResult accountInfo = xrplClient.accountInfo(requestParams);
            UnsignedInteger sequence = accountInfo.accountData().sequence();

            logger.debug("Validating sequence value:: {}", sequence);

            // Validate transaction amount
            logger.debug("Validating transaction amount");
            BigDecimal platformWalletBalance = accountInfo.accountData().balance().toXrp();
            FeeResult feeResult = xrplClient.fee();
            BigDecimal xrpFeeAmount = feeResult.drops().baseFee().toXrp();

            BigDecimal totalCost = amountToSend.add(PLATFORM_FEE).add(xrpFeeAmount);

            // Check platforms's balance
            if (platformWalletBalance.compareTo(totalCost) < 0) {
                throw new UnprocessedException(
                        "Insufficient balance",
                        "Platform's account does not have enough XRP to complete this payment",
                        "INSUFFICIENT_BALANCE"
                );
            }

            // Check user's balance in database
            if (xrpAccount.getBalance().compareTo(totalAmount) < 0) {
                throw new UnprocessedException(
                        "Insufficient balance",
                        "User do not have enough XRP to complete this payment",
                        "INSUFFICIENT_BALANCE"
                );
            }


            // Deduct immediately to prevent double spends
            xrpAccount.setBalance(xrpAccount.getBalance().subtract(totalAmount));
            xrpAccountRepository.save(xrpAccount);

            // Convert amounts to drops
            XrpCurrencyAmount amountInDrops = XrpCurrencyAmount.ofXrp(amountToSend);
            XrpCurrencyAmount feeInDrops = XrpCurrencyAmount.ofDrops(12); // Default fees

            // Extract the keys
            Seed seed = Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(PLATFORM_SECRET));
            KeyPair keyPair = seed.deriveKeyPair();
            PublicKey publicKey = keyPair.publicKey();

            // Create Payment object
            Payment payment = Payment.builder()
                    .account(Address.of(PLATFORM_ADDRESS))
                    .destination(Address.of(toAddress))
                    .amount(amountInDrops)
                    .fee(feeInDrops)
                    .sequence(sequence)
                    .signingPublicKey(publicKey)
                    .build();

            // Sign transaction
            SingleSignedTransaction<Payment> signedPayment = signWithSeed(PLATFORM_SECRET, payment);
            SubmitResult<Payment> result = xrplClient.submit(signedPayment);

            String txHash = result.transactionResult().hash().value();
            logger.debug("Transaction Status: {}", result.engineResult());
            if (result.engineResult().equals("tesSUCCESS")) {
                logger.debug("XRP Payment sent successfully");

                // Record transaction
                Transaction transaction = new Transaction();
                transaction.setXrpAccountId(xrpAccount.getId());
                transaction.setDestinationAddress(toAddress);
                transaction.setAmount(totalAmount);
                transaction.setTransactionHash(result.transactionResult().hash().value());
                transaction.setStatus("COMPLETED");
                transactionRepository.save(transaction);
                logger.debug("Transaction saved successfully into the DB");

                // Update xrp_accounts balance

                return new PaymentResponse("SUCCESS", txHash, "Payment of " + amountToSend + " XRP sent. Fee kept: " + PLATFORM_FEE + " XRP");
            } else {
                // Roll back balance deduction
                // TO DO: Only roll back if status is not wrong address
                xrpAccount.setBalance(xrpAccount.getBalance().add(totalAmount));
                userRepository.save(user);
                logger.debug("Roll back user balance successfully into the DB");

                // Record transaction
                Transaction transaction = new Transaction();
                transaction.setXrpAccountId(xrpAccount.getId());
                transaction.setDestinationAddress(toAddress);
                transaction.setAmount(totalAmount);
                transaction.setTransactionHash(result.transactionResult().hash().value());
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                logger.debug("Failed transaction saved successfully into the DB");

                throw new UnprocessedException("Failed to initiate payment due to "+ result.transactionResult().status(),"Failure to process the transaction","XRP_LEDGER_ERROR");
//                return new PaymentResponse("FAILED", null, "Error: " +result.engineResult() + " " + result.engineResultMessage());
            }
        }
        catch (BadRequestException | UnprocessedException | NotFoundException  | JsonRpcClientErrorException e) {
            throw e;
        }
        catch (Exception e) {
            throw new XrpServiceException(
                    "Failed to initiate payment: "+ e.getMessage(),
                    null,
                    "We couldn’t process your payment request at this time. Please try again later",
                    "SYSTEM_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}

