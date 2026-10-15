package nl.alikhoshkam.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class Requests {
  private Requests() {}

  public record CategoryInput(
      @NotBlank @Size(max = 80) String name, @NotNull @Size(max = 500) String description) {}

  public record ProductInput(
      @NotBlank @Size(max = 100) String name,
      @NotNull @Positive Long categoryId,
      @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal price,
      @NotNull Boolean available) {}

  public record TableInput(
      @Min(1) int tableNumber, @Min(1) @Max(50) int seats, @NotNull Boolean available) {}

  public record ReservationInput(
      @NotBlank @Size(max = 100) String customerName,
      @NotNull @Positive Long tableId,
      @NotNull LocalDateTime startsAt,
      @Min(1) @Max(50) int guests,
      @NotNull @Size(max = 1000) String remarks) {}

  public record LineInput(@NotNull @Positive Long productId, @Min(1) @Max(100) int quantity) {}

  public record OrderInput(
      @NotNull @Positive Long tableId,
      @NotEmpty @Size(max = 100) List<@NotNull @Valid LineInput> lines) {}

  public record InvoiceInput(@NotNull @Positive Long orderId) {}

  public record PaymentInput(@NotNull Boolean paid) {}
}
