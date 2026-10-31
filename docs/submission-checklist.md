# Inleverchecklist - Backend V4.2

## Lokaal opgeleverd

- [x] Spring Boot REST API met Java 21 en Maven.
- [x] Vier user stories en 32 genummerde eisen: 23 functioneel en 9 niet-functioneel.
- [x] Acht relationele entiteiten; één-op-één- en één-op-veel-relaties, foreign keys en constraints.
- [x] Klant- en personeelsrechten, eigenaarscontrole en OAuth2/JWT-validatie.
- [x] Zelfregistratie via Keycloak met standaard CUSTOMER; STAFF wordt beheerd toegekend.
- [x] CRUD voor menukaart, tafels, reserveringen, bestellingen en facturen volgens bedrijfsregels.
- [x] Veilige PNG/JPEG-upload en download.
- [x] Repositories, input/output-DTO's, validatie en centrale foutafhandeling.
- [x] data.sql met bruikbare voorbeeldgegevens.
- [x] Twintig nuttige unit-tests voor twee serviceklassen, elk 100% line coverage.
- [x] Vijf integratietests plus aanvullende WebMvc- en cryptografische JWT-tests.
- [x] Echte Keycloak-login en registratie met Authorization Code + PKCE lokaal getest.
- [x] Technisch ontwerp als PDF met titel, inhoudsopgave, klassendiagram en twee sequentiediagrammen.
- [x] Conceptverantwoordingsdocument als PDF met zes keuzes en zes limitaties, inclusief technische reflecties.
- [x] README-installatiehandleiding en API-documentatie als PDF en lokale HTML-pagina.
- [x] Postman-collectie en importeerbare Keycloak-realm als losse JSON-bestanden.
- [x] Broncode-ZIP zonder buildbestanden, IDE-mappen, lokale databases of gedownloade runtimes.
- [x] Minimaal twintig beschrijvende lokale Git-commits, gegroepeerd per implementatieonderdeel.
- [x] AI-bronvermelding en bijlage met de gebruikte gebruikersprompts.

## Nog door de student af te ronden

- [ ] Definitieve inleverdatum invullen in `docs/metadata.json` en PDF's opnieuw genereren.
- [ ] Bevestigen dat de ideefase een GO heeft gekregen.
- [ ] Per technische keuze en limitatie de eigen leerervaring toevoegen; de huidige reflecties zijn technische conceptanalyses.
- [ ] Eigen publieke GitHub-repository aanmaken of opgeven, broncode uploaden en URL in `docs/metadata.json` invullen.
- [ ] Minimaal vijf **echte** feature-pull-requests naar main mergen en hun links als bewijs opnemen. Lokale commits en een achteraf geschreven lijst vervangen die PR's niet.
- [ ] Alleen indien van toepassing: het officiële herkansingsfeedbacktemplate invullen.
- [ ] Documenten zelf nalopen en de code kunnen toelichten tijdens beoordeling.

## Technische verificatiegrens

De H2-configuratie, echte Keycloak-realm, rollen en OAuth2-flows zijn lokaal getest. De optionele PostgreSQL-route is niet uitgevoerd. Voor een databaseswitch of deployment moeten die omgeving, migraties en backups afzonderlijk getest worden.

## Definitief pakket maken

1. Vul `docs/metadata.json` en persoonlijke reflecties in `scripts/build_documents.py` aan.
2. Voer `python scripts/build_documents.py` uit en controleer de gegenereerde PDF's.
3. Voer `mvn clean verify` uit met Java 21.
4. Voer `python scripts/package_submission.py` uit.
5. Lever de zes bestanden uit `output/submission/` in: drie losse PDF's, twee losse JSON-exports en één broncode-ZIP.

Het pakket is een werkende lokale implementatie met documentatieconcepten; zolang de open punten hierboven ontbreken, is het niet volledig gereed voor beoordeling.
