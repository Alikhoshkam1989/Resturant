# Startcontrole van 25 oktober 2026

- Starten met de standaard Java 10 faalt met `UnsupportedClassVersionError`.
- De Windows-starter vindt het lokale JDK 21.0.12.1.
- Maven verify met JDK 21 inclusief de webapp: 37 tests zonder fouten of overgeslagen tests;
  alle coveragecontroles geslaagd.
- De extra taaltest bevestigt Engelse validatiemeldingen bij een Duitse
  Accept-Language-header. Codecommentaar is gecontroleerd en Nederlandstalig.
- De uitvoerbare JAR start met een tijdelijke H2-geheugendatabase op poort 18080.
  `/api-docs.html` geeft HTTP 200; `/api/products` zonder token geeft HTTP 401.
  De testserver is daarna gestopt.
- De nieuwe starter is zonder browservenster getest op poort 8080 met een
  tijdelijke database. De homepage en publieke aanmeldconfiguratie werken.
- Zeven aanvullende jsdom-tests slagen: rollen, veilige tekstweergave,
  reserveringsformulier en UTC, categoriebeheer, dubbele bestelregels,
  factuur-ID's/betaalstatus en PKCE-state.
- Met een tijdelijke lokale Keycloak-geheugendatabase zijn echte klant- en
  personeelstokens getest, inclusief 401/403, catalogusbeheer, reservering en
  annulering, besteltotaal, betaling en bescherming van gefactureerde bestellingen.
- Registratie, de website-callback, PKCE, CORS en tokenvernieuwing zijn geslaagd.
- De browserkoppeling werd door het toegangsbeleid geblokkeerd. Visuele controle
  en echte browserinteracties zijn daarom niet bevestigd; de DOM-tests en echte
  HTTP-tests zijn afzonderlijk uitgevoerd en vormen geen visuele browsertest.
- Het publicatiescript is in voorbeeldmodus getest onder Windows PowerShell,
  ook vanuit een uitgepakte ZIP zonder `.git`. Er is niets naar GitHub geupload
  en er zijn geen publicatiecommits gemaakt.
