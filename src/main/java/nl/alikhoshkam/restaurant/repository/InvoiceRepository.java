package nl.alikhoshkam.restaurant.repository;

import java.util.*;
import nl.alikhoshkam.restaurant.model.Invoice;
import org.springframework.data.jpa.repository.*;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
  boolean existsByOrderId(Long orderId);
}
