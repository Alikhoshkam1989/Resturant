package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "total >= 0")
public class Invoice {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @OneToOne(optional = false)
  @JoinColumn(name = "order_id", nullable = false, unique = true)
  private RestaurantOrder order;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal total;

  @Column(nullable = false)
  private boolean paid;
}
