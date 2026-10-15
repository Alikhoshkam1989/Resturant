package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dining_tables")
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "seats > 0 and table_number > 0")
public class DiningTable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, unique = true)
  private int tableNumber;

  @Column(nullable = false)
  private int seats;

  @Column(nullable = false)
  private boolean available;
}
