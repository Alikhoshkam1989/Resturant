package nl.alikhoshkam.restaurant.repository;

import jakarta.persistence.LockModeType;
import java.util.*;
import nl.alikhoshkam.restaurant.model.DiningTable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from DiningTable t where t.id = :id")
  Optional<DiningTable> findLockedById(@Param("id") Long id);
}
