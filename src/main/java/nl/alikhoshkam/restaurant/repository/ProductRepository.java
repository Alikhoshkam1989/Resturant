package nl.alikhoshkam.restaurant.repository;

import java.util.*;
import nl.alikhoshkam.restaurant.model.Product;
import org.springframework.data.jpa.repository.*;

public interface ProductRepository extends JpaRepository<Product, Long> {
  boolean existsByCategoryId(Long categoryId);
}
