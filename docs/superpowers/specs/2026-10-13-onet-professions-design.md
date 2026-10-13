# ~920 ametit O\*NET-ist: disain

Kuupäev: 2026-10-13 · Staatus: otsused tehtud (vt 5), ootab plaani kinnitamist

## 1. Miks

Praegu on AI asendatavuse testis 19 käsitsi kirjutatud ametit. Kasutaja, kelle ametit nimekirjas pole, peab valima „Other“ ja saab üldise tulemuse. Konkurent ReplacedByAI näitab 1016 ametit. See on täpselt O\*NET-SOC 2019 taksonoomia ametinimetuste arv, nii et nende allikas on O\*NET.

Eesmärk on, et peaaegu iga kasutaja leiaks oma ameti ja saaks tulemuse, mis põhineb tema ameti päris ülesannetel. Seejuures peab skoori metoodika jääma samaks ja seletatavaks.

## 2. Eesmärgid, mitte-eesmärgid, anti-eesmärgid

**Eesmärgid**
- Kõik O\*NET-i ametid, millel on ülesannete andmed (**923**, vt 3.1), on testis valitavad.
- Iga ameti ülesanded ja nende ajaline osakaal tulevad andmetest, mitte käsitsi.
- Import on korratav skript. O\*NET-i ja AEI uuendused loetakse sisse skripti uuesti käivitades.
- Praegused 19 käsitsi tehtud ametit jäävad alles ja on paremad kui genereeritud vasted.
- Ameti saab üles leida ka tavalise nime järgi („coder“, „nurse“), mitte ainult O\*NET-i ametliku nimetuse järgi.

**Mitte-eesmärgid (v1)**
- Eestikeelsed ametinimed (ESCO). Toode on praegu inglise keeles. ESCO–O\*NET vastavustabel on olemas, nii et selle saab hiljem lisada.
- Robootika ehk füüsiliste ülesannete automatiseerimine. Mõõdame AI tööriistade mõju, nagu praegu.
- Skoorija (`AiReadinessScorer`) valemi muutmine.
- Andmebaasi viimine. Kataloog jääb classpath'i JSON-iks.

**Anti-eesmärgid (läbikukkumine, isegi kui tehniliselt töötab)**
- Genereeritud ametite skoorid on süstemaatiliselt teisel skaalal kui käsitsi tehtud 19. Näiteks saab „Software Developers“ 25, kui meie käsitsi tehtud „Software developer“ saab 55. Siis on tulemused omavahel võrreldamatud.
- Tulemuste lehel on 30 pikka O\*NET-i lauset ja seda ei loe keegi.
- Valikuleht muutub 920 kaardi kerimiseks.
- O\*NET-i ja AEI litsentsi nõutud viide puudub.

## 3. Andmeallikad (kontrollitud 2026-10-13)

### 3.1 O\*NET 29.3 (USA tööministeerium, CC BY 4.0)
- 1016 ametinimetust, millest **923-l on ülesannete andmed**. Ülejäänud 93 on peamiselt „All Other“ koondkategooriad ilma ülesanneteta. Me ei luba „1016 ametit“, vaid ausalt umbes 920.
- Kasutame faile *Occupation Data*, *Task Statements*, *Alternate Titles* (otsingu jaoks) ja *Related Occupations* (`pivotRoles` jaoks).
- Versioon **29.3**, mitte uusim 31.x, sest AEI andmed põhinevad 29.3-l ja ülesannete tekstid peavad kokku sobima.

### 3.2 Anthropic Economic Index (Hugging Face `Anthropic/EconomicIndex`; andmed CC BY, kood MIT)
- `robot_exposure/robot_exposure_tasks.csv`: 923 ametit ja 18 796 ülesannet. Väljad: `onetsoc_code`, `task_id`, `task_statement`, **`time_share`** (summa ameti kohta on 1,000 ± 0,001), `physical` (Yes/No), `exposure_tier`.
  → **Ülesannete osakaal tuleb siit otse.** Mediaanametil on 20 ülesannet, miinimum 4, maksimum 40.
- `labor_market_impacts/task_penetration.csv`: 17 992 ülesannet, väärtused vahemikus 0..1, **93% on 0**. See näitab, kas ülesannet on Claude'iga **tegelikult** tehtud, mitte seda, kas AI **suudaks** seda teha. Tekstiga sobib kokku 18 700 / 18 796 ülesannet.
- `labor_market_impacts/job_exposure.csv`: 756 ametit, vahemik 0..0,745.

### 3.3 Miks AEI andmed ei sobi otse automatiseeritavuseks

Võrdlesin meie 19 ameti ülesannete eksponeeritust (Σ osakaal × automatiseeritavus) AEI `observed_exposure` väärtusega:

| Amet | Meie (0–100) | AEI observed (0–1) |
|---|---|---|
| Software developer | 55 | 0,29 |
| Accountant | 64 | 0,35 |
| Translator | 70 | 0,43 |
| Copywriter | 69 | 0,25 |
| Marketing | 54 | 0,65 |
| Customer support | 64 | 0,70 |
| Healthcare (RN) | 34 | 0,06 |
| Skilled trades (electrician) | 22 | 0,00 |

Järjestus on laias laastus sarnane, aga skaala erineb 2–3 korda ja mõni amet on lausa vastupidi (copywriter vs marketing). AEI mõõdab juba toimunud kasutust, meie skoor aga võimekust. Kui võtaksime AEI otse, muutuks skoori tähendus ja tekiks anti-eesmärgis kirjeldatud skaalaviga. → **Automatiseeritavus tuleb kombinatsioonist, kus AEI on kontroll, mitte valemi osa (vt 5, O1).**

## 4. Lahendus

### 4.1 Andmevoog

```
O*NET 29.3 txt ─┐
AEI CSV-d ──────┼─► tools/onet-import/build.py ─► onet_tasks.csv (vahefail, commititud)
                │                                      │
A: LLM-hinnangud (rate_tasks.py, rubric.md) ─┐         │
B: GPTs are GPTs sildid (kui saadaval) ──────┼─ blend ─► automation + observedUse
C: AEI penetration (kontroll + märgis) ──────┘         │
ametirühma mallid (templates.json) ──────────────────► │
LLM-i projektilaused (write_projects.py) ────────────► ▼
                                    backend/src/main/resources/ai-readiness/professions-onet.json
```

- Skript elab kaustas `tools/onet-import/` (Python 3, ainult standardteek ja `csv`). Allalaetud toorandmed ei lähe gitti (`.gitignore`), skript laeb need ise alla.
- **Vahefail `onet_tasks.csv` ja lõplik JSON commititakse.** Nii on hinnangute muutused PR-is diffina üle vaadatavad ja build ei sõltu internetist.

### 4.2 Iga ameti väljade päritolu

| Väli | Allikas |
|---|---|
| `id` | O\*NET-SOC kood, nt `onet-15-1252-00` (ei põrka kokku praeguste id-dega) |
| `name` | O\*NET *Occupation Data* `Title` |
| `category` | SOC peagrupp (2 esimest numbrit, 22 gruppi), nt „Computer and Mathematical“ |
| `tasks[].share` | AEI `time_share` × 100, ümardatud **suurima jäägi meetodiga**, et summa oleks täpselt 100. Seda nõuab `AiReadinessCatalog.validate()`. |
| `tasks[].automation` | A+B kombo (5, O1), kalibreeritud |
| `tasks[].observedUse` | C: AEI `penetration` üle läve (5, O1). Uus valikuline väli, käsitsi tehtud ametitel puudub. |
| `tasks[].name` | O\*NET-i ülesande lause. Pikkuse probleem on lahendatud kuvamisel (4.5), andmeid ei lühendata. |
| `humanEdge` | **Andmetest:** 3 ülesannet, mille `share × (1 − automation)` on suurim, ehk suure ajaga ja madala automatiseeritavusega. Genereerimist pole vaja. |
| `pivotRoles` | O\*NET *Related Occupations*, 3 esimest „Primary-Short“ seost |
| `aiTools` | Ametirühma mall (5, O2) |
| `roadmap` | Ametirühma mall annab faasid ja sisu. `project` on LLM-i kirjutatud üks lause selle ameti kohta (5, O2). |

### 4.3 Käsitsi tehtud 19 ametit

- `professions.json` jääb samaks ja läheb esimesena. Igale käsitsi tehtud ametile lisatakse väli `onetCodes` (üks või mitu koodi, nt healthcare → RN + arstid).
- Laadija peidab O\*NET-i ametid, mille kood on mõne käsitsi tehtud ameti `onetCodes`-is. Kasutaja ei näe topelt „Software developer“ ja „Software Developers“ rida.
- **Kalibreerimiskontroll:** importskript arvutab nende koodide genereeritud eksponeerituse ja trükib selle käsitsi tehtud väärtuse kõrvale. Kui keskmine erinevus on üle 10 punkti, peatub skript hoiatusega (anti-eesmärk 1).

### 4.4 Backend

- `CatalogModels.Profession` saab valikulised väljad `onetCode` ja `alternateTitles` (otsingu jaoks, frontendile ei saadeta).
- `AiReadinessCatalog` laeb mõlemad failid, rakendab 4.3 peitmise ja valideerib nagu praegu. Duplikaat-id peatab käivitamise.
- `GET /api/ai-readiness/professions` jääb samaks, aga vastuses on umbes 940 kirjet (umbes 70 kB).
- Uus `GET /api/ai-readiness/professions/search?q=…&limit=20` otsib nimest ja alternatiivnimetustest. Esimesena tulevad täpsed ja algusega vasted, siis sisaldavad.
- Skoorija valem ei muutu. `ProfessionTask` ja `TaskExposure` saavad valikulise välja `observedUse` (boolean), mille skoorija annab tulemusse edasi.
- **Testid:**
  - kataloogi test: kõik ametid laaditakse, osakaalude summa on 100, automatiseeritavus on 0..1, id-d on unikaalsed, käsitsi tehtud koodid on peidetud;
  - otsingu test: „nurse“ leiab RN-i, „coder“ leiab Software Developers.

### 4.5 Frontend

- **Valikuleht:** otsing on peamine. Ilma otsinguta näitame 19 käsitsi tehtud ametit („Popular“) ja SOC peagruppide filtrit. Otsing kasutab uut endpointi (debounce 150 ms) ja näitab kuni 20 tulemust.
- **Tulemuste ülesannete tabel:** näitame 6 suurima osakaaluga ülesannet ja rida „+ N more tasks (X% of the week)“, mis avaneb klikiga. Skoor arvestab kõiki ülesandeid.
- **Märgis „AI is already used for this“** ülesande juures, kui `observedUse` on tõene, koos lühikese selgitusega (tooltip), et see põhineb päris kasutusandmetel (Anthropic Economic Index).
- **Viide** tulemuse „Method“ plokki ja avalehele:
  > Occupation and task data: O\*NET 29.3, U.S. Department of Labor / ETA, CC BY 4.0. Task time shares: Anthropic Economic Index, CC BY.
  
  Kui B on kasutusel, lisandub viide Eloundou et al. (2024), „GPTs are GPTs“, *Science*. Lisaks lause, et automatiseeritavus on mudeli hinnang, mis on kalibreeritud ja kontrollitud.

## 5. Otsused (kasutaja, 2026-10-13)

### O1 – Automatiseeritavus: A+B+C kombo

Iga allikas katab teise nõrkust. Igal on oma roll:

1. **A – LLM-hinnang (alus).** Iga O\*NET-i ülesanne saab hinnangu 0..1 fikseeritud rubriigi järgi (`tools/onet-import/rubric.md`). Küsimus on: „Mis osa sellest ülesandest suudavad tänased AI tööriistad teha nii, et inimene ainult suunab ja kontrollib?“ Rubriigis on ankrunäited meie käsitsi tehtud ametite ülesannetest. Hinnatakse ühekordse partiina (Anthropic API). Tulemus läheb faili `ratings_llm.csv` ja see commititakse.
2. **B – GPTs are GPTs (ankur, valikuline).** Kui Eloundou et al. ülesannete sildid on alla laaditavad ja litsents lubab, teisendame sildid numbriks: E0 → 0,15, E2 → 0,55, E1 → 0,80. Neil ülesannetel on `automation = 0,5 × A + 0,5 × B`. Kui B-d ei saa kasutada, jääb `automation = A` ja dokumenti kirjutatakse põhjus. **Ülejäänud plaan ei sõltu B-st.**
3. **Kalibreerimine.** Pärast A+B segamist kontrollitakse käsitsi tehtud 19 ameti vasteid (4.3). Kui keskmine erinevus on üle 10 punkti, rakendatakse kõigile hinnangutele üks lineaarne korrektsioon `a × x + b`, mis sobitatakse nende 19 vaste järgi. Korrektsiooni kordajad trükitakse aruandesse.
4. **C – AEI `penetration` (kontroll ja märgis, mitte valemi osa).**
   - *Esimene samm:* kontrolli AEI labor-market väljalaske dokumentatsioonist, mida `penetration` täpselt mõõdab (93% on 0) ja kirjuta definitsioon siia.
   - *Kontroll:* kui `penetration` on kõrge, aga lõplik `automation` on alla 0,3, läheb ülesanne faili `review.csv` ülevaatamiseks.
   - *Märgis:* `observedUse = penetration ≥ lävi`. Lävi valitakse pärast definitsiooni kontrolli ja kirjutatakse siia.

### O2 – Tööriistad ja stardiplaan: mall ja LLM-i projektilause

- **22 ametirühma malli** (`tools/onet-import/templates.json`, üks iga SOC peagrupi kohta), käsitsi kirjutatud. Iga mall annab `aiTools` (3) ja kolm faasi (`title` ja `items`).
- **LLM kirjutab igale ametile igasse faasi ühe lause `project`**, mis on konkreetne selle ameti ülesannete põhjal. Sisend on ameti nimi, 5 suurima osakaaluga ülesannet ja faasi pealkiri. Max 200 tähemärki. Fail `projects_llm.csv` commititakse.
- Käsitsi tehtud 19 ametit jäävad muutmata.

## 6. Ehitaja otsustada (minu valikud, saab muuta)

- Ülesande nimi jääb O\*NET-i lauseks. Lühendamine oleks genereeritud tekst ja seda me v1-s ei tee.
- `onet_tasks.csv`, `ratings_llm.csv`, `projects_llm.csv` ja `professions-onet.json` on commititud genereeritud failid, buildi ajal neid ei genereerita.
- 4.3 kalibreerimise lävi on 10 punkti.
- B siltide teisendus E0/E2/E1 → 0,15/0,55/0,80 ja segamine 50/50.
- `humanEdge` valem: `share × (1 − automation)`.
- LLM-i käivituste mudel ja täpne hind arvutatakse enne käivitamist ja kinnitatakse kasutajaga (vajab Anthropic API võtit env muutujast, mitte koodis).

## 7. Etapid (iga etapp on eraldi PR)

1. **Import ilma automatiseeritavuseta:** skript, `onet_tasks.csv` (osakaalud, SOC grupid, seotud ametid, alternatiivnimetused, AEI `penetration`). Andmed alles kontrollitakse, rakendus ei muutu.
2. **Automatiseeritavus (O1):**
   - kontrolli C definitsiooni;
   - kontrolli B kättesaadavust ja litsentsi;
   - rubriik ja A hinnangud (hind kinnitatakse enne);
   - segamine, kalibreerimise aruanne meie 19 ameti vastu, `review.csv`.
3. **Kataloog ja backend:** mallid ja LLM-i projektilaused (O2), `professions-onet.json`, laadija, peitmine, `observedUse`, otsinguendpoint, testid.
4. **Frontend:** otsingupõhine valikuleht, kokkuvõetud ülesannete tabel, märgis „AI is already used for this“, viited.
5. **Testimine ja kokkuvõte:** kõik ametid ja vastusekombinatsioonid, aruanne eraldi failis. See on varem pausile pandud testimise ülesanne.

## 8. Riskid

- **USA-kesksed nimetused ja ülesanded** (nt USA õigussüsteem). Leevendus: käsitsi tehtud ametid on eespool. ESCO jääb hilisemaks.
- **O\*NET 29.3 vananeb.** Leevendus: skript on korratav. Uuema versiooni ülesannete tekstid ei pruugi AEI-ga kokku sobida, nii et uuendus nõuab uut AEI väljalaset.
- **LLM-hinnangud (A) võivad olla nihkes.** Leevendus: B ankur (kui saadaval), kalibreerimine meie 19 ameti järgi, AEI kontroll ja pisteline ülevaatus enne merge'i.

## Allikad

- O\*NET-SOC taksonoomia: https://www.onetcenter.org/taxonomy.html
- O\*NET andmebaas ja litsents: https://www.onetcenter.org/database.html
- Anthropic Economic Index: https://huggingface.co/datasets/Anthropic/EconomicIndex
- ESCO–O\*NET vastavustabel: https://esco.ec.europa.eu/en/about-esco/data-science-and-esco/crosswalk-between-esco-and-onet
- GPTs are GPTs: https://arxiv.org/abs/2303.10130
