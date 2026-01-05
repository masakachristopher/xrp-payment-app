package com.xrp_payment_app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.UnsignedInteger;
import com.xrp_payment_app.dto.PaymentResponse;
import com.xrp_payment_app.dto.PaymentSignResponse;
import com.xrp_payment_app.entity.Transaction;
import com.xrp_payment_app.entity.User;
import com.xrp_payment_app.entity.XrpAccount;
import com.xrp_payment_app.exception.*;
import com.xrp_payment_app.repository.TransactionRepository;
import com.xrp_payment_app.repository.UserRepository;
import com.xrp_payment_app.repository.XrpAccountRepository;
import com.xrp_payment_app.utils.ExtractKeyPair;
import com.xrp_payment_app.utils.GlobalExceptionHandler;
import com.xrp_payment_app.utils.XamanPayloadBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.crypto.keys.*;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.model.client.transactions.SubmitResult;
import org.xrpl.xrpl4j.model.ledger.AccountRootObject;
import org.xrpl.xrpl4j.model.transactions.*;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.xrp_payment_app.utils.SecureSigning.signWithSeed;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final BigDecimal PLATFORM_FEE = new BigDecimal("0.2"); // Platform's XRP charge  keep
    private final String PLATFORM_SECRET;
    private final String PLATFORM_ADDRESS;

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private XrpAccountRepository xrpAccountRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private final XrplClientService xrplClientService;
    @Autowired
    private final XamanClientService xamanClientService;
    @Autowired
    private XamanPayloadBuilder xamanPayloadBuilder;

    public PaymentService(
            @Value("${platform.secret}") String platformSecret,
            @Value("${platform.address}") String platformAddress,
            XrplClientService xrplClientService,
            XamanClientService xamanClientService
    ) {
        this.PLATFORM_SECRET = platformSecret;
        this.PLATFORM_ADDRESS = platformAddress;
        this.xrplClientService = xrplClientService;
        this.xamanClientService = xamanClientService;
    }

    @PostConstruct
    public void init() {
        Objects.requireNonNull(PLATFORM_SECRET, "Platform secret must not be null");
        Objects.requireNonNull(PLATFORM_ADDRESS, "Platform address must not be null");
    }

    public PaymentResponse sendCustodialXrpWithFee(String userId, String toAddress, BigDecimal amountToSend) throws BadRequestException, NotFoundException, UnprocessedException, JsonRpcClientErrorException {
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

            // Get XRP account
            XrpAccount senderAccount = xrpAccountRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new NotFoundException(
                            "XRP account not found",
                            "No XRP account linked to this user",
                            "ACCOUNT_NOT_FOUND"
                    ));

            BigDecimal xrpFeeAmount = xrplClientService.getFees().drops().baseFee().toXrp();

            logger.info("Calculating the total cost");
            // Calculate total cost
            // BigDecimal totalAmount = amountToSend.add(PLATFORM_FEE);
            BigDecimal userPaymentAmount = amountToSend;
            BigDecimal platformFee = PLATFORM_FEE;
            BigDecimal networkFee = xrpFeeAmount;

            BigDecimal userTotalDebit = userPaymentAmount.add(platformFee).add(networkFee);
            BigDecimal platformTotalDebit = userPaymentAmount.add(networkFee);
            // Load account info
            logger.info("Loading the account info");
            AccountRootObject account = xrplClientService.getAccountData(PLATFORM_ADDRESS);
            UnsignedInteger sequence =  account.sequence();
            // UnsignedInteger userPaymentSequence =  sequence;
            // UnsignedInteger feePaymentSequence =  sequence.plus(UnsignedInteger.ONE);

            logger.info("Platform account sequence: {}", sequence);

            // Validate transaction amount
            logger.info("Validating transaction amount");
            BigDecimal platformWalletBalance = account.balance().toXrp();
            // BigDecimal xrpFeeAmount = xrplClientService.getFees().drops().baseFee().toXrp();

            // BigDecimal totalCost = amountToSend.add(PLATFORM_FEE).add(xrpFeeAmount);

            // Check platforms's balance
            if (platformWalletBalance.compareTo(platformTotalDebit) < 0) {
                throw new UnprocessedException(
                        "Insufficient balance",
                        "Platform's account does not have enough XRP to complete this payment",
                        "INSUFFICIENT_BALANCE"
                );
            }

            // Load sender's account info
            logger.info("Loading the account info");
            BigDecimal senderBalance = xrplClientService.getBalance(senderAccount.getXrpAddress());

            // Check user's balance
            if (senderBalance.compareTo(userTotalDebit) < 0) {
                throw new UnprocessedException(
                        "Insufficient balance",
                        "User do not have enough XRP to complete this payment",
                        "INSUFFICIENT_BALANCE"
                );
            }
            // Convert amounts to drops
            XrpCurrencyAmount amountInDrops = XrpCurrencyAmount.ofXrp(amountToSend);
            // XrpCurrencyAmount paymentAmountInDrops = XrpCurrencyAmount.ofXrp(amountToSend);
            // XrpCurrencyAmount feePaymentAmountInDrops = XrpCurrencyAmount.ofXrp(PLATFORM_FEE);
            XrpCurrencyAmount feeInDrops = XrpCurrencyAmount.ofXrp(xrpFeeAmount);

            // Extract the public key
            PublicKey publicKey = null;
            // PrivateKey privateKey = null;

            ExtractKeyPair extractKeyPair = new ExtractKeyPair();
            KeyPair keyPair = extractKeyPair.deriveKeyPairFromSecret(PLATFORM_SECRET);
            publicKey = keyPair.publicKey();
            // privateKey = keyPair.privateKey();

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
            SubmitResult<Payment> result = xrplClientService.getXrplClient().submit(signedPayment);

            String txHash = result.transactionResult().hash().value();
            logger.debug("Transaction Status: {}", result.engineResult());
            if (result.engineResult().equals("tesSUCCESS")) {
                logger.debug("XRP Payment sent successfully");

                // Record transaction
                Transaction transaction = new Transaction();
                transaction.setXrpAccountId(senderAccount.getId());
                transaction.setDestinationAddress(toAddress);
                transaction.setAmount(amountToSend);
                transaction.setPlatformFee(PLATFORM_FEE);
                transaction.setNetworkFee(networkFee);
                transaction.setTransactionHash(result.transactionResult().hash().value());
                transaction.setStatus("COMPLETED");
                transactionRepository.save(transaction);
                logger.debug("Transaction saved successfully into the DB and return response to client");

                return new PaymentResponse("SUCCESS", txHash, "Payment of " + amountToSend + " XRP sent. Fee kept: " + PLATFORM_FEE + " XRP");
            } else {

                // Record transaction
                Transaction transaction = new Transaction();
                transaction.setXrpAccountId(senderAccount.getId());
                transaction.setDestinationAddress(toAddress);
                transaction.setAmount(amountToSend);
                transaction.setPlatformFee(PLATFORM_FEE);
                transaction.setNetworkFee(networkFee);
                transaction.setTransactionHash(result.transactionResult().hash().value());
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                logger.debug("Failed transaction saved successfully into the DB");

                throw new UnprocessedException(
                        "Failed to initiate payment due to "+ result.engineResult(),
                        "Failure to process the transaction: " + result.engineResultMessage(),
                        "XRP_LEDGER_ERROR");
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

    public PaymentSignResponse sendNonCustodialXrpBatchWithFee(String requestId, String userName, String senderAddress, String toAddress, BigDecimal amountToSend) throws BadRequestException, NotFoundException, UnprocessedException, JsonRpcClientErrorException {
        try{
            if (senderAddress == null || senderAddress.isEmpty()) {
                logger.error("Invalid User ID: {}", senderAddress);
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

            // Check duplicate requestId
            List<Transaction> txnList = transactionRepository.findByRequestId(requestId);
            if (!txnList.isEmpty()) {
                logger.error("Duplicate Request ID: {}", requestId);
                throw new BadRequestException(
                        "Duplicate Request ID",
                        "A transaction with this Request ID already exists",
                        "DUPLICATE_REQUEST_ID"
                );
            }  

            XrpAccount xrpAccount = xrpAccountRepository.findByXrpAddress(senderAddress).orElse(null);
            Long userId = null;
            if (xrpAccount == null) { 
       
                User user = userRepository.findByuserName(userName).orElse(null);

                if (user != null) {
                    userId = user.getId();
                    logger.info("User found with ID: {}", userId); 
                    XrpAccount newAccount = new XrpAccount();
                    newAccount.setUserId(userId);
                    newAccount.setXrpAddress(senderAddress);
                    XrpAccount savedAccount = xrpAccountRepository.save(newAccount);
                    logger.info("Linked new XRP address to existing user: userId={}, accountId={}", userId, savedAccount.getId());
                } else {
                    logger.info("No user found with userName: {}", userName);
                    User newUser = new User();
                    newUser.setUserName("user-" + UUID.randomUUID().toString().substring(0, 8));
                    User savedUser = userRepository.save(newUser);
                    userId = savedUser.getId();

                    XrpAccount newAccount = new XrpAccount();
                    newAccount.setUserId(userId);
                    newAccount.setXrpAddress(senderAddress);
                    XrpAccount savedAccount = xrpAccountRepository.save(newAccount);

                    logger.info("New user and XRP account created: userId={}, accountId={}", userId, savedAccount.getId());
                }
            } else {
                logger.info("User found with ID: {}", xrpAccount.getUserId()); 
                userId = xrpAccount.getUserId();
            }
        
     
            XrpAccount senderAccount = xrpAccountRepository.findByUserIdAndXrpAddress(userId, senderAddress)
                .orElseThrow(() -> new NotFoundException(
                        "XRP account not found",
                        "No XRP account linked to this user",
                        "ACCOUNT_NOT_FOUND"
                ));          

            // Load account info
            logger.info("Loading the account info");
            AccountRootObject account = xrplClientService.getAccountData(senderAddress);
            UnsignedInteger sequence =  account.sequence();
            UnsignedInteger userPaymentSequence =  sequence;
            UnsignedInteger feePaymentSequence =  sequence.plus(UnsignedInteger.ONE);

            logger.info("Platform account sequence: {}", sequence);

            // Validate transaction amount
            logger.info("Validating transaction amount");
            BigDecimal xrpFeeAmount = xrplClientService.getFees().drops().baseFee().toXrp();
            BigDecimal paymentTxFee = xrpFeeAmount;
            BigDecimal platformFeeTxFee = xrpFeeAmount;

            BigDecimal totalCost = amountToSend
                                        .add(PLATFORM_FEE)
                                        .add(paymentTxFee)
                                        .add(platformFeeTxFee);

            // Load sender's account info
            logger.info("Loading the account info");
            BigDecimal senderBalance = xrplClientService.getBalance(senderAddress);

            if (senderBalance.compareTo(totalCost) < 0) {
                throw new UnprocessedException(
                        "Insufficient balance",
                        "User do not have enough XRP to complete this payment",
                        "INSUFFICIENT_BALANCE"
                );
            }
            // Convert amounts to drops
            XrpCurrencyAmount paymentAmountInDrops = XrpCurrencyAmount.ofXrp(amountToSend);
            XrpCurrencyAmount feePaymentAmountInDrops = XrpCurrencyAmount.ofXrp(PLATFORM_FEE);
            XrpCurrencyAmount feeInDrops = XrpCurrencyAmount.ofXrp(xrpFeeAmount);

            ObjectNode userPayload = xamanPayloadBuilder.buildPaymentPayload(
                    toAddress,
                    paymentAmountInDrops.value().longValue(),
                    feeInDrops.value().longValue(),
                    userPaymentSequence.longValue(), 
                    false
            );

             ObjectNode feePayload = xamanPayloadBuilder.buildPaymentPayload(
                    PLATFORM_ADDRESS,
                    feePaymentAmountInDrops.value().longValue(),
                    feeInDrops.value().longValue(),
                    feePaymentSequence.longValue(), 
                    false
            );

            JsonNode userResponse = xamanClientService.callXamanCreatePayload(userPayload);
            JsonNode feeResponse = xamanClientService.callXamanCreatePayload(feePayload);

            String userPayloadUuid = userResponse.get("uuid").asText();
            String userXamanRedirectUrl = userResponse.get("next").get("always").asText();

            String feePayloadUuid = feeResponse.get("uuid").asText();
            String feeXamanRedirectUrl = feeResponse.get("next").get("always").asText();

            if (userXamanRedirectUrl == null || userXamanRedirectUrl.isEmpty() || userPayloadUuid == null || userPayloadUuid.isEmpty()) {
                throw new UnprocessedException(
                        "Failed to create Xaman payload",
                        "Could not get a valid response from Xaman",
                        "XAMAN_PAYLOAD_ERROR"
                );
            }

             if (feeXamanRedirectUrl == null || feeXamanRedirectUrl.isEmpty() || feePayloadUuid == null || feePayloadUuid.isEmpty()) {
                throw new UnprocessedException(
                        "Failed to create Xaman payload",
                        "Could not get a valid response from Xaman",
                        "XAMAN_PAYLOAD_ERROR"
                );
            }

            // Save transaction as INITIATED in DB
            Transaction paymentTransaction = new Transaction();
            paymentTransaction.setXrpAccountId(senderAccount.getId());
            paymentTransaction.setDestinationAddress(toAddress);
            paymentTransaction.setAmount(amountToSend);
            paymentTransaction.setPlatformFee(PLATFORM_FEE);
            paymentTransaction.setNetworkFee(xrpFeeAmount);
            paymentTransaction.setStatus("INITIATED");
            paymentTransaction.setPaymentReference(userPayloadUuid);
            paymentTransaction.setPaymentType("USER_PAYMENT");
            paymentTransaction.setRequestId(requestId);
            transactionRepository.save(paymentTransaction);

            Transaction feeTransaction = new Transaction();
            feeTransaction.setXrpAccountId(senderAccount.getId());
            feeTransaction.setDestinationAddress(PLATFORM_ADDRESS);
            feeTransaction.setAmount(PLATFORM_FEE);
            feeTransaction.setPlatformFee(new BigDecimal("0.0"));
            feeTransaction.setNetworkFee(xrpFeeAmount);
            feeTransaction.setStatus("INITIATED");
            feeTransaction.setPaymentReference(feePayloadUuid);
            feeTransaction.setPaymentType("PLATFORM_FEE");
            feeTransaction.setRequestId(requestId);
            transactionRepository.save(feeTransaction);

            logger.info("Transaction saved as INITIATED in DB with payment request: {}", requestId);

            return new PaymentSignResponse(requestId, "PENDING", userPayloadUuid, userXamanRedirectUrl, feePayloadUuid, feeXamanRedirectUrl, "Payment is pending user approval via Xaman");

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

    public PaymentSignResponse sendNonCustodialXrpWithFee(String requestId, String userName, String senderAddress, String toAddress, BigDecimal amountToSend) throws BadRequestException, NotFoundException, UnprocessedException, JsonRpcClientErrorException {
        try{
            if (senderAddress == null || senderAddress.isEmpty()) {
                logger.error("Invalid User ID: {}", senderAddress);
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

            // Check duplicate requestId
            List<Transaction> txnList = transactionRepository.findByRequestId(requestId);
            if (!txnList.isEmpty()) {
                logger.error("Duplicate Request ID: {}", requestId);
                throw new BadRequestException(
                        "Duplicate Request ID",
                        "A transaction with this Request ID already exists",
                        "DUPLICATE_REQUEST_ID"
                );
            }  

            XrpAccount xrpAccount = xrpAccountRepository.findByXrpAddress(senderAddress).orElse(null);
            Long userId = null;
            if (xrpAccount == null) { 
       
                User user = userRepository.findByuserName(userName).orElse(null);

                if (user != null) {
                    userId = user.getId();
                    logger.info("User found with ID: {}", userId); 
                    XrpAccount newAccount = new XrpAccount();
                    newAccount.setUserId(userId);
                    newAccount.setXrpAddress(senderAddress);
                    XrpAccount savedAccount = xrpAccountRepository.save(newAccount);
                    logger.info("Linked new XRP address to existing user: userId={}, accountId={}", userId, savedAccount.getId());
                } else {
                    logger.info("No user found with userName: {}", userName);
                    User newUser = new User();
                    newUser.setUserName("user-" + UUID.randomUUID().toString().substring(0, 8));
                    User savedUser = userRepository.save(newUser);
                    userId = savedUser.getId();

                    XrpAccount newAccount = new XrpAccount();
                    newAccount.setUserId(userId);
                    newAccount.setXrpAddress(senderAddress);
                    XrpAccount savedAccount = xrpAccountRepository.save(newAccount);

                    logger.info("New user and XRP account created: userId={}, accountId={}", userId, savedAccount.getId());
                }
            } else {
                logger.info("User found with ID: {}", xrpAccount.getUserId()); 
                userId = xrpAccount.getUserId();
            }
        
     
            XrpAccount senderAccount = xrpAccountRepository.findByUserIdAndXrpAddress(userId, senderAddress)
                .orElseThrow(() -> new NotFoundException(
                        "XRP account not found",
                        "No XRP account linked to this user",
                        "ACCOUNT_NOT_FOUND"
                ));          

            // Load account info
            logger.info("Loading the account info");
            AccountRootObject account = xrplClientService.getAccountData(senderAddress);
            UnsignedInteger sequence =  account.sequence();
            UnsignedInteger userPaymentSequence =  sequence;

            logger.info("Platform account sequence: {}", sequence);

            // Validate transaction amount
            logger.info("Validating transaction amount");
            BigDecimal xrpFeeAmount = xrplClientService.getFees().drops().baseFee().toXrp();
            BigDecimal paymentTxFee = xrpFeeAmount;

            BigDecimal totalCost = amountToSend
                                        .add(PLATFORM_FEE)
                                        .add(paymentTxFee);
                                        // .add(platformFeeTxFee);

            // Load sender's account info
            logger.info("Loading the account info");
            BigDecimal senderBalance = xrplClientService.getBalance(senderAddress);

            if (senderBalance.compareTo(totalCost) < 0) {
                throw new UnprocessedException(
                        "Insufficient balance",
                        "User do not have enough XRP to complete this payment",
                        "INSUFFICIENT_BALANCE"
                );
            }
            // Convert amounts to drops
            XrpCurrencyAmount paymentAmountInDrops = XrpCurrencyAmount.ofXrp(amountToSend);
            XrpCurrencyAmount feeInDrops = XrpCurrencyAmount.ofXrp(xrpFeeAmount);

            ObjectNode userPayload = xamanPayloadBuilder.buildPaymentPayload(
                    toAddress,
                    paymentAmountInDrops.value().longValue(),
                    feeInDrops.value().longValue(),
                    userPaymentSequence.longValue(), 
                    false
            );

            JsonNode userResponse = xamanClientService.callXamanCreatePayload(userPayload);

            String userPayloadUuid = userResponse.get("uuid").asText();
            String userXamanRedirectUrl = userResponse.get("next").get("always").asText();

            if (userXamanRedirectUrl == null || userXamanRedirectUrl.isEmpty() || userPayloadUuid == null || userPayloadUuid.isEmpty()) {
                throw new UnprocessedException(
                        "Failed to create Xaman payload",
                        "Could not get a valid response from Xaman",
                        "XAMAN_PAYLOAD_ERROR"
                );
            }

            // Save transaction as INITIATED in DB
            Transaction paymentTransaction = new Transaction();
            paymentTransaction.setXrpAccountId(senderAccount.getId());
            paymentTransaction.setDestinationAddress(toAddress);
            paymentTransaction.setAmount(amountToSend);
            paymentTransaction.setPlatformFee(PLATFORM_FEE);
            paymentTransaction.setNetworkFee(xrpFeeAmount);
            paymentTransaction.setStatus("INITIATED");
            paymentTransaction.setPaymentReference(userPayloadUuid);
            paymentTransaction.setPaymentType("USER_PAYMENT");
            paymentTransaction.setRequestId(requestId);
            transactionRepository.save(paymentTransaction);

            logger.info("Transaction saved as INITIATED in DB with payment request: {}", requestId);

            return new PaymentSignResponse(requestId, "PENDING", userPayloadUuid, userXamanRedirectUrl, null, null, "Payment is pending user approval via Xaman");

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

