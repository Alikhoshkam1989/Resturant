# Requirements en traceerbaarheid

## US1

Als klant wil ik mij registreren en veilig inloggen, zodat ik mijn eigen reserveringen kan beheren zonder toegang tot gegevens van andere klanten.

## US2

Als klant wil ik een beschikbare tafel reserveren, mijn reservering wijzigen en annuleren, zodat mijn restaurantbezoek passend en actueel is.

## US3

Als personeelslid wil ik bestellingen met meerdere producten beheren en daarvoor een factuur met betaalstatus maken, zodat ik de bediening en afrekening centraal kan afhandelen.

## US4

Als personeelslid wil ik producten, categorieën, tafels en productafbeeldingen beheren, zodat klanten en medewerkers actuele restaurantgegevens gebruiken.

| Eis | Story | Acceptatiecriterium |
|---|---|---|
| F01 | US1 | Nieuwe accounts kunnen zich via de Keycloak-loginpagina registreren en krijgen automatisch CUSTOMER. |
| F02 | US1 | Keycloak authenticeert klant en personeel en geeft een ondertekend access token uit voor restaurant-api. |
| F03 | US1 | Alleen een Keycloak-beheerder kan STAFF toekennen; zelfregistratie geeft geen personeelsrechten. |
| F04 | US1 | Een klant kan geen reservering van een ander lezen, wijzigen of annuleren; de API antwoordt 403. |
| F05 | US2 | Een ingelogde gebruiker kan met naam, tafel, UTC-begintijd, aantal gasten en opmerkingen een reservering maken. |
| F06 | US2 | De API accepteert alleen een toekomstige begintijd en berekent de eindtijd als begintijd plus twee uur. |
| F07 | US2 | De API weigert reserveringen voor een niet-inzetbare tafel en groepen groter dan de capaciteit. |
| F08 | US2 | De API weigert overlappende actieve reserveringen voor dezelfde tafel; aansluitende tijdvakken zijn toegestaan. |
| F09 | US2 | De eigenaar en personeel kunnen een actieve reservering wijzigen; een geannuleerde reservering blijft vergrendeld. |
| F10 | US2 | DELETE annuleert een reservering met behoud van historie en geeft het tijdvak vrij. |
| F11 | US2 | GET reserveringen toont klanten alleen hun eigen lijst en personeel de volledige lijst. |
| F12 | US3 | Personeel kan een bestelling met een tafel en 1 tot 100 productregels maken, bekijken, wijzigen en verwijderen. |
| F13 | US3 | Een product mag per bestelling eenmaal voorkomen met een hoeveelheid van 1 tot 100; onbeschikbare producten worden geweigerd. |
| F14 | US3 | De server berekent het totaal uit vastgelegde eenheidsprijzen maal aantallen met decimale precisie. |
| F15 | US3 | Bestaande bestelregels behouden productnaam en eenheidsprijs wanneer de menukaart verandert. |
| F16 | US3 | Personeel kan maximaal één factuur per bestelling aanmaken en deze lezen; het bedrag komt uit de bestelling. |
| F17 | US3 | Een gefactureerde bestelling kan niet worden gewijzigd of verwijderd zolang de factuur bestaat. |
| F18 | US3 | Personeel kan een factuur als betaald markeren; betaald kan niet terug naar onbetaald of worden verwijderd. |
| F19 | US3 | Een onbetaalde factuur mag worden verwijderd, zodat een foutieve bestelling opnieuw bewerkt kan worden. |
| F20 | US4 | Personeel kan categorieën en producten aanmaken, lezen, wijzigen en verwijderen indien ongebruikt. |
| F21 | US4 | Personeel kan tafels beheren; gebruikte tafels en capaciteit van gereserveerde tafels worden beschermd. |
| F22 | US4 | Klanten en personeel kunnen producten, categorieën en tafels lezen; mutaties vereisen STAFF. |
| F23 | US4 | Personeel kan per product één PNG/JPEG-afbeelding uploaden of vervangen; klanten en personeel kunnen die downloaden. |
| N01 | US1 | Ieder beveiligd verzoek valideert JWT-handtekening, issuer, audience en verloopdatum; ontbrekende tokens geven 401. |
| N02 | US2 | Reserveringscontrole en opslag gebeuren in één transactie met een schrijflock op de tafel; twee gelijktijdige aanvragen kunnen niet beide slagen. |
| N03 | US3 | JPA foreign keys, unieke factuur-orderkoppeling, versienummers en numerieke constraints bewaken database-integriteit. |
| N04 | US4 | Uploads zijn maximaal 2 MiB en 16 miljoen pixels; inhoud wordt gedecodeerd en bestandspaden komen nooit uit de bestandsnaam. |
| N05 | US1-4 | De Java 21/Spring Boot API gebruikt afzonderlijke controllers, services, repositories en DTO-records; entities worden niet als HTTP-respons gebruikt. |
| N06 | US1-4 | Bekende fouten worden als 400/403/404/409/413/415 afgehandeld; interne foutdetails worden niet aan de client getoond. |
| N07 | US2-3 | Maven verify voert minimaal twee integratietests en twintig unit-tests uit; PricingService en ReservationPolicy halen elk 100% line coverage. |
| N08 | US1-4 | De JDBC-configuratie is extern instelbaar; H2 is standaard en een PostgreSQL-driver wordt meegeleverd. |
| N09 | US4 | data.sql plaatst bij starten bruikbare demonstratiegegevens; herstart overschrijft bestaande voorbeelden niet. |
