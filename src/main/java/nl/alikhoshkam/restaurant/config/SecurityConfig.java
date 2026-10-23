package nl.alikhoshkam.restaurant.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> access = jwt.getClaimAsMap("realm_access");
          Object roles = access == null ? null : access.get("roles");
          if (!(roles instanceof Collection<?> list)) return List.of();
          return list.stream()
              .filter(r -> r instanceof String && Set.of("CUSTOMER", "STAFF").contains(r))
              .map(
                  r ->
                      (org.springframework.security.core.GrantedAuthority)
                          new SimpleGrantedAuthority("ROLE_" + r))
              .toList();
        });
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/owner-setup").permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/index.html", "/api-docs.html", "/app.css", "/app.js", "/app-config")
                    .permitAll()
                    .requestMatchers("/api/reservations/**")
                    .hasAnyRole("CUSTOMER", "STAFF")
                    .requestMatchers(
                        HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/tables/**")
                    .hasAnyRole("CUSTOMER", "STAFF")
                    .requestMatchers("/api/**")
                    .hasRole("STAFF")
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
        .build();
  }

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks,
      @Value("${app.jwt-audience}") String audience) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    OAuth2TokenValidator<Jwt> audienceValidator =
        jwt ->
            jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid audience", null));
    OAuth2TokenValidator<Jwt> subjectValidator =
        jwt ->
            jwt.getSubject() != null
                    && !jwt.getSubject().isBlank()
                    && jwt.getSubject().length() <= 255
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Missing or invalid subject", null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audienceValidator, subjectValidator));
    return decoder;
  }

  @Bean
  java.time.Clock clock() {
    return java.time.Clock.systemUTC();
  }
}
