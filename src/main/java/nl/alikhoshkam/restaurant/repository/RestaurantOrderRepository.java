package nl.alikhoshkam.restaurant.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import nl.alikhoshkam.restaurant.model.RestaurantOrder;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RestaurantOrderRepository extends JpaRepository<RestaurantOrder, Long> {
  boolean existsByDiningTableId(Long tableId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from RestaurantOrder o where o.id = :id")
  Optional<RestaurantOrder> findLockedById(@Param("id") Long id);
}
