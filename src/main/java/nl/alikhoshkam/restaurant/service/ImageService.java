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
public class ImageService {
  private final ProductRepository products;
  private final ProductImageRepository images;

  public record ImageData(byte[] content, String contentType) {}

  public void upload(Long productId, org.springframework.web.multipart.MultipartFile file) {
    Product product =
        products.findById(productId).orElseThrow(() -> ApiException.notFound("Product"));
    if (file.isEmpty()) throw ApiException.badRequest("Image is empty");
    if (file.getSize() > 2 * 1024 * 1024)
      throw new ApiException(
          org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Maximum size is 2 MiB");
    try {
      byte[] data = file.getBytes();
      String type = detectType(data);
      try (var stream =
          javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(data))) {
        var readers = javax.imageio.ImageIO.getImageReaders(stream);
        if (!readers.hasNext()) throw ApiException.badRequest("Invalid image");
        var reader = readers.next();
        try {
          reader.setInput(stream);
          if ((long) reader.getWidth(0) * reader.getHeight(0) > 16_000_000)
            throw ApiException.badRequest("Image has too many pixels");
          if (reader.read(0) == null)
            throw ApiException.badRequest("Image cannot be read");
        } finally {
          reader.dispose();
        }
      }
      ProductImage image = images.findByProductId(productId).orElseGet(ProductImage::new);
      image.setProduct(product);
      image.setContentType(type);
      image.setContent(data);
      images.save(image);
    } catch (java.io.IOException e) {
      throw ApiException.badRequest("Image cannot be read");
    }
  }

  public ImageData download(Long productId) {
    ProductImage image =
        images.findByProductId(productId).orElseThrow(() -> ApiException.notFound("Image"));
    return new ImageData(image.getContent(), image.getContentType());
  }

  private String detectType(byte[] data) {
    if (data.length >= 8
        && java.util.Arrays.equals(
            java.util.Arrays.copyOf(data, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      return "image/png";
    if (data.length >= 3 && data[0] == (byte) 255 && data[1] == (byte) 216 && data[2] == (byte) 255)
      return "image/jpeg";
    throw new ApiException(
        org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "Only PNG and JPEG are allowed");
  }
}
