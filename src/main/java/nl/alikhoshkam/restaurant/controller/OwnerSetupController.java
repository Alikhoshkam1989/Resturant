package nl.alikhoshkam.restaurant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/owner-setup")
public class OwnerSetupController {
  private final Path directory;
  private final ObjectMapper mapper;
  private final RestClient client;

  public OwnerSetupController(ObjectMapper mapper,
      @Value("${app.owner-setup-directory:data/owner-setup}") String directory,
      @Value("${app.owner-setup-url:http://127.0.0.1:8081}") String url) {
    this.mapper = mapper;
    this.directory = Path.of(directory);
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(15000);
    this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }

  public record Registration(
      @NotBlank @Size(max=100) String code,
      @Pattern(regexp="[a-zA-Z0-9][a-zA-Z0-9._-]{2,63}") @NotNull String username,
      @NotBlank @Email @Size(max=254) String email,
      @NotNull @Size(min=12,max=128) String password) {}

  @GetMapping
  public Map<String, Boolean> status() {
    return Map.of("available", Files.exists(directory.resolve("owner-setup.json"))
        && !Files.exists(directory.resolve("owner-created")));
  }

  @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
  public synchronized Map<String, String> register(@Valid @RequestBody Registration input,
      HttpServletRequest request) throws IOException {
    // Alleen lokale beheerders met de eenmalige code mogen een eigenaar aanmaken.
    if (!Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1").contains(request.getRemoteAddr()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Set up the owner on the computer running this app.");
    String origin = request.getHeader("Origin");
    if (origin != null && !origin.equals(URI.create(request.getRequestURL().toString()).resolve("/").toString().replaceAll("/$", "")))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Open this form from the restaurant website.");
    if (!status().get("available"))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner setup is closed.");
    try (var channel = FileChannel.open(directory.resolve("owner-setup.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
         var lock = channel.lock()) {
      if (!status().get("available")) throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner setup is closed.");
      var setup = mapper.readTree(directory.resolve("owner-setup.json").toFile());
      if (!MessageDigest.isEqual(input.code().getBytes(StandardCharsets.UTF_8), setup.path("code").asText().getBytes(StandardCharsets.UTF_8)))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect setup code. Copy it from the application start window.");
      var form = new LinkedMultiValueMap<String, String>();
      form.add("grant_type", "client_credentials");
      form.add("client_id", setup.path("clientId").asText());
      form.add("client_secret", setup.path("secret").asText());
      String userPath = null;
      String token = null;
      try {
        var result = client.post().uri("/realms/master/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Map.class);
        token = (String) result.get("access_token");
        final String bearer = token;
        var role = client.get().uri("/admin/realms/master/roles/admin").headers(h -> h.setBearerAuth(bearer)).retrieve().body(Map.class);
        var created = client.post().uri("/admin/realms/master/users").headers(h -> h.setBearerAuth(bearer))
            .contentType(MediaType.APPLICATION_JSON).body(Map.of("username", input.username(), "email", input.email(),
                "enabled", true, "firstName", input.username(), "lastName", "Owner",
                "credentials", List.of(Map.of("type", "password", "value", input.password(), "temporary", false))))
            .retrieve().toBodilessEntity();
        userPath = Objects.requireNonNull(created.getHeaders().getLocation()).getPath();
        client.post().uri(userPath + "/role-mappings/realm").headers(h -> h.setBearerAuth(bearer))
            .contentType(MediaType.APPLICATION_JSON).body(List.of(role)).retrieve().toBodilessEntity();
        Files.writeString(directory.resolve("owner-created"), input.username(), StandardOpenOption.CREATE_NEW);
        // Verwijder de tijdelijke service na het aanmaken van de permanente eigenaar.
        var clients = client.get().uri("/admin/realms/master/clients?clientId=" + setup.path("clientId").asText())
            .headers(h -> h.setBearerAuth(bearer)).retrieve().body(List.class);
        if (clients != null && clients.size() == 1) {
          String id = (String) ((Map<?, ?>) clients.getFirst()).get("id");
          client.delete().uri("/admin/realms/master/clients/" + id).headers(h -> h.setBearerAuth(bearer)).retrieve().toBodilessEntity();
        }
        Files.deleteIfExists(directory.resolve("owner-setup.json"));
        return Map.of("message", "Owner account created. You can now sign in as restaurant owner.");
      } catch (RestClientException | IOException error) {
        if (Files.exists(directory.resolve("owner-created")))
          return Map.of("message", "Owner account created. You can now sign in. Temporary setup cleanup needs attention in administration.");
        if (userPath != null && token != null) {
          final String bearer = token;
          try { client.delete().uri(userPath).headers(h -> h.setBearerAuth(bearer)).retrieve().toBodilessEntity(); }
          catch (RestClientException ignored) { /* Een beheerder kan het onvolledige account opruimen. */ }
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not create owner. Check that the username is unused and the sign-in service is running.");
      }
    }
  }
}
