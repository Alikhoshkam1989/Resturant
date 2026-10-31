package nl.alikhoshkam.restaurant.service;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import nl.alikhoshkam.restaurant.model.DiningTable;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ReservationPolicyTest {
  private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneOffset.UTC);
  private final ReservationPolicy policy = new ReservationPolicy(clock);
  private final LocalDateTime tomorrow = LocalDateTime.of(2030, 1, 2, 18, 0);

  private DiningTable table(boolean available, int seats) {
    DiningTable table = new DiningTable();
    table.setAvailable(available);
    table.setSeats(seats);
    return table;
  }

  @Test
  void allocatesTwoHours() {
    // Voorbereiden
    var table = table(true, 4);
    // Uitvoeren
    var end = policy.validate(table, tomorrow, 4);
    // Controleren
    assertThat(end).isEqualTo(tomorrow.plusHours(2));
  }

  @Test
  void rejectsUnavailableTable() {
    // Voorbereiden
    var table = table(false, 4);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.validate(table, tomorrow, 2))
        .hasMessageContaining("not available");
  }

  @Test
  void rejectsOverCapacity() {
    // Voorbereiden
    var table = table(true, 2);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.validate(table, tomorrow, 3))
        .hasMessageContaining("Guest count");
  }

  @Test
  void rejectsEmptyParty() {
    // Voorbereiden
    var table = table(true, 2);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.validate(table, tomorrow, 0))
        .hasMessageContaining("Guest count");
  }

  @Test
  void rejectsPast() {
    // Voorbereiden
    var past = LocalDateTime.now(clock).minusSeconds(1);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.validate(table(true, 4), past, 2))
        .hasMessageContaining("future");
  }

  @Test
  void rejectsCurrentInstant() {
    // Voorbereiden
    var now = LocalDateTime.now(clock);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.validate(table(true, 4), now, 2))
        .hasMessageContaining("future");
  }

  @Test
  void rejectsMissingDate() {
    // Voorbereiden
    LocalDateTime date = null;
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.validate(table(true, 4), date, 2))
        .hasMessageContaining("future");
  }

  @Test
  void permitsOwner() {
    // Voorbereiden
    String subject = "customer-a";
    // Uitvoeren / Controleren
    assertThatCode(() -> policy.requireOwner(subject, subject, false)).doesNotThrowAnyException();
  }

  @Test
  void rejectsAnotherCustomer() {
    // Voorbereiden
    String owner = "customer-a", visitor = "customer-b";
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> policy.requireOwner(owner, visitor, false))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void permitsStaffManagingAnotherCustomer() {
    // Voorbereiden
    String owner = "customer-a", staff = "staff-a";
    // Uitvoeren / Controleren
    assertThatCode(() -> policy.requireOwner(owner, staff, true)).doesNotThrowAnyException();
  }
}
