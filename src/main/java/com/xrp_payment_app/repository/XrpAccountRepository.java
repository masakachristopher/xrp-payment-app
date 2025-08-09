package com.xrp_payment_app.repository;

import com.xrp_payment_app.entity.XrpAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface XrpAccountRepository extends JpaRepository<XrpAccount, Long> {
    Optional<XrpAccount> findByUserId(Long userId);
}
