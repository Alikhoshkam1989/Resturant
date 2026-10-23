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
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
  private final InvoiceService service;

  @GetMapping
  public List<InvoiceView> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public InvoiceView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<InvoiceView> create(@Valid @RequestBody InvoiceInput input) {
    var result = service.create(input);
    return ResponseEntity.created(URI.create("/api/invoices/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public InvoiceView update(@PathVariable Long id, @Valid @RequestBody PaymentInput input) {
    return service.payment(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
