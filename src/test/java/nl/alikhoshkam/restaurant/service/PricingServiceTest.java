package nl.alikhoshkam.restaurant.service;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import nl.alikhoshkam.restaurant.exception.ApiException;
import nl.alikhoshkam.restaurant.model.OrderLine;
import org.junit.jupiter.api.Test;

class PricingServiceTest {
  private final PricingService service = new PricingService();

  private OrderLine line(String price, int quantity) {
    OrderLine line = new OrderLine();
    line.setUnitPrice(price == null ? null : new BigDecimal(price));
    line.setQuantity(quantity);
    return line;
  }

  @Test
  void calculatesMultipleProductsWithQuantities() {
    // Voorbereiden
    var lines = List.of(line("14.50", 2), line("3.00", 3));
    // Uitvoeren
    var total = service.total(lines);
    // Controleren
    assertThat(total).isEqualByComparingTo("38.00");
  }

  @Test
  void preservesDecimalPrecision() {
    // Voorbereiden
    var lines = List.of(line("0.10", 3), line("0.20", 1));
    // Uitvoeren
    var total = service.total(lines);
    // Controleren
    assertThat(total.toPlainString()).isEqualTo("0.50");
  }

  @Test
  void allowsComplimentaryProducts() {
    // Voorbereiden
    var lines = List.of(line("0.00", 1));
    // Uitvoeren
    var total = service.total(lines);
    // Controleren
    assertThat(total).isEqualByComparingTo("0.00");
  }

  @Test
  void rejectsEmptyOrder() {
    // Voorbereiden
    List<OrderLine> lines = List.of();
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsMissingOrder() {
    // Voorbereiden
    List<OrderLine> lines = null;
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsZeroQuantity() {
    // Voorbereiden
    var lines = List.of(line("10", 0));
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).hasMessageContaining("Quantity");
  }

  @Test
  void rejectsExcessiveQuantity() {
    // Voorbereiden
    var lines = List.of(line("10", 101));
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).hasMessageContaining("Quantity");
  }

  @Test
  void rejectsNegativePrice() {
    // Voorbereiden
    var lines = List.of(line("-1", 1));
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).hasMessageContaining("Price");
  }

  @Test
  void rejectsMissingPrice() {
    // Voorbereiden
    var lines = List.of(line(null, 1));
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).hasMessageContaining("Price");
  }

  @Test
  void rejectsDatabasePrecisionOverflow() {
    // Voorbereiden
    var lines = List.of(line("9999999999.99", 100));
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> service.total(lines)).hasMessageContaining("too high");
  }
}
