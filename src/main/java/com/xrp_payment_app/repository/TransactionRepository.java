package com.xrp_payment_app.repository;

import com.xrp_payment_app.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}