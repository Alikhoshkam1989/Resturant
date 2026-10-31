# Webapp: The Table

## Gebruik

De homepage staat op `http://localhost:8080/`. De volledige applicatie wordt vanuit
dezelfde Spring Boot-JAR aangeboden. Er is geen aparte frontend-build of Node.js
nodig om de applicatie te gebruiken. Java 21 en Keycloak blijven vereist.

- Klanten: menukaart doorzoeken, reserveringen maken, wijzigen en annuleren.
- Personeel: daarnaast categorieen, gerechten, foto's en tafels beheren,
  bestellingen met meerdere regels maken/wijzigen, facturen maken en betalen.
- Gefactureerde bestellingen en betaalde facturen zijn in de interface beschermd;
  de backend controleert deze regels onafhankelijk opnieuw.
- Datums worden lokaal ingevoerd en getoond; de API ontvangt UTC zonder suffix.
- Niet-beschikbare services geven een zichtbare foutmelding. Er worden geen
  fictieve boekingen of succesvolle wijzigingen getoond.

## Aanmelding

De startpagina biedt aparte ingangen: Customer login, Staff login en Restaurant
owner login. Klanten kunnen zichzelf registreren. De personeelsingang
vraagt opnieuw om aanmelding en controleert de STAFF-rol; de API blijft de
werkelijke autorisatie afdwingen. De eigenaarsingang opent de bestaande beveiligde
Keycloak-beheerconsole. Via Create owner account kan de eerste eigenaar direct
op de website een gebruikersnaam, e-mailadres en wachtwoord kiezen. De eenmalige
code staat in het Start-Restaurant-venster. Registratie werkt alleen op de computer
waar de applicatie draait en wordt na succes gesloten. Een tijdelijk serviceaccount
wordt vooraf met de officiele bootstrap-admin-opdracht voorbereid en na succes
verwijderd. De lokale instellingen staan met beperkte Windows-bestandsrechten in
data/owner-setup; deze map wordt nooit meegeleverd. Bestaande databases blijven
behouden. Sluit bij een upgrade eerst de app en aanmeldservice en start het nieuwe
pakket opnieuw. Een bestaande installatie kan hiermee een eigenaar toevoegen;
bestaande beheerders blijven behouden. Voor dagelijks
restaurantbeheer gebruikt de eigenaar een afzonderlijk personeelsaccount.

Authorization Code met PKCE S256 via Keycloak. De browser controleert de state en
de ouderdom van het aanmeldverzoek. Tokens staan alleen in sessionStorage van het
tabblad; vernieuwing gebeurt bij verlopen toegangstokens. Afmelden verwijdert de
lokale sessie en meldt af bij Keycloak. De server blijft alle JWT's en rollen
controleren. `/app-config` bevat uitsluitend publieke issuer- en clientinformatie.

De client `restaurant-postman` heeft voor de webapp Web origin
`http://localhost:8080` nodig. Deze staat in de bijgewerkte realm-export. Voor een
bestaande realm moet deze origin via de Keycloak-beheerconsole worden toegevoegd.
Verwijder daarvoor geen bestaande databases of volumes.

## Vormgeving

De gevraagde UI/UX-Pro-Max-skill is geraadpleegd vanuit
https://github.com/nextlevelbuilder/ui-ux-pro-max-skill.
De zoekopdracht `restaurant reservation minimal` met variance 2 en density 4
adviseert minimalisme, duidelijke hierarchie en functionele tussenruimte.
Volgens de gebruikerswens is gekozen voor rustig gebroken wit, donkergroen en
een klein terracotta-accent in plaats van de nadrukkelijke rode aanbeveling.
Systeemlettertypen voorkomen externe fontdownloads. Knoppen hebben zichtbare
focus, minimaal 44 px hoogte en tekstlabels. Formulieren gebruiken native
invoervelden en dialogen. Er zijn geen decoratieve animaties; verminderde beweging
wordt gerespecteerd. De interface en meldingen zijn Engels; commentaar Nederlands.

## Frontendtests

`scripts/frontend.test.cjs` test de echte applicatiecode in jsdom zonder een
browservenster. Deze tests vervangen geen visuele browsercontrole.
Optionele ontwikkelafhankelijkheden kunnen in de genegeerde map tmp worden gezet:

```powershell
New-Item -ItemType Directory -Path tmp/frontend-dom -Force
npm install --prefix tmp/frontend-dom jsdom@30.1.3
$env:NODE_PATH = (Resolve-Path tmp/frontend-dom/node_modules).Path
node --test scripts/frontend.test.cjs
```

De tests controleren rollen, veilige tekstweergave, formulieren, UTC-omrekening,
dubbele bestelregels, factuur-ID's, betaalstatus, uploads, foutafhandeling,
sessievernieuwing en PKCE-state. Maven verify
controleert daarnaast de backend en publieke beschikbaarheid van de webbestanden.

`python scripts/full_smoke_test.py` controleert alle API-onderdelen met echte
demoaccounts. Gebruik hiervoor uitsluitend een tijdelijke database: deze test
maakt eigen gegevens aan en test ook betalingen en verwijderacties.
`powershell -File scripts/test-publisher.ps1` controleert vier commits en pushes
in een tijdelijk lokaal bare Git-repository, met echte pauzes van vier minuten.
Een geisoleerde Git-configuratie leidt alleen de test-URL naar de lokale schijf;
er worden geen GitHub-gegevens of persoonlijke Git-instellingen gewijzigd.
