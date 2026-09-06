package com.eventplatform.payment.repository;

import com.eventplatform.payment.domain.PaymentCommand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCommandRepository extends JpaRepository<PaymentCommand, String> {
}
