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
public class TableService {
  private final DiningTableRepository tables;
  private final ReservationRepository reservations;
  private final RestaurantOrderRepository orders;

  public List<TableView> list() {
    return tables.findAll().stream().map(Views::of).toList();
  }

  public TableView get(Long id) {
    return Views.of(find(id));
  }

  public TableView save(Long id, TableInput input) {
    DiningTable t =
        id == null
            ? new DiningTable()
            : tables.findLockedById(id).orElseThrow(() -> ApiException.notFound("Table"));
    if (id != null && input.seats() < t.getSeats() && reservations.existsByDiningTableId(id))
      throw ApiException.conflict("Cannot reduce capacity while reservations are linked");
    t.setTableNumber(input.tableNumber());
    t.setSeats(input.seats());
    t.setAvailable(input.available());
    return Views.of(tables.save(t));
  }

  public void delete(Long id) {
    DiningTable t = find(id);
    if (reservations.existsByDiningTableId(id) || orders.existsByDiningTableId(id))
      throw ApiException.conflict("Table is in use");
    tables.delete(t);
  }

  private DiningTable find(Long id) {
    return tables.findById(id).orElseThrow(() -> ApiException.notFound("Table"));
  }
}
