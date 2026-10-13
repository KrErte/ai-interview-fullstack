# O\*NET import, etapp 4: frontend. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Kasutaja leiab oma ameti umbes 920 hulgast otsingu või ametirühma järgi. Tulemuse lehel on ülesannete tabel kokkuvõetud, märgis „AI is already used for this“ ja allikaviited.

**Architecture:** Backend saab rühmade nimekirja ja otsingu rühmafiltri. Frontendi `ai-check.page.ts` profession-samm teeb serveripoolse otsingu (debounce) ja tulemuste ülesannete tabel kuvab top 6 ning laiendatava ülejäänu.

**Tech Stack:** Spring Boot 3.3 / JUnit 5, Angular 19 standalone, RxJS, Tailwind (tume teema, `styles.css` tokenid).

**Spec:** `docs/superpowers/specs/2026-10-13-onet-professions-design.md` (4.5)

## Global Constraints

- **Rühm** on O\*NET-i SOC peagrupi nimi (`category` O\*NET-i ametitel). Käsitsi tehtud amet kuulub oma esimese `onetCodes` koodi rühma. `other`-il rühma pole, ja see jääb alati valikuna nähtavaks.
- Otsingu `limit` maksimum jääb 50. Kui rühmas on rohkem, kuvatakse „Showing 50 of N, type to narrow“.
- Frontend ei lae kunagi kõiki 918 ametit korraga.
- Kood on inglise keeles (toode on inglise keeles), stiil järgib `styles.css` klasse (`card`, `tile`, `kicker`, `btn-*`).
- Viide O\*NET-i ametitele (CC BY nõue): „Occupation and task data: O\*NET 29.3, U.S. Department of Labor / ETA, CC BY 4.0. Task time shares and observed AI use: Anthropic Economic Index, CC BY. Human exposure labels: Eloundou et al. (2024), *Science*. Automation estimates combine a Claude rating with those labels.“

## Review Focus

1. **Kiire trükkimine:** iga klahvivajutus ei tohi teha päringut ja vana vastus ei tohi uut üle kirjutada. Lahendus: `debounceTime(150)` ja `switchMap`.
2. **Otsing ilma tulemuseta** näitab nuppu „Other / I can't find my profession“, mitte tühja lehte.
3. **Salvestatud tulemus** (`localStorage`), mis tehti enne seda muudatust, peab avanema. `observedUse` puudub ja on valikuline.
4. **Pikk O\*NET-i nimi või rühm** (nt „Arts, Design, Entertainment, Sports, and Media“) ei lõhu mobiilis paigutust (390 px).
5. **Väike ülesannete arv:** käsitsi tehtud ametil (4–7 ülesannet) ei kuvata „+ 0 more tasks“ rida.

---

### Task 1: Backend: rühmad ja rühmafilter

**Files:** `AiReadinessService.java`, `AiReadinessController.java`, `AssessmentModels.java` (+ `record CategorySummary(String name, int count)`), test `AiReadinessSearchTest.java`

**Interfaces:**
- Produces:
  - `List<CategorySummary> categories()`: rühmad nime järgi sorteeritult koos ametite arvuga (käsitsi tehtud arvestatakse oma rühma);
  - `search(String query, String category, int limit)`: `category` null või tühi tähendab kõiki. Tühi päring koos rühmaga tagastab rühma ametid (käsitsi tehtud ees, siis tähestik).
  - Endpointid: `GET /professions/categories`, `GET /professions/search?q=&category=&limit=`.
- [x] **Testid:**
  - `categoriesHaveCounts`: 22 rühma, summa on 917 (918 miinus `other`)
  - `categoryFilterOnlyReturnsThatGroup`
  - `emptyQueryWithCategoryListsGroupFeaturedFirst`: „Computer and Mathematical“ algab `software-developer`-iga
  - olemasolevad otsingutestid kasutavad uut signatuuri

### Task 2: Frontend: teenus ja mudel

**Files:** `ai-readiness.service.ts`, `ai-readiness.model.ts`

- `searchProfessions(q: string, category: string | null, limit = 50): Observable<ProfessionSummary[]>`
- `categories(): Observable<CategorySummary[]>`

### Task 3: Frontend: valikuleht

**Files:** `ai-check.page.ts`

- Ilma päringu ja rühmata kuvatakse „Popular“, ehk 19 käsitsi tehtud ametit.
- Otsing käib `Subject` + `debounceTime(150)` + `distinctUntilChanged` + `switchMap` kaudu.
- Rühmad on rida nuppe („All fields“ + 22). Valitud rühm on aktsendiga.
- Tulemuste arv ja märkus „Showing 50 of N“ rühma piiri korral.
- Tühi tulemus näitab nuppu, mis valib `other`.
- Valitud amet salvestatakse `selectedProfession`-isse, mitte ei otsita nimekirjast.

### Task 4: Frontend: tulemuste leht

**Files:** `ai-check.page.ts`

- Ülesannete tabel: 6 suurima osakaaluga ülesannet automatiseeritavuse järjekorras. Kui ülesandeid on rohkem kui 6, tuleb rida „+ N more tasks (X% of the week)“, mis avab ülejäänud. 0% ülesanded on ainult laiendatud vaates.
- `observedUse` märgis: „AI already used“, väike aktsendiga pill, `title` selgitusega.
- Viide Method-plokis O\*NET-i ametitele (Global Constraints tekst) ja avalehe meetodiplokis lühike allikarida.

### Task 5: Kontroll brauseris

- `npm run build` ja `./gradlew test` peavad läbima.
- Docker rebuild, seejärel Playwrightis 1440 ja 390 px:
  - otsing („barista“, „nurse“);
  - rühma valik;
  - O\*NET-i ameti test lõpuni;
  - märgis ja „+ N more tasks“;
  - vana salvestatud tulemus.
- **Pärast ekraanipilte `browser_close`.**
