package nl.alikhoshkam.restaurant.repository;

import java.time.LocalDateTime;
import java.util.*;
import nl.alikhoshkam.restaurant.model.Reservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
  List<Reservation> findByCustomerSubjectOrderByStartsAt(String subject);

  boolean existsByDiningTableId(Long tableId);

  @Query(
      "select count(r) > 0 from Reservation r where r.diningTable.id = :tableId and r.cancelled ="
          + " false and r.startsAt < :end and r.endsAt > :start and (:excluded is null or r.id <>"
          + " :excluded)")
  boolean overlaps(
      @Param("tableId") Long tableId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("excluded") Long excluded);
}
