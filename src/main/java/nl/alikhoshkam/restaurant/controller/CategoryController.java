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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
  private final CatalogService service;

  @GetMapping
  public List<CategoryView> list() {
    return service.categories();
  }

  @GetMapping("/{id}")
  public CategoryView get(@PathVariable Long id) {
    return service.category(id);
  }

  @PostMapping
  public ResponseEntity<CategoryView> create(@Valid @RequestBody CategoryInput input) {
    var result = service.saveCategory(null, input);
    return ResponseEntity.created(URI.create("/api/categories/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public CategoryView update(@PathVariable Long id, @Valid @RequestBody CategoryInput input) {
    return service.saveCategory(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteCategory(id);
  }
}
