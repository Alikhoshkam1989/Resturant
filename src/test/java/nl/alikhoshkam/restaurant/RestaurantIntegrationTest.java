package nl.alikhoshkam.restaurant;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RestaurantIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  private RequestPostProcessor staff() {
    return jwt()
        .jwt(j -> j.subject("staff-test"))
        .authorities(new SimpleGrantedAuthority("ROLE_STAFF"));
  }

  private RequestPostProcessor customer(String name) {
    return jwt().jwt(j -> j.subject(name)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }

  private long create(String path, String body) throws Exception {
    var result =
        mvc.perform(post(path).with(staff()).contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  private long table(int number) throws Exception {
    return create("/api/tables", "{\"tableNumber\":" + number + ",\"seats\":4,\"available\":true}");
  }

  private String reservation(long table, LocalDateTime time) throws Exception {
    return json.writeValueAsString(
        Map.of(
            "customerName",
            "Test Klant",
            "tableId",
            table,
            "startsAt",
            time.toString(),
            "guests",
            2,
            "remarks",
            "Bij het raam"));
  }

  @Test
  void websiteAssetsAndLoginConfigurationArePublicButApiIsProtected() throws Exception {
    mvc.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("The Table")));
    mvc.perform(get("/app.js")).andExpect(status().isOk());
    mvc.perform(get("/app.css")).andExpect(status().isOk());
    mvc.perform(get("/app-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientId").value("restaurant-postman"));
    mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    mvc.perform(post("/app-config")).andExpect(status().isUnauthorized());
  }

  @Test
  void reservationOwnershipOverlapAndCancellation() throws Exception {
    // Voorbereiden: echte database, controllers, services en repositories.
    long table = table(101);
    var start = LocalDateTime.now().plusDays(5).withNano(0);
    String input = reservation(table, start);
    // Uitvoeren
    var response =
        mvc.perform(
                post("/api/reservations")
                    .with(customer("alice"))
                    .contentType("application/json")
                    .content(input))
            .andExpect(status().isCreated())
            .andReturn();
    long id = json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
    // Controleren: eigenaarschap, overlappende en aansluitende tijdvakken en vrijgave na annulering.
    mvc.perform(get("/api/reservations/" + id).with(customer("bob")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/reservations").with(customer("bob")))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(
            post("/api/reservations")
                .with(customer("bob"))
                .contentType("application/json")
                .content(reservation(table, start.plusMinutes(30))))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/reservations")
                .with(customer("bob"))
                .contentType("application/json")
                .content(reservation(table, start.plusHours(2))))
        .andExpect(status().isCreated());
    mvc.perform(
            put("/api/reservations/" + id)
                .with(customer("alice"))
                .contentType("application/json")
                .content(input))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/reservations/" + id).with(customer("bob")))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/reservations/" + id).with(customer("alice")))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/reservations")
                .with(customer("bob"))
                .contentType("application/json")
                .content(input))
        .andExpect(status().isCreated());
    mvc.perform(
            put("/api/reservations/" + id)
                .with(staff())
                .contentType("application/json")
                .content(input))
        .andExpect(status().isConflict());
  }

  @Test
  void orderPriceSnapshotInvoiceAndPaymentLifecycle() throws Exception {
    // Voorbereiden
    long table = table(102);
    long category =
        create("/api/categories", "{\"name\":\"Testgerechten\",\"description\":\"Integratie\"}");
    String productInput =
        "{\"name\":\"Soep\",\"categoryId\":" + category + ",\"price\":6.25,\"available\":true}";
    long product = create("/api/products", productInput);
    String orderInput =
        "{\"tableId\":" + table + ",\"lines\":[{\"productId\":" + product + ",\"quantity\":2}]}";
    // Uitvoeren
    long order = create("/api/orders", orderInput);
    mvc.perform(
            put("/api/products/" + product)
                .with(staff())
                .contentType("application/json")
                .content(productInput.replace("6.25", "9.00")))
        .andExpect(status().isOk());
    // Controleren: cataloguswijzigingen overschrijven de afgesproken bestelprijzen niet.
    mvc.perform(
            put("/api/orders/" + order)
                .with(staff())
                .contentType("application/json")
                .content(orderInput))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(12.50));
    long invoice = create("/api/invoices", "{\"orderId\":" + order + "}");
    mvc.perform(
            post("/api/invoices")
                .with(staff())
                .contentType("application/json")
                .content("{\"orderId\":" + order + "}"))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/orders/" + order)
                .with(staff())
                .contentType("application/json")
                .content(orderInput))
        .andExpect(status().isConflict());
    mvc.perform(
            put("/api/invoices/" + invoice)
                .with(staff())
                .contentType("application/json")
                .content("{\"paid\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paid").value(true));
    mvc.perform(
            put("/api/invoices/" + invoice)
                .with(staff())
                .contentType("application/json")
                .content("{\"paid\":false}"))
        .andExpect(status().isConflict());
    mvc.perform(delete("/api/invoices/" + invoice).with(staff())).andExpect(status().isConflict());
    mvc.perform(delete("/api/products/" + product).with(staff())).andExpect(status().isConflict());
    mvc.perform(get("/api/invoices/" + invoice).with(customer("alice")))
        .andExpect(status().isForbidden());
  }

  @Test
  void imageUploadRoundTripAndRejectsDisguisedFiles() throws Exception {
    // Voorbereiden
    long category =
        create("/api/categories", "{\"name\":\"Fototest\",\"description\":\"Afbeeldingen\"}");
    long product =
        create(
            "/api/products",
            "{\"name\":\"Thee foto\",\"categoryId\":"
                + category
                + ",\"price\":3.00,\"available\":true}");
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(
        new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB),
        "png",
        bytes);
    var file = new MockMultipartFile("file", "../../photo.png", "image/png", bytes.toByteArray());
    String path = "/api/products/" + product + "/image";
    // Uitvoeren
    mvc.perform(
            multipart(path)
                .file(file)
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    })
                .with(staff()))
        .andExpect(status().isNoContent());
    // Controleren
    mvc.perform(get(path).with(customer("alice")))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(bytes.toByteArray()));
    mvc.perform(
            multipart(path)
                .file(file)
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    })
                .with(customer("alice")))
        .andExpect(status().isForbidden());
    var fake =
        new MockMultipartFile(
            "file", "fake.png", "image/png", "<script>alert(1)</script>".getBytes());
    mvc.perform(
            multipart(path)
                .file(fake)
                .with(
                    r -> {
                      r.setMethod("PUT");
                      return r;
                    })
                .with(staff()))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  void authenticationValidationAndMissingResources() throws Exception {
    // Voorbereiden / Uitvoeren / Controleren
    mvc.perform(get("/index.html")).andExpect(status().isOk());
    mvc.perform(get("/api-docs.html")).andExpect(status().isOk());
    mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/products").header("Authorization", "Bearer invalid.jwt.value"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/tables")
                .with(customer("alice"))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/tables")
                .with(staff())
                .contentType("application/json")
                .content("{\"tableNumber\":0,\"seats\":0,\"available\":true}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/products/999999").with(staff())).andExpect(status().isNotFound());
    mvc.perform(get("/api/products/not-a-number").with(staff())).andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/orders")
                .with(staff())
                .contentType("application/json")
                .content("{\"tableId\":1,\"lines\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void simultaneousReservationsCannotDoubleBookATable() throws Exception {
    // Voorbereiden: elk HTTP-verzoek heeft een eigen transactie en persistentiecontext.
    long table = table(103);
    String input = reservation(table, LocalDateTime.now().plusDays(10));
    var start = new java.util.concurrent.CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Integer> reserve =
          () -> {
            start.await();
            return mvc.perform(
                    post("/api/reservations")
                        .with(customer("race"))
                        .contentType("application/json")
                        .content(input))
                .andReturn()
                .getResponse()
                .getStatus();
          };
      // Uitvoeren
      var first = executor.submit(reserve);
      var second = executor.submit(reserve);
      start.countDown();
      // Controleren
      assertThat(
              java.util.List.of(
                  first.get(15, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(15, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
    }
  }
}
