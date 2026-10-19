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
public class InvoiceService {
  private final InvoiceRepository invoices;
  private final RestaurantOrderRepository orders;
  private final Clock clock;

  public List<InvoiceView> list() {
    return invoices.findAll().stream().map(Views::of).toList();
  }

  public InvoiceView get(Long id) {
    return Views.of(find(id));
  }

  public InvoiceView create(InvoiceInput input) {
    RestaurantOrder order =
        orders
            .findLockedById(input.orderId())
            .orElseThrow(() -> ApiException.notFound("Order"));
    if (invoices.existsByOrderId(order.getId()))
      throw ApiException.conflict("Order already has an invoice");
    Invoice invoice = new Invoice();
    invoice.setOrder(order);
    invoice.setCreatedAt(LocalDateTime.now(clock));
    invoice.setTotal(order.getTotal());
    return Views.of(invoices.save(invoice));
  }

  public InvoiceView payment(Long id, PaymentInput input) {
    Invoice invoice = find(id);
    if (invoice.isPaid() && !input.paid())
      throw ApiException.conflict("Payment cannot be reversed");
    invoice.setPaid(input.paid());
    return Views.of(invoices.save(invoice));
  }

  public void delete(Long id) {
    Invoice invoice = find(id);
    orders
        .findLockedById(invoice.getOrder().getId())
        .orElseThrow(() -> ApiException.notFound("Order"));
    if (invoice.isPaid())
      throw ApiException.conflict("Paid invoice cannot be deleted");
    invoices.delete(invoice);
  }

  private Invoice find(Long id) {
    return invoices.findById(id).orElseThrow(() -> ApiException.notFound("Invoice"));
  }
}
