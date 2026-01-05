package com.xrp_payment_app.repository;

import com.xrp_payment_app.entity.User;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByuserName(String userName);
}