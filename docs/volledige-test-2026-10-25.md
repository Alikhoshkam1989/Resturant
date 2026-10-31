# Uitgebreide controle van 25 oktober 2026

## Geslaagd

- Maven verify: 37 tests, geen fouten, alle coveragecontroles geslaagd.
- Frontend: 19 jsdom-tests op de echte applicatiecode. Rollen, veilige
  tekstweergave, zoeken, aanmaken/bewerken/verwijderen, UTC-omrekening, bestelregels,
  facturen, afbeeldingsupload, sessievernieuwing en foutafhandeling zijn getest.
- Live API: 92 HTTP-controles met echte Keycloak-tokens op een uit de ZIP gestarte
  applicatie en een tijdelijke H2-database. Inclusief alle zes resources,
  401/403, eigenaarschap, overlap, validatie, prijsbehoud, betaling, verwijderen,
  PNG-upload/download, beschadigde bestanden, bestandstype en groottebeperking.
- Registratie, PKCE, de website-callback, CORS, tokenvernieuwing, de afmeldredirect
  en het ongeldig maken van het refresh-token na afmelden zijn geslaagd tegen
  een tijdelijke lokale Keycloak-installatie.
- De starter weigert Java 10 met een duidelijke melding en controleert of de poort
  al bezet is voordat een tweede server wordt gestart.
- De bijgewerkte starter bereikt op een vrije poort de applicatie en `/app-config`.
  De JAR-integriteitscontrole slaagt; HTML, JavaScript en CSS komen overeen met
  de bronbestanden. De nagekeken codecommentaren bevatten geen Duitse tekst.
- De publicatiescript heeft vier echte commits en pushes naar een uitsluitend
  lokaal testrepository uitgevoerd. De gemeten afstanden zijn 242, 252 en 249
  seconden, dus telkens minstens vier minuten. Er zijn geen datums vervalst.
  De uploader is niet meegecommit, de bronmap heeft geen nieuwe Git-geschiedenis
  en een niet-leeg doel wordt geweigerd. De testharnas moest voor die verwachte
  fout afzonderlijke stderr-captatie gebruiken; de weigering is opnieuw bevestigd.
- De hoofdteksten, secundaire teksten, accenttekst en statuslabels voldoen volgens
  berekening aan een contrastverhouding van minstens 4,5:1. Dit is geen volledige
  toegankelijkheidsaudit of visuele browsercontrole.

## Opgeloste bevindingen

- Een verlopen sessie tijdens opslaan sloot het formulier en verborg daarbij de
  foutmelding. De melding verschijnt nu op de homepage als het formulier gesloten is.
- De starter kon bij een reeds draaiende server tijdelijk de verkeerde instantie
  als gereed herkennen. Een poortcontrole voorkomt dit.
- Het focuskader is donkerder gemaakt voor beter contrast. Lange factuurbedragen
  en productnamen mogen afbreken om kleine schermen niet te laten overlopen.

## Niet bevestigd of geblokkeerd

- De beschikbare browserkoppeling blokkeert toegang tot de lokale pagina.
  Echte kliktesten, screenshots, responsive rendering en standaardbrowser-opening
  zijn niet bevestigd. DOM-tests en HTTP-tests zijn geen vervanging hiervoor.
- De oorspronkelijke externe infrastructuurtest was geblokkeerd. Het actuele
  pakket bevat een lokale Keycloak-runtime; zie de actuele startcontrole.
- De computer, GitHub-toegang en configuratie van de vriend zijn niet beschikbaar.
  Er is niets op GitHub gepubliceerd.
