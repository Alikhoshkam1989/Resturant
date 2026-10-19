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
public class OrderService {
  private final RestaurantOrderRepository orders;
  private final DiningTableRepository tables;
  private final ProductRepository products;
  private final InvoiceRepository invoices;
  private final PricingService pricing;
  private final Clock clock;

  public List<OrderView> list() {
    return orders.findAll().stream().map(Views::of).toList();
  }

  public OrderView get(Long id) {
    return Views.of(find(id));
  }

  public OrderView create(OrderInput input) {
    RestaurantOrder order = new RestaurantOrder();
    order.setCreatedAt(LocalDateTime.now(clock));
    apply(order, input);
    return Views.of(orders.save(order));
  }

  public OrderView update(Long id, OrderInput input) {
    RestaurantOrder order = find(id);
    requireUnbilled(id);
    apply(order, input);
    return Views.of(orders.save(order));
  }

  public void delete(Long id) {
    RestaurantOrder order = find(id);
    requireUnbilled(id);
    orders.delete(order);
  }

  private RestaurantOrder find(Long id) {
    return orders.findLockedById(id).orElseThrow(() -> ApiException.notFound("Order"));
  }

  private void requireUnbilled(Long id) {
    if (invoices.existsByOrderId(id)) throw ApiException.conflict("Order has been invoiced");
  }

  private void apply(RestaurantOrder order, OrderInput input) {
    DiningTable table =
        tables.findById(input.tableId()).orElseThrow(() -> ApiException.notFound("Table"));
    if (!table.isAvailable()) throw ApiException.conflict("Table is not available");
    Map<Long, OrderLine> previous = new HashMap<>();
    for (OrderLine line : order.getLines()) previous.put(line.getProduct().getId(), line);
    Set<Long> seen = new HashSet<>();
    List<OrderLine> replacement = new ArrayList<>();
    for (LineInput requested : input.lines()) {
      if (!seen.add(requested.productId()))
        throw ApiException.badRequest("A product may only occur once in an order");
      Product p =
          products
              .findById(requested.productId())
              .orElseThrow(() -> ApiException.notFound("Product"));
      if (!p.isAvailable())
        throw ApiException.conflict("Product is not available: " + p.getName());
      OrderLine line = new OrderLine();
      line.setOrder(order);
      line.setProduct(p);
      line.setQuantity(requested.quantity());
      OrderLine old = previous.get(p.getId());
      line.setUnitPrice(old == null ? p.getPrice() : old.getUnitPrice());
      line.setProductName(old == null ? p.getName() : old.getProductName());
      replacement.add(line);
    }
    order.setDiningTable(table);
    order.setTotal(pricing.total(replacement));
    order.getLines().clear();
    order.getLines().addAll(replacement);
  }
}
