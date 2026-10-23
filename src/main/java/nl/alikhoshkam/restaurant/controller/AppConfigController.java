package nl.alikhoshkam.restaurant.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppConfigController {
  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuer;

  @GetMapping("/app-config")
  public Map<String, String> configuration() {
    return Map.of("issuer", issuer, "clientId", "restaurant-postman");
  }
}
