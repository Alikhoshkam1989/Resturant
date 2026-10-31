# Restaurant API

**Ali Khoshkam - Eindopdracht Leerlijn Backend (30 EC), versie 4.2**

Deze Spring Boot REST API beheert een restaurant: klanten reserveren een tafel en personeel beheert de menukaart, productafbeeldingen, tafels, bestellingen en facturen. Authenticatie en registratie verlopen via Keycloak. De API heeft twee rollen: `CUSTOMER` (klant) en `STAFF` (personeel).

De meegeleverde Engelstalige webapp **The Table** staat op `http://localhost:8080/`.
Klanten bekijken de menukaart en beheren hun reserveringen. Personeel beheert ook
categorieen, gerechten, foto's, tafels, bestellingen en facturen via formulieren.
De API-documentatie blijft apart beschikbaar op `/api-docs.html`.

Deze GitHub-repository bevat de broncode, tests, technische documentatie en
importeerbare Keycloak- en Postman-configuratie. Lokale startscripts, runtimes
en publicatiehulpmiddelen horen niet bij deze broncode-export.

## Inhoud

1. [Functionaliteit](#functionaliteit)
2. [Benodigdheden](#benodigdheden)
3. [Projectstructuur en technieken](#projectstructuur-en-technieken)
4. [Installeren en starten](#installeren-en-starten)
5. [Inloggen en registreren](#inloggen-en-registreren)
6. [API gebruiken](#api-gebruiken)
7. [Tests](#tests)
8. [Configuratie en PostgreSQL](#configuratie-en-postgresql)
9. [Inleveren en openstaande punten](#inleveren-en-openstaande-punten)

## Functionaliteit

- Klanten maken, bekijken, wijzigen en annuleren hun eigen reserveringen. Personeel beheert alle reserveringen. Een nieuwe reservering behoort altijd aan de ingelogde gebruiker.
- Een reservering duurt twee uur. Overlap en overschrijding van de tafelcapaciteit worden geweigerd. Aansluitende tijdvakken zijn toegestaan.
- Personeel beheert categorieën, producten en tafels. Verwijderen van gebruikte gegevens wordt beschermd door bedrijfsregels en databaseconstraints.
- Bestellingen bevatten meerdere productregels. De API berekent het totaal; productnamen en prijzen worden bij bestellen vastgelegd. Bestaande regels behouden hun afgesproken prijs bij wijzigingen.
- Eén factuur per bestelling. Een factuur blokkeert wijzigingen aan de bestelling. Een betaalde factuur kan niet worden verwijderd of teruggezet naar onbetaald. Een onbetaalde factuur mag worden verwijderd, waarna de bestelling weer bewerkbaar is.
- Personeel uploadt PNG/JPEG-bestanden van maximaal 2 MiB en 16 miljoen pixels. Klanten en personeel kunnen deze downloaden. De API controleert de inhoud en gebruikt geen aangeleverde bestandsnaam als bestandspad.

Alle bedragen zijn EUR-eindprijzen zoals ingevoerd in de menukaart. Dit project berekent geen afzonderlijke btw-regels; de garagecasus is een voorbeeld, niet het gekozen domein. De factuur is een eenvoudig betalingsdocument voor de opdracht. Alle API-datums en tijden worden als **UTC zonder suffix** uitgewisseld, bijvoorbeeld `2030-12-01T17:00:00`; een frontend moet lokale tijd omrekenen.

## Benodigdheden

- **JDK 21** (bijvoorbeeld Eclipse Temurin). Controleer `java -version` én `mvn -version`: beide moeten JDK 21 gebruiken.
- **Maven 3.9+**: downloadt dependencies en bouwt de applicatie.
- **Keycloak 26** (of een vergelijkbare recente Keycloak-server), apart geïnstalleerd. Deze repository levert geen Keycloak-runtime mee.
- **Git** voor versiebeheer. **Postman** of `curl.exe` voor API-verzoeken.
- Vrije poorten **8080** (API), **8081** (Keycloak) en optioneel **5432** (PostgreSQL).
- Internet voor de eerste Maven-build.

## Projectstructuur en technieken

```text
src/main/java/nl/alikhoshkam/restaurant/
  config/       OAuth2 JWT-validatie, rollen en Clock
  controller/   HTTP-endpoints en responsecodes
  dto/          Gevalideerde inputrecords en outputrecords
  exception/    Centrale foutafhandeling met ProblemDetail
  model/        Acht JPA-entiteiten en relationele constraints
  repository/   Spring Data JPA en queries met locks
  service/      Bedrijfsregels en transacties
src/main/resources/
  application.yml   Configuratie met omgevingsvariabelen
  data.sql          Automatisch geladen demonstratiegegevens
  static/           Webapp The Table en HTML API-documentatie
src/test/           JUnit-, WebMvc-, JWT- en integratietests
exports/            Postman-collectie en Keycloak-realm
docs/               Technische documentatie en requirements
```

Spring Boot 3.5.6 verzorgt configuratie en HTTP. Spring Data JPA/Hibernate scheidt databasebewerkingen van de services. H2 is de standaarddatabase; PostgreSQL is optioneel. Spring Security valideert JWT-handtekeningen, issuer, audience en geldigheid. Bean Validation controleert DTO's; services bewaken bedrijfsregels. Lombok genereert getters, setters en constructors tijdens compilatie. JUnit 5, AssertJ, Mockito, MockMvc en JaCoCo verzorgen de tests en coverage.

## Installeren en starten

Deze broncode-export start **niet** met `Start-Restaurant.cmd` of PowerShell-starters. Installeer Java 21, Maven en Keycloak zelf en volg de stappen hieronder.

1. Clone of open de repository in een terminal in de map met `pom.xml`.
2. Stel JDK 21 in. Gebruik het pad van jouw installatie:

   ```powershell
   $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21'
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"
   java -version
   mvn -version
   ```

   Beide commando's moeten Java 21 tonen.

3. Installeer Keycloak afzonderlijk (bijvoorbeeld Keycloak 26). Start de server op poort **8081**, gebonden aan localhost. Voorbeeld met de Keycloak-distributie:

   ```powershell
   $env:KC_HTTP_PORT = '8081'
   $env:KC_HOSTNAME = 'localhost'
   .\bin\kc.bat start-dev --http-port=8081
   ```

4. Importeer de realm-export **eenmalig** wanneer de realm `restaurant` nog niet bestaat:

   ```powershell
   .\bin\kc.bat import --file "PAD\NAAR\restaurant-api\exports\restaurant-realm.json"
   ```

   Of gebruik in de Keycloak-beheerconsole **Create realm → Browse** en kies `exports/restaurant-realm.json`.
   De export bevat de clients `restaurant-api` en `restaurant-postman`, de rollen `CUSTOMER` en `STAFF`, en lokale demoaccounts.
   Bij een bestaande realm: voeg in de beheerconsole bij client `restaurant-postman` de Web origin `http://localhost:8080` toe als die ontbreekt. Een realm-import past bestaande clients niet altijd aan. Gebruik dezelfde hostnaam en poort; andere deployments vereisen passende redirect-URI's en Web origins.

5. Wacht tot Keycloak luistert op `http://localhost:8081`. Open `http://localhost:8081/realms/restaurant` om te controleren dat de realm beschikbaar is.

6. Bouw en controleer de applicatie met Java 21:

   ```powershell
   mvn clean verify
   ```

7. Start de API:

   ```powershell
   java -jar target/restaurant-api-1.0.0.jar
   ```

   Alternatief voor ontwikkeling: `mvn spring-boot:run`. De standaard H2-database wordt bewaard in `data/`. Demo-data wordt door `data.sql` toegevoegd als de betreffende voorbeelden ontbreken. Bestaande demo-rijen worden niet overschreven. Dit betekent ook dat verwijderde demo-rijen bij een volgende start terug kunnen komen.

8. Open `http://localhost:8080/` voor de webapp of `http://localhost:8080/api-docs.html` voor documentatie. `GET /api/products` zonder token hoort **401** op te leveren. Dat bevestigt dat de API beveiligd is.
9. Stop de API en Keycloak met Ctrl+C; gegevens blijven bewaard.

De API verwacht tokens van issuer `http://localhost:8081/realms/restaurant` en audience `restaurant-api`. Zonder een draaiende Keycloak-server op poort 8081 starten de JWT-validatie en de website-login niet.

## Inloggen en registreren

| Account | Wachtwoord voor lokale demo | Rol | Rechten |
|---|---|---|---|
| `klant` | `Klant-Demo-2026!` | CUSTOMER | Menukaart/tafels lezen, eigen reserveringen beheren, afbeeldingen downloaden |
| `personeel` | `Personeel-Demo-2026!` | STAFF | Alle API-functionaliteit |

Deze bekende wachtwoorden zijn uitsluitend voor lokaal testen en staan in de geïmporteerde realm-export. Gebruik voor een echte deployment eigen accounts, HTTPS en secretsbeheer. Stel voor optioneel Keycloak-beheer een eigen bootstrap-admin in (`KC_BOOTSTRAP_ADMIN_USERNAME` en `KC_BOOTSTRAP_ADMIN_PASSWORD`) voordat je Keycloak de eerste keer start.

**Website:** kies Customer login, Staff login of Create an account. Klanten kunnen zichzelf registreren. Nieuwe gebruikers krijgen automatisch CUSTOMER. Een beheerder kent na controle STAFF toe via Keycloak Users > gebruiker > Role mapping. Gebruikers kunnen zichzelf geen personeelsrechten geven.

**Postman / OAuth2:** Authorization Code + PKCE (S256), client `restaurant-postman`, scope `openid profile email`, callback `https://oauth.pstmn.io/v1/callback`. Autorisatie-URL: `http://localhost:8081/realms/restaurant/protocol/openid-connect/auth`. Token-URL: `http://localhost:8081/realms/restaurant/protocol/openid-connect/token`. Op het loginvenster staat Registreren.

Er is bewust geen eigen `/register` of `/login`-endpoint met wachtwoordopslag: Keycloak verzorgt deze functies via OpenID Connect/OAuth2. De lokale Postman-collectie bevat daarnaast password-grantverzoeken om eenvoudig te testen. Deze testclient staat direct grants toe; gebruik voor normale applicaties Authorization Code + PKCE en schakel direct grants uit.

De eenmalige **owner setup** op de website is bedoeld voor een lokale Windows-distributie met startvenster en setupcode. Die starters zitten niet in deze repository. Maak de eerste beheerder daarom via de Keycloak-beheerconsole aan, of gebruik het meegeleverde demo-account `personeel`.

## API gebruiken

1. Importeer `exports/restaurant.postman_collection.json` in Postman.
2. Voer in de map Authenticatie `Token klant` of `Token personeel` uit. De testscript slaat het access token op in de collectievariabele `token`.
3. Voer GET-verzoeken uit. Een POST slaat het ontvangen id automatisch op in de betreffende collectievariabele. Controleer referenties in JSON-bodies (bijvoorbeeld `categoryId`, `tableId`, `productId`) en kies bestaande ids.
4. Voor upload kies je in het multipart-formulier een bestand bij `file`. Laat Postman de multipart Content-Type met boundary genereren.
5. Een DELETE-reservering annuleert; andere DELETE-verzoeken verwijderen indien toegestaan. Voer DELETE niet uit voordat je de vervolgstappen van een workflow hebt getest.

Voorbeeld in PowerShell:

```powershell
$tokenResponse = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8081/realms/restaurant/protocol/openid-connect/token' `
  -Body @{grant_type='password';client_id='restaurant-postman';username='klant';password='Klant-Demo-2026!'}
$headers = @{Authorization = "Bearer $($tokenResponse.access_token)"}
Invoke-RestMethod -Uri 'http://localhost:8080/api/products' -Headers $headers
```

De complete endpointdocumentatie met invoer, uitvoer, autorisatie en foutcodes staat op `/api-docs.html`. Algemene antwoorden: 200 lezen/wijzigen, 201 aanmaken met Location-header, 204 verwijderen/upload, 400 invoerfout, 401 ontbrekend/ongeldig token, 403 onvoldoende rechten, 404 ontbrekende resource, 409 conflict, 413 te groot bestand, 415 verkeerd bestandstype. Applicatiefouten hebben `application/problem+json`; beveiligingsfouten kunnen een lege body en WWW-Authenticate-header hebben.

## Tests

```powershell
mvn clean verify
```

Tests gebruiken een afzonderlijke H2-geheugendatabase en veranderen de lokale database niet. Ze vereisen geen draaiende Keycloak-server. Integratietests lopen door controllers, services en echte repositories met MockMvc. Afzonderlijke JWT-tests gebruiken echte RSA-handtekeningen en een tijdelijke lokale JWKS-server. WebMvc-tests controleren de rolconversie uit Keycloak-claims.

- `PricingServiceTest`: 10 unit-tests voor berekening, precisie en ongeldig bestelbedrag.
- `ReservationPolicyTest`: 10 unit-tests voor capaciteit, tijd en eigenaarschap.
- `RestaurantIntegrationTest`: reserveringen, gelijktijdige boekingen, orders/facturen, afbeeldingsroundtrip en foutafhandeling.
- `JwtDecoderTest`: geldige tokens, verval, vervalste handtekening, verkeerde issuer/audience.
- `TableControllerWebMvcTest`: rolconversie, leesrechten en verboden mutaties.
- `OwnerSetupControllerTest`: lokale eigenaarsinrichting.

JaCoCo schrijft `target/site/jacoco/index.html` en `jacoco.xml`. De build faalt tenzij **PricingService én ReservationPolicy ieder 100% line coverage** hebben. Dit is geen claim dat de hele applicatie 100% coverage heeft. Testresultaten staan in `target/surefire-reports/`. Actuele resultaten en de grenzen van verificatie staan in `docs/verification.md`.

## Configuratie en PostgreSQL

| Variabele | Standaard | Functie |
|---|---|---|
| PORT | 8080 | API-poort |
| DB_URL | jdbc:h2:file:./data/restaurant;DB_CLOSE_ON_EXIT=FALSE | JDBC-verbinding |
| DB_USERNAME | sa | Databasegebruiker |
| DB_PASSWORD | leeg | Databasewachtwoord |
| JWT_ISSUER | http://localhost:8081/realms/restaurant | Vereiste tokenissuer |
| JWT_JWK_URI | http://localhost:8081/realms/restaurant/protocol/openid-connect/certs | Publieke verificatiesleutels |

Voor een afzonderlijk geinstalleerde PostgreSQL-server: maak eerst de database en gebruiker aan en configureer de verbinding:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/restaurant'
$env:DB_USERNAME = 'restaurant'
$env:DB_PASSWORD = 'LocalDatabase-ChangeMe!'
java -jar target/restaurant-api-1.0.0.jar
```

De JDBC-driver wordt uit de URL afgeleid. Wijzigingen aan repositories zijn niet nodig. Een databaseswitch kopieert bestaande gegevens niet automatisch. De meegeleverde configuratie gebruikt `ddl-auto=update` voor lokaal ontwikkelen. Gebruik voor productie gecontroleerde schema-migraties en zet demo-initialisatie uit met `SPRING_SQL_INIT_MODE=never`. H2 en de echte Keycloak-flow zijn afzonderlijk verifieerbaar; de PostgreSQL-route is alleen getest als dat expliciet in `docs/verification.md` staat.

Bij poortconflicten wijzig je PORT of stop je de oude eigen instantie. Bij 401 controleer je tokenverval, issuer en audience `restaurant-api`. Bij 403 controleer je de realm-rol en eigenaarschap. Bij Maven-fouten controleer je eerst `mvn -version`. Stel na het kiezen van een ander databaseprofiel alle drie databasevariabelen consequent in.

## Inleveren en openstaande punten

De applicatieteksten en de HTML-API-documentatie zijn Engelstalig; codecommentaar is Nederlandstalig. De HTML in `src/main/resources/static/` wordt rechtstreeks onderhouden.

- Lever het technisch ontwerp, verantwoordingsdocument en API-documentatie als **losse PDF's** in.
- Lever beide JSON-exports los in en de broncode. Deze repository bevat geen `target`, `.idea`, `.iml`, `.tools`, database of secrets.
- **Nog door Ali te bevestigen:** inleverdatum, goedkeuring ideefase, eigen persoonlijke reflecties en eventuele herkansingsfeedback. Conceptreflecties beschrijven technische bevindingen en doen geen uitspraken over Ali's persoonlijke leerervaring.
- **GitHub:** publieke repository en minimaal vijf echte, gemergede pull requests. Lokale commits vervangen deze eis niet. Zie `docs/submission-checklist.md`.
- AI-gebruik is vermeld in de documentatie; de ontvangen prompts staan in `docs/ai-prompts.md`. Controleer, begrijp en bespreek de code voordat je de opdracht inlevert.

Officiële referenties: [Spring Boot vereisten](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Spring Security JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html), [Keycloak realm-import](https://www.keycloak.org/server/importExport).
