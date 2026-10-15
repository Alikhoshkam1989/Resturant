package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_lines")
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "quantity > 0 and unit_price >= 0")
public class OrderLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private RestaurantOrder order;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private Product product;

  @Column(nullable = false, length = 100)
  private String productName;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private int quantity;
}
