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
public class ReservationService {
  private final ReservationRepository reservations;
  private final DiningTableRepository tables;
  private final ReservationPolicy policy;

  public List<ReservationView> list(String subject, boolean staff) {
    return (staff
            ? reservations.findAll()
            : reservations.findByCustomerSubjectOrderByStartsAt(subject))
        .stream().map(Views::of).toList();
  }

  public ReservationView get(Long id, String subject, boolean staff) {
    return Views.of(owned(id, subject, staff));
  }

  public ReservationView create(ReservationInput input, String subject) {
    Reservation r = new Reservation();
    r.setCustomerSubject(subject);
    apply(r, input);
    return Views.of(reservations.save(r));
  }

  public ReservationView update(Long id, ReservationInput input, String subject, boolean staff) {
    Reservation r = owned(id, subject, staff);
    if (r.isCancelled())
      throw ApiException.conflict("Cancelled reservation cannot be changed");
    apply(r, input);
    return Views.of(reservations.save(r));
  }

  public void cancel(Long id, String subject, boolean staff) {
    Reservation r = owned(id, subject, staff);
    r.setCancelled(true);
    reservations.save(r);
  }

  private Reservation owned(Long id, String subject, boolean staff) {
    Reservation r =
        reservations.findById(id).orElseThrow(() -> ApiException.notFound("Reservation"));
    policy.requireOwner(r.getCustomerSubject(), subject, staff);
    return r;
  }

  private void apply(Reservation r, ReservationInput input) {
    DiningTable table =
        tables.findLockedById(input.tableId()).orElseThrow(() -> ApiException.notFound("Table"));
    LocalDateTime end = policy.validate(table, input.startsAt(), input.guests());
    if (reservations.overlaps(table.getId(), input.startsAt(), end, r.getId()))
      throw ApiException.conflict("Table is reserved during this time slot");
    r.setDiningTable(table);
    r.setCustomerName(input.customerName());
    r.setStartsAt(input.startsAt());
    r.setEndsAt(end);
    r.setGuests(input.guests());
    r.setRemarks(input.remarks());
  }
}
