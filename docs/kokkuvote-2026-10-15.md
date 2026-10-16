# Kokkuvõte: mis sai tehtud (15.–16. oktoober 2026)

Kõik allpool olev on 16. oktoobri hommikul `main` harusse merge'itud. Tootmisse ei ole midagi deploy'tud: CI ainult testib ja deploy käib eraldi (`deploy-vps.sh`).

## Merge'itud PR-id

| PR | Mis muutus |
|----|-----------|
| #50 | Nähtav brändinimi Futureproof → careerrisk.ee. |
| #51 | Kõik 899 ametit said oma AI õppimise roadmapi. Varem jagasid nad 22 ühist malli. |
| #53 | /jobs lehe kokkuvõtte viga parandatud. |
| #52 | Tulemuse lehest sai plaan: progress, esimene samm ja värske salvestatud tulemus. |
| #54 | Avalehel "Continue your plan" kaart naasvale kasutajale. |
| #55 | Iganädalane meeldetuletus kalendrisse (.ics). |
| #56 | Ööplaan ja ööraport (`docs/overnight-*.md`). |

## 1. Ametipõhised roadmapid (#51)

- Iga roadmap on üles ehitatud nii:
  - **3 faasi:** alusta sealt, kus AI juba aitab → tee see korratavaks → arenda seda, mida AI ei suuda;
  - igas faasis **3 sammu ja praktiline projekt**;
  - lisaks **3 AI tööriista** ja **3 inimlikku tugevust**.
- **Sisu allikas:** ameti 10 suurimat ülesannet O*NET andmetest ja seotud ametid.
- **Kvaliteedikontroll** (`tools/onet-import/roadmap_quality.py`) lükkab tagasi:
  - liiga lühikesed sammud, projektid ja pealkirjad;
  - tühisõnad;
  - korduvad sammud.

  Kontrolli mittelaabiv roadmap jäetakse välja ja selle asemel näidatakse malli.
- **Esimese automaatse katse 380 õhukest roadmapi visati välja** ja kirjutati käsitsi uuesti.
- **117 vanema roadmapi viimane samm kirjutati ümber.** Sabloonlause "Grow toward X, or look at related roles" asemel öeldakse nüüd, miks järgmine amet sobib.
- **Sisureeglid:**
  - kliendi-, patsiendi- ega lasteandmed ei lähe avalikesse AI tööriistadesse;
  - litsentsi nõudvad otsused jäävad inimesele;
  - palganumbreid ega töökoha kadumise ennustusi ei ole.
- **Füüsilise töö ametites** (kokk, müürsepp, metsatööline) jääb AI dokumentide, planeerimise ja õppimise juurde. Roadmap ütleb ausalt, mis jääb käte tööks.

## 2. /jobs lehe viga (#53)

Avalik leht ütles kokkade kohta: "AI saab teha ~13% tööst, peamiselt: toidu maitsestamine ja valmistamine". See ülesanne on vaid 12% automatiseeritav. Nüüd nimetatakse ülesannet ainult siis, kui AI suudab sellest teha vähemalt 40%. Muidu öeldakse, et AI aitab põhitöö ümber.

## 3. Tulemuse leht kui plaan (#52)

- **Salvestatud tulemus värskeneb.** Vastused salvestatakse koos tulemusega. Vana tulemuse avamisel arvutatakse see uue sisuga ümber, et varem testi teinud inimesed ei näeks vana malliplaani. AI kommentaar jääb alles.
  - Ilma vastusteta salvestatud tulemus näitab vihjet test uuesti teha.
- **Progress.** Iga roadmapi sammu ja projekti saab märkida tehtuks. Näha on loendur faasi kohta ja progressiriba kogu plaani kohta. Kõik salvestub brauserisse, kontot pole vaja.
- **"Start this week".** Kast pakub üht konkreetset esimest sammu: ülesanne, mille puhul AI aitab kõige rohkem aega säästa, tööriist proovimiseks ja esimene projekt.
  - Käsitöö-ametitele (alla 40% automatiseeritav) pakutakse ülesande asemel roadmapi esimest sammu.
- **Väike parandus.** "Take the test again" nullib nüüd ka AI kommentaari valiku.

## 4. "Continue your plan" (#54)

Kui brauseris on salvestatud tulemus, näitab avaleht kaarti ameti nime ja progressiga, näiteks "2 / 12 steps". Kaart avab otse tulemuse (`/ai-test?view=saved`).

## 5. Iganädalane meeldetuletus (#55)

"Start this week" kastis on nupp, mis laeb alla kalendrifaili (.ics):

- 12 nädalat, igal esmaspäeval kell 18;
- meeldetuletus 15 minutit enne;
- sessiooni pikkus sõltub testis märgitud nädalatundidest (60, 90 või 120 minutit);
- kirjelduses on esimene samm ja link plaanile.

Fail tehakse brauseris, serverisse ei saadeta midagi.

## Testid

- **Backend:** 380 testi on rohelised.
- **`onet-import`:** 83 testi on rohelised.
- **Frontend:** 96 testi on rohelised (öö alguses oli 80).
- **Production build:** on roheline.
- **CI:** roheline enne iga merge'i. #54 ja #55 olid kuhjatud teiste PR-ide peale, nende puhul kontrollisin uue `main`-iga liidetud seisu kohapeal (96/96 testi ja build).
- **Brauseris kontrollitud:**
  - vana tulemuse värskendamine;
  - linnukeste säilimine;
  - "Start this week" nii kontoritöö kui käsitöö ameti puhul;
  - mobiilivaade (390 px);
  - avalehe kaart;
  - .ics faili sisu.
- **Kontrollimata:** .ics faili import Google Calendarisse või Outlooki.

## Sinu otsust vajavad

1. **H2 andmebaasifail on gitis** (`backend/data/aiinterview-db.mv.db`). Selle tõttu ei käivitu `gradlew bootRun` puhtast checkoutist. Tootmist see ei mõjuta, sest seal on PostgreSQL.
   - Parandus on `git rm --cached` ja `.gitignore` kirje.
   - Pull kustutaks faili aga teiste arvutitest, seega jätsin selle tegemata.
2. **Peamises kaustas on commitimata muudatused**, mis pole minu tehtud: `AiReadinessCoach.java`, `AiReadinessService.java`, `OpenAiClient.java`. Ma ei puutunud neid.
3. **Tootmise deploy** (`deploy-vps.sh`) on tegemata. Uued roadmapid ja funktsioonid jõuavad careerrisk.ee-sse alles pärast seda.

## Järgmised ideed (backlog)

- Jagamislink tulemuselt ameti avalikule /jobs lehele.
- Võimalus muuta ülesannete osakaalu, kui sinu nädal erineb kataloogist.
- Tulemuse lehe "Starter plan" ümbernimetamine ametipõhiseks roadmapiks.
- Kataloogi eestikeelne tõlge (899 roadmapi). See on suunaotsus.
