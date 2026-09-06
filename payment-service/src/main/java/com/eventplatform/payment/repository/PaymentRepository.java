package com.eventplatform.payment.repository;

import com.eventplatform.payment.domain.Payment;
import com.eventplatform.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingIdAndStatusIn(UUID bookingId, List<PaymentStatus> statuses);

    Optional<Payment> findByProviderRef(String providerRef);

    List<Payment> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
