package nl.alikhoshkam.restaurant.repository;

import java.util.*;
import nl.alikhoshkam.restaurant.model.OrderLine;
import org.springframework.data.jpa.repository.*;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
  boolean existsByProductId(Long productId);
}
