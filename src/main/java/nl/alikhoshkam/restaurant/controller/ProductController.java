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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
  private final CatalogService service;

  @GetMapping
  public List<ProductView> list() {
    return service.products();
  }

  @GetMapping("/{id}")
  public ProductView get(@PathVariable Long id) {
    return service.product(id);
  }

  @PostMapping
  public ResponseEntity<ProductView> create(@Valid @RequestBody ProductInput input) {
    var result = service.saveProduct(null, input);
    return ResponseEntity.created(URI.create("/api/products/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public ProductView update(@PathVariable Long id, @Valid @RequestBody ProductInput input) {
    return service.saveProduct(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteProduct(id);
  }
}
