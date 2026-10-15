package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@org.hibernate.annotations.Check(constraints = "guests > 0 and ends_at > starts_at")
public class Reservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @Column(nullable = false, length = 255)
  private String customerSubject;

  @Column(nullable = false, length = 100)
  private String customerName;

  @ManyToOne(optional = false)
  @JoinColumn(nullable = false)
  private DiningTable diningTable;

  @Column(nullable = false)
  private LocalDateTime startsAt;

  @Column(nullable = false)
  private LocalDateTime endsAt;

  @Column(nullable = false)
  private int guests;

  @Column(nullable = false, length = 1000)
  private String remarks;

  @Column(nullable = false)
  private boolean cancelled;
}
