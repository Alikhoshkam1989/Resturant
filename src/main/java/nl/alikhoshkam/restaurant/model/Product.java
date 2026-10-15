package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "price >= 0")
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, length = 100)
  private String name;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private Category category;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean available;
}
