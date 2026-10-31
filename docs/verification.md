# Verificatierapport

Uitgevoerd op 16 september 2026, lokaal op Windows. Het project is gebouwd met Eclipse Temurin JDK 21.0.12.1, Maven 3.9.4 en Spring Boot 3.5.6. De meetgegevens staan in `verification-results.json`.

## Geautomatiseerde tests

Commando: `mvn -B -q verify`.

| Testklasse | Tests | Resultaat |
|---|---:|---|
| PricingServiceTest | 10 | Geslaagd |
| ReservationPolicyTest | 10 | Geslaagd |
| RestaurantIntegrationTest | 5 | Geslaagd |
| JwtDecoderTest | 6 | Geslaagd |
| TableControllerWebMvcTest | 4 | Geslaagd |
| **Totaal** | **35** | **0 fouten, 0 mislukte of overgeslagen tests** |

JaCoCo: PricingService heeft 13 gedekte en 0 gemiste uitvoerbare regels; ReservationPolicy heeft 9 gedekte en 0 gemiste regels. Beide hebben dus **100% line coverage**. De Maven-check handhaaft dit voor deze twee klassen. Er wordt geen 100%-claim gedaan voor de hele applicatie.

De integratietests gebruiken echte H2-repositories en transacties. Zij controleren onder andere dat twee gelijktijdige reserveringen voor dezelfde tafel precies één 201 en één 409 opleveren. Andere scenario's testen eigenaarschap, annulering en vrijgave van tijdvakken, historische bestelprijzen, facturatievergrendeling, betaling, afbeelding-upload/download en foutafhandeling.

JWT-tests verifiëren echte RSA-handtekeningen via een tijdelijke JWKS-server. Zij weigeren een verkeerde issuer, audience, verlopen token, vervalste handtekening en ontbrekend subject. WebMvc-tests verifiëren Keycloak-rolconversie, geweigerde mutaties, onbekende rollen en HTTP 405 met Allow-header.

## Echte lokale Keycloak- en API-test

Voor deze eerdere controle is Keycloak **26.7.3** rechtstreeks met **JDK 25** gestart, gebonden aan 127.0.0.1:8081. De aangeleverde realm-JSON is geïmporteerd. De API is gestart op 127.0.0.1:8080 met een afzonderlijke H2-geheugendatabase.

`python scripts/smoke_test.py` is geslaagd voor:

- Tokens voor de meegeleverde klant- en personeelsaccounts.
- 401 zonder token; 403 voor een klant die categorieën probeert te beheren.
- Categorie, product en tafel aanmaken met personeelsrechten.
- Reservering maken, dubbele boeking weigeren en annuleren met klantrechten.
- Bestelling met twee producten van EUR 12,50: berekend totaal EUR 25,00.
- Factuur aanmaken en betaald markeren.
- Weigeren van verwijdering van een gefactureerde bestelling.

`python scripts/registration_smoke_test.py` is eveneens geslaagd: zelfregistratie via de Keycloak-formulieren, Authorization Code met PKCE S256, code-uitwisseling voor tokens, aanwezig subject, automatische CUSTOMER-rol en afwezigheid van STAFF. De test gebruikt HTTP-formulieren en bootst voor localhost het secure-cookiegedrag van browsers na; er is geen browserautomatisering nodig.

De live test ontdekte dat de Keycloak-client de scope `basic` nodig heeft om `sub` op te nemen. De export bevat die scope nu. De API weigert daarnaast tokens zonder bruikbaar subject. Deze fout en correctie zijn niet verborgen achter gemockte authenticatie.

De smoke-scripts maken testgegevens en, voor registratie, een testaccount aan. Voer ze uitsluitend uit in een lokale, wegwerpbare omgeving. De gebruikte tijdelijke testservers zijn na verificatie gestopt; de normale installatieprocedure staat in README.md.

## Documentatie en verpakking

- Drie PDF's met samen 30 pagina's: technisch ontwerp 10, verantwoordingsdocument 9, API-documentatie 11.
- Alle pagina's zijn gerenderd en visueel gecontroleerd op afkapping, overlapping, pagina-indeling en leesbaarheid.
- De PDF-generator weigert tekst en diagrammen die buiten de paginaruimte vallen.
- Requirements: vier user stories, 23 functionele en 9 niet-functionele eisen.
- Broncode-ZIP wordt expliciet samengesteld uit broncode, docs, exports, scripts en rootconfiguratie. Er worden geen buildmappen, IDE-bestanden, lokale databases of runtimes opgenomen.

## Grenzen en open punten

PostgreSQL is niet uitgevoerd. Er is geen productie-, belasting-, backupherstel- of complete penetratietest uitgevoerd. De testomgeving bevat alleen demonstratiegegevens.

Een publieke GitHub-URL, vijf gemergede PR's, definitieve inleverdatum, persoonlijke reflecties en bevestiging van goedkeuring van de ideefase ontbreken nog. Zie `submission-checklist.md`. Lokale technische verificatie maakt die beoordelingsvoorwaarden niet overbodig.
