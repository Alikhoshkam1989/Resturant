# Implementatiekeuzes en reflectieconcept

Persoonlijke leerervaringen moeten door Ali worden toegevoegd.

## Tafel vergrendelen vóór overlapcontrole

ReservationService.apply() haalt de tafel op met findLockedById(). Alleen eerst zoeken naar overlap is onvoldoende: twee transacties kunnen tegelijk niets vinden. De tafellock serialiseert aanvragen voor dezelfde tafel; verschillende tafels kunnen onafhankelijk worden verwerkt.

Technische reflectie: Een globale Java-lock zou maar één proces beveiligen. De database-lock werkt ook bij meerdere API-processen. De concurrentietest laat zien dat precies één aanvraag slaagt. Voor productie zouden lockwachttijden gemeten moeten worden.

## Prijzen en namen als momentopname

OrderLine bewaart unitPrice en productName naast de productrelatie. OrderService neemt die bij toevoegen over en behoudt ze voor bestaande productregels bij wijziging. Het totaal wordt met BigDecimal uit deze regels berekend.

Technische reflectie: Dynamisch de actuele catalogusprijs lezen zou bestaande afspraken wijzigen. De momentopname kost extra opslag, maar houdt de rekening uitlegbaar. Een toekomstige prijswijzigingsfunctie moet daarom expliciet nieuwe afspraken vastleggen.

## Identity buiten het domein

Keycloak beheert registratie, wachtwoorden en rollen. Reservation bewaart alleen customerSubject uit het gevalideerde JWT. Een klant kan geen owner of rol in de requestbody kiezen. Een audiencevalidator beperkt tokens tot restaurant-api.

Technische reflectie: Een eigen wachtwoordtabel zou dubbele beveiligingsverantwoordelijkheid creëren. De gekozen koppeling via subject houdt klantidentiteit stabiel bij een naamswijziging. Accountverwijdering vraagt wel een afzonderlijk beleid voor historische reserveringen.

## DTO-mapping binnen de transactie

Views.of() wordt in de services aangeroepen, terwijl de transactie actief is. Controllers ontvangen uitsluitend records; open-in-view staat uit. Daardoor blijven database-entiteiten en relaties binnen de backendlaag.

Technische reflectie: Mapping in een controller lijkt overzichtelijk maar kan luie relaties na het sluiten van de transactie openen. De huidige aanpak maakt de databasegrens expliciet. Bij grotere lijsten zijn gerichte DTO-projecties efficiënter.

## Afbeeldingsbytes in de database

ProductImage is een optionele één-op-één-entiteit. Uploads worden op magic bytes, decodeerbaarheid, bestandsgrootte en pixelgrens gecontroleerd. De opgegeven bestandsnaam wordt niet opgeslagen als pad. Metadata en inhoud worden samen in één transactie gewijzigd.

Technische reflectie: Een lokale uploadmap vraagt synchronisatie tussen filesystem en database en geeft risico op padmanipulatie. Database-opslag is eenvoudig voor kleine afbeeldingen, maar grotere volumes vragen objectopslag en gecontroleerde levenscyclus.

## Factuur als grens voor wijzigingen

InvoiceService vergrendelt de bestelling en maakt maximaal één factuur. OrderService weigert mutaties zodra een factuur bestaat. Betaald kan niet ongedaan worden gemaakt; een onbetaalde factuur kan bewust worden verwijderd.

Technische reflectie: Zonder deze grens zou een factuurbedrag afwijken van een later gewijzigde order. De keuze houdt dit project begrijpelijk, maar ondersteunt nog geen deelbetaling, terugbetaling of creditnota. Dat vraagt een afzonderlijk betalingsmodel.

# Limitaties

## Vaste reserveringsduur en één tafel

Elke reservering duurt twee uur en gebruikt één tafel. Grote gezelschappen of verschillende verblijfsduur kunnen niet worden gepland.

Vervolg: Voeg configureerbare duur, openingstijden en combinaties van tafels toe. Dit verandert de beschikbaarheidsberekening en vergt scenario’s voor gedeeltelijke overlap.

Reflectie: De huidige afbakening maakt conflictpreventie toetsbaar. Een gesprek met het restaurant moet eerst vaststellen welke planningsregels werkelijk nodig zijn.

## Geen keukenworkflow of voorraad

Een bestelling is een verzameling regels met een totaal. Er zijn geen statussen voor bereiden, serveren of ingrediëntvoorraad.

Vervolg: Voeg orderstatusovergangen, keukenoverzichten en voorraadmutaties toe met traceerbare gebeurtenissen.

Reflectie: Beschikbaarheid per product is voldoende voor de opdracht, maar kan geen voorraadtekort tijdens drukte verklaren. De volgende versie moet operationele gebeurtenissen modelleren.

## Eenvoudige afrekening

De factuur bevat bedrag, datum en betaalstatus, maar geen btw-uitsplitsing, deelbetalingen, betaalprovider of creditnota’s.

Vervolg: Introduceer factuurregels met tariefmomentopnamen en een apart betalingsregister; koppel daarna pas een betaalprovider.

Reflectie: Een boolean voorkomt onnodige complexiteit in dit onderwijsproject. Voor feitelijk restaurantgebruik is meer financiële functionaliteit nodig; dat is een inhoudelijke vervolgopdracht.

## Geen bevestigingen of herinneringen

Een reservering wordt alleen in de API opgeslagen. Klanten ontvangen geen mail, sms of herinnering en er is geen wachtlijst.

Vervolg: Voeg een notificatiewachtrij met herhaalpogingen en voorkeuren toe; maak een wachtlijst met expliciete toewijzing.

Reflectie: Een bevestigingsbericht moet pas na een succesvolle transactie worden verstuurd. Een outboxpatroon voorkomt berichten voor teruggedraaide boekingen.

## Beperkte zoek- en beheermogelijkheden

Lijsten zijn niet gepagineerd en er zijn geen datumfilters, rapportages of retentie- en accountverwijderflows.

Vervolg: Voeg paginering, gerichte filters, rapportagequery’s en een beleid voor historische persoonsgegevens toe.

Reflectie: De kleine testset maskeert grote datavolumes. Querymetingen en concrete beheerbehoeften horen vooraf te gaan aan verdere optimalisatie.

## Lokale infrastructuur en media

De standaard is H2 met schema-update en kleine afbeeldingen in de database. Geautomatiseerde schema-migraties, monitoring, backupherstel en objectopslag ontbreken.

Vervolg: Gebruik gecontroleerde migraties, databasebackup/restore-tests, metrics en objectopslag vóór een grotere uitrol.

Reflectie: Het lokale pakket is reproduceerbaar, maar een geslaagde lokale test is geen bewijs voor productiecapaciteit. De PostgreSQL-route moet op de doelomgeving apart worden geverifieerd.
