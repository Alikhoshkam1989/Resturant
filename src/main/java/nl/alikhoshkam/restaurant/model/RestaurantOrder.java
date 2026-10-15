package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "restaurant_orders")
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "total >= 0")
public class RestaurantOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private DiningTable diningTable;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderLine> lines = new ArrayList<>();

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;
}
