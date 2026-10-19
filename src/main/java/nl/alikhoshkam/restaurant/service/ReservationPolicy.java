package nl.alikhoshkam.restaurant.service;

import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import nl.alikhoshkam.restaurant.dto.*;
import nl.alikhoshkam.restaurant.dto.Requests.*;
import nl.alikhoshkam.restaurant.dto.Views.*;
import nl.alikhoshkam.restaurant.exception.ApiException;
import nl.alikhoshkam.restaurant.model.*;
import nl.alikhoshkam.restaurant.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationPolicy {
  private final Clock clock;

  public LocalDateTime validate(DiningTable table, LocalDateTime start, int guests) {
    if (!table.isAvailable()) throw ApiException.conflict("Table is not available");
    if (guests < 1 || guests > table.getSeats())
      throw ApiException.badRequest("Guest count exceeds table capacity");
    if (start == null || !start.isAfter(LocalDateTime.now(clock)))
      throw ApiException.badRequest("Reservation must be in the future");
    return start.plusHours(2);
  }

  public void requireOwner(String owner, String subject, boolean staff) {
    if (!staff && !Objects.equals(owner, subject))
      throw new org.springframework.security.access.AccessDeniedException(
          "Reservation belongs to another customer");
  }
}
