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
public class PricingService {
  public BigDecimal total(List<OrderLine> lines) {
    if (lines == null || lines.isEmpty())
      throw ApiException.badRequest("Order must contain lines");
    BigDecimal total = BigDecimal.ZERO;
    for (OrderLine line : lines) {
      if (line.getQuantity() < 1 || line.getQuantity() > 100)
        throw ApiException.badRequest("Quantity must be between 1 and 100");
      if (line.getUnitPrice() == null || line.getUnitPrice().signum() < 0)
        throw ApiException.badRequest("Price must not be negative or missing");
      total = total.add(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
    }
    if (total.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw ApiException.badRequest("Order total is too high");
    return total.setScale(2, RoundingMode.HALF_UP);
  }
}
