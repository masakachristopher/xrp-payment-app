package com.xrp_payment_app.repository;

import com.xrp_payment_app.entity.XrpAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface XrpAccountRepository extends JpaRepository<XrpAccount, Long> {
    Optional<XrpAccount> findByUserId(Long userId);

    Optional<XrpAccount> findByXrpAddress(String xrpAddress);

    Optional<XrpAccount> findByUserIdAndXrpAddress(Long userId, String xrpAddress);

    List<XrpAccount> findAllByUserId(Long userId);

}
