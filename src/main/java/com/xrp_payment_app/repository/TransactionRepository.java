package com.xrp_payment_app.repository;

import com.xrp_payment_app.entity.Transaction;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Transaction findByPaymentReferenceAndPaymentType(String paymentReference, String paymentType);
    List<Transaction> findByRequestId(String requestId);
}