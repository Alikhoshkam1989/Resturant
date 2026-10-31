package nl.alikhoshkam.restaurant.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.*;
import nl.alikhoshkam.restaurant.config.SecurityConfig;
import nl.alikhoshkam.restaurant.dto.Views.TableView;
import nl.alikhoshkam.restaurant.service.TableService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TableController.class)
@Import(SecurityConfig.class)
class TableControllerWebMvcTest {
  @Autowired MockMvc mvc;
  @MockitoBean TableService service;
  @MockitoBean JwtDecoder decoder;

  private void token(String role) {
    when(decoder.decode("test-token"))
        .thenReturn(
            Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build());
  }

  @Test
  void convertsKeycloakCustomerRoleForReadAccess() throws Exception {
    // Voorbereiden
    token("CUSTOMER");
    when(service.list()).thenReturn(List.of(new TableView(1L, 1, 4, true)));
    // Uitvoeren / Controleren
    mvc.perform(get("/api/tables").header("Authorization", "Bearer test-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].seats").value(4));
  }

  @Test
  void customerCannotDeleteTable() throws Exception {
    // Voorbereiden
    token("CUSTOMER");
    // Uitvoeren / Controleren
    mvc.perform(delete("/api/tables/1").header("Authorization", "Bearer test-token"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void unknownRoleHasNoAccess() throws Exception {
    // Voorbereiden
    token("ADMIN");
    // Uitvoeren / Controleren
    mvc.perform(get("/api/tables").header("Authorization", "Bearer test-token"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void validationRemainsEnglishWithGermanLanguageHeader() throws Exception {
    token("STAFF");
    mvc.perform(
            post("/api/tables")
                .header("Authorization", "Bearer test-token")
                .header("Accept-Language", "de")
                .contentType("application/json")
                .content("{\"tableNumber\":0,\"seats\":4,\"available\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("tableNumber: must be greater than or equal to 1"));
    verifyNoInteractions(service);
  }

  @Test
  void unsupportedMethodReturns405AndAllowHeader() throws Exception {
    // Voorbereiden
    token("STAFF");
    // Uitvoeren / Controleren
    mvc.perform(patch("/api/tables/1").header("Authorization", "Bearer test-token"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"));
    verifyNoInteractions(service);
  }
}
