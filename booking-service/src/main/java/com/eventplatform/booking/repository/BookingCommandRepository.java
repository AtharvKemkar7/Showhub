package com.eventplatform.booking.repository;

import com.eventplatform.booking.domain.BookingCommand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingCommandRepository extends JpaRepository<BookingCommand, String> {
}
