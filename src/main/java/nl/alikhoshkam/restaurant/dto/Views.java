package nl.alikhoshkam.restaurant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import nl.alikhoshkam.restaurant.model.*;

public final class Views {
  private Views() {}

  public record CategoryView(Long id, String name, String description) {}

  public record ProductView(
      Long id,
      String name,
      Long categoryId,
      BigDecimal price,
      boolean available,
      String imageUrl) {}

  public record TableView(Long id, int tableNumber, int seats, boolean available) {}

  public record ReservationView(
      Long id,
      String customerName,
      Long tableId,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      int guests,
      String remarks,
      boolean cancelled) {}

  public record LineView(Long productId, String productName, BigDecimal unitPrice, int quantity) {}

  public record OrderView(
      Long id, Long tableId, LocalDateTime createdAt, List<LineView> lines, BigDecimal total) {}

  public record InvoiceView(
      Long id, Long orderId, LocalDateTime createdAt, BigDecimal total, boolean paid) {}

  public static CategoryView of(Category c) {
    return new CategoryView(c.getId(), c.getName(), c.getDescription());
  }

  public static ProductView of(Product p) {
    return new ProductView(
        p.getId(),
        p.getName(),
        p.getCategory().getId(),
        p.getPrice(),
        p.isAvailable(),
        "/api/products/" + p.getId() + "/image");
  }

  public static TableView of(DiningTable t) {
    return new TableView(t.getId(), t.getTableNumber(), t.getSeats(), t.isAvailable());
  }

  public static ReservationView of(Reservation r) {
    return new ReservationView(
        r.getId(),
        r.getCustomerName(),
        r.getDiningTable().getId(),
        r.getStartsAt(),
        r.getEndsAt(),
        r.getGuests(),
        r.getRemarks(),
        r.isCancelled());
  }

  public static OrderView of(RestaurantOrder o) {
    return new OrderView(
        o.getId(),
        o.getDiningTable().getId(),
        o.getCreatedAt(),
        o.getLines().stream()
            .map(
                l ->
                    new LineView(
                        l.getProduct().getId(),
                        l.getProductName(),
                        l.getUnitPrice(),
                        l.getQuantity()))
            .toList(),
        o.getTotal());
  }

  public static InvoiceView of(Invoice i) {
    return new InvoiceView(
        i.getId(), i.getOrder().getId(), i.getCreatedAt(), i.getTotal(), i.isPaid());
  }
}
