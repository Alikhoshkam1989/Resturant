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
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {
  private final TableService service;

  @GetMapping
  public List<TableView> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public TableView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<TableView> create(@Valid @RequestBody TableInput input) {
    var result = service.save(null, input);
    return ResponseEntity.created(URI.create("/api/tables/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public TableView update(@PathVariable Long id, @Valid @RequestBody TableInput input) {
    return service.save(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
