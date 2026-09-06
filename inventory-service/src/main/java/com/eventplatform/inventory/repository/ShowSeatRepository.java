package com.eventplatform.inventory.repository;

import com.eventplatform.inventory.domain.SeatState;
import com.eventplatform.inventory.domain.ShowSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    List<ShowSeat> findByShowIdOrderByRowLabelAscSeatNumberAsc(UUID showId);

    boolean existsByShowId(UUID showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.showId = :showId and s.seatId in :seatIds order by s.seatId")
    List<ShowSeat> lockSeats(@Param("showId") UUID showId, @Param("seatIds") Collection<UUID> seatIds);

    List<ShowSeat> findByLockId(UUID lockId);

    List<ShowSeat> findByBookingId(UUID bookingId);

    List<ShowSeat> findByStateAndLockExpiresAtBefore(SeatState state, Instant cutoff);
}
