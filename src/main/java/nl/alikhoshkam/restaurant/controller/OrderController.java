package nl.alikhoshkam.restaurant.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import nl.alikhoshkam.restaurant.dto.Requests.*;
import nl.alikhoshkam.restaurant.dto.Views.*;
import nl.alikhoshkam.restaurant.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService service;

  @GetMapping
  public List<OrderView> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public OrderView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<OrderView> create(@Valid @RequestBody OrderInput input) {
    var result = service.create(input);
    return ResponseEntity.created(URI.create("/api/orders/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public OrderView update(@PathVariable Long id, @Valid @RequestBody OrderInput input) {
    return service.update(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
