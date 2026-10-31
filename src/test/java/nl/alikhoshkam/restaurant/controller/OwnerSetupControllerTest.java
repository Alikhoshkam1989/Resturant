package nl.alikhoshkam.restaurant.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;
import nl.alikhoshkam.restaurant.exception.ApiExceptionHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OwnerSetupControllerTest {
  @TempDir Path directory;
  final ObjectMapper mapper = new ObjectMapper();
  HttpServer server;
  MockMvc mvc;
  List<String> requests;
  boolean failRole;

  @BeforeEach void setup() throws Exception {
    Files.writeString(directory.resolve("owner-setup.json"), "{\"clientId\":\"setup-client\",\"secret\":\"private-secret\",\"code\":\"private-code\"}");
    requests = Collections.synchronizedList(new ArrayList<>());
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      requests.add(exchange.getRequestMethod() + " " + path);
      String body = "{}";
      int status = 200;
      if (path.endsWith("/token")) body = "{\"access_token\":\"test-token\"}";
      else if (path.endsWith("/roles/admin")) body = "{\"id\":\"admin-id\",\"name\":\"admin\"}";
      else if (path.endsWith("/users")) {
        status = 201;
        exchange.getResponseHeaders().add("Location", "/admin/realms/master/users/owner-id");
      } else if (path.endsWith("/role-mappings/realm") && failRole) status = 503;
      else if (path.endsWith("/clients")) body = "[{\"id\":\"client-id\"}]";
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    mvc = MockMvcBuilders.standaloneSetup(new OwnerSetupController(mapper, directory.toString(), "http://127.0.0.1:" + server.getAddress().getPort()))
        .setControllerAdvice(new ApiExceptionHandler()).build();
  }

  @AfterEach void stop() { server.stop(0); }

  String payload(String code, String password) throws Exception {
    return mapper.writeValueAsString(Map.of("code",code,"username","owner-test","email","owner@example.com","password",password));
  }

  @Test void createsPermanentOwnerAndClosesSetup() throws Exception {
    mvc.perform(post("/owner-setup").contentType(MediaType.APPLICATION_JSON).content(payload("private-code", "long-test-password")))
        .andExpect(status().isOk());
    assertTrue(requests.contains("POST /admin/realms/master/users/owner-id/role-mappings/realm"));
    assertTrue(requests.contains("DELETE /admin/realms/master/clients/client-id"));
    assertFalse(Files.exists(directory.resolve("owner-setup.json")));
    mvc.perform(get("/owner-setup")).andExpect(jsonPath("$.available").value(false));
    mvc.perform(post("/owner-setup").contentType(MediaType.APPLICATION_JSON).content(payload("private-code", "long-test-password")))
        .andExpect(status().isConflict());
  }

  @Test void rejectsWrongCodeWithoutContactingIdentityServer() throws Exception {
    mvc.perform(post("/owner-setup").contentType(MediaType.APPLICATION_JSON).content(payload("wrong", "long-test-password")))
        .andExpect(status().isForbidden());
    assertTrue(requests.isEmpty());
  }

  @Test void rejectsRemoteAndCrossOriginRequests() throws Exception {
    mvc.perform(post("/owner-setup").with(r -> { r.setRemoteAddr("100.80.64.105"); return r; })
        .contentType(MediaType.APPLICATION_JSON).content(payload("private-code", "long-test-password")))
        .andExpect(status().isForbidden());
    mvc.perform(post("/owner-setup").header("Origin", "https://other.example")
        .contentType(MediaType.APPLICATION_JSON).content(payload("private-code", "long-test-password")))
        .andExpect(status().isForbidden());
    assertTrue(requests.isEmpty());
  }

  @Test void rejectsShortPassword() throws Exception {
    mvc.perform(post("/owner-setup").contentType(MediaType.APPLICATION_JSON).content(payload("private-code", "short")))
        .andExpect(status().isBadRequest());
    assertTrue(requests.isEmpty());
  }

  @Test void rollsBackUserWhenRoleAssignmentFails() throws Exception {
    failRole = true;
    mvc.perform(post("/owner-setup").contentType(MediaType.APPLICATION_JSON).content(payload("private-code", "long-test-password")))
        .andExpect(status().isBadGateway());
    assertTrue(requests.contains("DELETE /admin/realms/master/users/owner-id"));
    assertFalse(Files.exists(directory.resolve("owner-created")));
    mvc.perform(get("/owner-setup")).andExpect(jsonPath("$.available").value(true));
  }
}
