package nl.alikhoshkam.restaurant.service;

import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import nl.alikhoshkam.restaurant.dto.*;
import nl.alikhoshkam.restaurant.dto.Requests.*;
import nl.alikhoshkam.restaurant.dto.Views.*;
import nl.alikhoshkam.restaurant.exception.ApiException;
import nl.alikhoshkam.restaurant.model.*;
import nl.alikhoshkam.restaurant.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final OrderLineRepository lines;

  public List<CategoryView> categories() {
    return categories.findAll().stream().map(Views::of).toList();
  }

  public CategoryView category(Long id) {
    return Views.of(findCategory(id));
  }

  public CategoryView saveCategory(Long id, CategoryInput input) {
    Category c = id == null ? new Category() : findCategory(id);
    c.setName(input.name().trim());
    c.setDescription(input.description());
    return Views.of(categories.save(c));
  }

  public void deleteCategory(Long id) {
    Category c = findCategory(id);
    if (products.existsByCategoryId(id)) throw ApiException.conflict("Category contains products");
    categories.delete(c);
  }

  public List<ProductView> products() {
    return products.findAll().stream().map(Views::of).toList();
  }

  public ProductView product(Long id) {
    return Views.of(findProduct(id));
  }

  public ProductView saveProduct(Long id, ProductInput input) {
    Product p = id == null ? new Product() : findProduct(id);
    p.setName(input.name().trim());
    p.setCategory(findCategory(input.categoryId()));
    p.setPrice(input.price());
    p.setAvailable(input.available());
    return Views.of(products.save(p));
  }

  public void deleteProduct(Long id) {
    Product p = findProduct(id);
    if (lines.existsByProductId(id))
      throw ApiException.conflict("Product is used in an order; disable availability instead");
    images.deleteByProductId(id);
    images.flush();
    products.delete(p);
  }

  private Category findCategory(Long id) {
    return categories.findById(id).orElseThrow(() -> ApiException.notFound("Category"));
  }

  private Product findProduct(Long id) {
    return products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
  }
}
