package nl.alikhoshkam.restaurant.controller;

import lombok.RequiredArgsConstructor;
import nl.alikhoshkam.restaurant.dto.Requests.*;
import nl.alikhoshkam.restaurant.dto.Views.*;
import nl.alikhoshkam.restaurant.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products/{productId}/image")
@RequiredArgsConstructor
public class ImageController {
  private final ImageService service;

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void upload(@PathVariable Long productId, @RequestPart("file") MultipartFile file) {
    service.upload(productId, file);
  }

  @GetMapping
  public ResponseEntity<byte[]> download(@PathVariable Long productId) {
    var image = service.download(productId);
    String extension = image.contentType().equals("image/png") ? ".png" : ".jpg";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=product-" + productId + extension)
        .header("X-Content-Type-Options", "nosniff")
        .body(image.content());
  }
}
