package nl.alikhoshkam.restaurant.repository;

import java.util.*;
import nl.alikhoshkam.restaurant.model.Category;
import org.springframework.data.jpa.repository.*;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
