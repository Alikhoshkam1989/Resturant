package nl.alikhoshkam.restaurant.config;

import static org.assertj.core.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.*;

class JwtDecoderTest {
  private HttpServer server;
  private RSAKey key;
  private JwtDecoder decoder;

  @BeforeEach
  void setup() throws Exception {
    key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/jwks",
        exchange -> {
          byte[] body =
              new JWKSet(key.toPublicJWK())
                  .toString()
                  .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          try (var stream = exchange.getResponseBody()) {
            stream.write(body);
          }
        });
    server.start();
    decoder =
        new SecurityConfig()
            .jwtDecoder(
                "https://issuer.example.test",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks",
                "restaurant-api");
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  private String token(String issuer, String audience, Instant expiry, RSAKey signingKey)
      throws Exception {
    var claims =
        new JWTClaimsSet.Builder()
            .subject("customer")
            .issuer(issuer)
            .audience(audience)
            .issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(expiry))
            .build();
    var signed =
        new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
    signed.sign(new RSASSASigner(signingKey));
    return signed.serialize();
  }

  @Test
  void acceptsTrustedSignatureIssuerAudienceAndExpiry() throws Exception {
    // Voorbereiden
    var token =
        token("https://issuer.example.test", "restaurant-api", Instant.now().plusSeconds(300), key);
    // Uitvoeren
    var result = decoder.decode(token);
    // Controleren
    assertThat(result.getSubject()).isEqualTo("customer");
  }

  @Test
  void rejectsWrongAudience() throws Exception {
    // Voorbereiden
    var token =
        token("https://issuer.example.test", "different-api", Instant.now().plusSeconds(300), key);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  void rejectsWrongIssuer() throws Exception {
    // Voorbereiden
    var token =
        token(
            "https://untrusted.example.test",
            "restaurant-api",
            Instant.now().plusSeconds(300),
            key);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    // Voorbereiden
    var token =
        token(
            "https://issuer.example.test", "restaurant-api", Instant.now().minusSeconds(300), key);
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  @Test
  void rejectsForgedSignature() throws Exception {
    // Voorbereiden
    var token =
        token(
            "https://issuer.example.test",
            "restaurant-api",
            Instant.now().plusSeconds(300),
            new RSAKeyGenerator(2048).generate());
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsSignedTokenWithoutSubject() throws Exception {
    // Voorbereiden: Keycloak-clients hebben de basic-scope nodig voor de sub-claim.
    var claims =
        new JWTClaimsSet.Builder()
            .issuer("https://issuer.example.test")
            .audience("restaurant-api")
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .build();
    var signed =
        new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims);
    signed.sign(new RSASSASigner(key));
    // Uitvoeren / Controleren
    assertThatThrownBy(() -> decoder.decode(signed.serialize()))
        .isInstanceOf(JwtValidationException.class);
  }
}
