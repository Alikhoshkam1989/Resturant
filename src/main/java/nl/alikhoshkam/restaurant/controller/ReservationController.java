package nl.alikhoshkam.restaurant.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.alikhoshkam.restaurant.dto.Requests.*;
import nl.alikhoshkam.restaurant.dto.Views.*;
import nl.alikhoshkam.restaurant.service.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {
  private final ReservationService service;

  @GetMapping
  public List<ReservationView> list(Authentication auth) {
    return service.list(auth.getName(), staff(auth));
  }

  @GetMapping("/{id}")
  public ReservationView get(@PathVariable Long id, Authentication auth) {
    return service.get(id, auth.getName(), staff(auth));
  }

  @PostMapping
  public ResponseEntity<ReservationView> create(
      @Valid @RequestBody ReservationInput input, Authentication auth) {
    var result = service.create(input, auth.getName());
    return ResponseEntity.created(URI.create("/api/reservations/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public ReservationView update(
      @PathVariable Long id, @Valid @RequestBody ReservationInput input, Authentication auth) {
    return service.update(id, input, auth.getName(), staff(auth));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancel(@PathVariable Long id, Authentication auth) {
    service.cancel(id, auth.getName(), staff(auth));
  }

  private boolean staff(Authentication auth) {
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"));
  }
}
