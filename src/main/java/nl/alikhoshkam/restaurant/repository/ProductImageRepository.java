package nl.alikhoshkam.restaurant.repository;

import java.util.*;
import nl.alikhoshkam.restaurant.model.ProductImage;
import org.springframework.data.jpa.repository.*;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
  Optional<ProductImage> findByProductId(Long productId);

  void deleteByProductId(Long productId);
}
