package nl.alikhoshkam.restaurant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_images")
@Getter
@Setter
public class ProductImage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version private long version;

  @OneToOne(optional = false)
  @JoinColumn(nullable = false, unique = true)
  private Product product;

  @Column(nullable = false)
  private String contentType;

  @Lob
  @Column(nullable = false)
  private byte[] content;
}
