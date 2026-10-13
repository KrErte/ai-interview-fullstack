# O\*NET import, etapp 3: kataloog ja backend. Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Backend teenindab umbes 920 O\*NET-i ametit. Hindamine töötab iga ameti peal ja otsing leiab need nime ning alternatiivnimetuse järgi. Praegune frontend jääb muutmata tööle.

**Architecture:**
- Python-generaator (`tools/onet-import/catalog.py`) kirjutab etappide 1–2 väljundist ja 22 ametirühma mallist faili `backend/src/main/resources/ai-readiness/professions-onet.json`.
- `AiReadinessCatalog` laeb käsitsi tehtud ja genereeritud ametid ning peidab O\*NET-i ametid, mis dubleerivad käsitsi tehtuid (`onetCodes`).
- Uus endpoint `GET /api/ai-readiness/professions/search`.

**Tech Stack:** Python 3 stdlib, Java 17, Spring Boot 3.3, JUnit 5, Gradle.

**Spec:** `docs/superpowers/specs/2026-10-13-onet-professions-design.md` (4.2, 4.3, 4.4, 5 / O2)

## Global Constraints

- Ühilduvus: `GET /api/ai-readiness/professions` tagastab **ainult käsitsi tehtud ametid** (19), et praegune frontend ei peaks muutuma. O\*NET-i ametid tulevad otsingust (etapp 4 frontend).
- Iga ameti `tasks[].share` summa on 100 ja `automation` on 0..1. `AiReadinessCatalog.validate()` peatab käivituse muul juhul.
- O\*NET-i ameti id: `onet-` + kood, kus `.` asendatakse `-`-ga, nt `onet-15-1252-00`.
- `observedUse` on valikuline. Käsitsi tehtud ametitel puudub ja loetakse `false`-ks.
- Skoorija valem ei muutu.
- Genereeritud JSON commititakse. Genereerimine ei jookse buildi ajal.
- **O2 projektilause:** selles etapis tuleb `project` ametirühma mallist. LLM-i ametipõhine lause on Task 6 ja vajab API võtit ning kasutaja hinnakinnitust.

## Review Focus

1. **Otsing tühja, tühikutega või väga lühikese päringuga** ei tohi visata viga ega tagastada 940 rida. Tühja päringu korral tagastatakse käsitsi tehtud ametid ja `limit` piiratakse vahemikku 1..50.
2. **Käsitsi tehtud ameti duplikaat:** päring „software developer“ peab esimesena andma käsitsi tehtud ameti ja peidetud `Software Developers` ei tohi tulemustes olla.
3. **Tõstutundlikkus ja diakriitika:** „NURSE“ ja „nurse“ annavad sama tulemuse.
4. **Hindamine O\*NET-i id-ga:** POST `/assess` id-ga `onet-29-1141-00` peab töötama, aga peidetud id annab 400, mitte 500.
5. **Käivitusaeg ja mälu:** umbes 5 MB JSON laeb ühe korra. Test kontrollib, et kataloog laeb ja valideerib kõik ametid.

---

### Task 1: Ametirühma mallid ja generaator

**Files:**
- Create: `tools/onet-import/templates.json`, `tools/onet-import/catalog.py`, `tools/onet-import/test_catalog.py`
- Create (genereeritud): `backend/src/main/resources/ai-readiness/professions-onet.json`

**Interfaces:**
- Produces: `templates.json` = `{ "<soc_major>": { "aiTools": [3 × str], "roadmap": [3 × {"title", "items": [3 × str], "project"}] } }` kõigile 22 grupile.
- Produces: `build_catalog(out_dir: Path, templates: dict, hidden_codes: set[str]) -> list[dict]`. Iga element on `Profession` JSON:
  - `id`, `name`, `category` (SOC grupi nimi), `onetCode`, `alternateTitles`;
  - `tasks[]`: `{name, share, automation, observedUse}`;
  - `humanEdge`: 3 ülesannet suurima `share × (1 − automation)` järgi;
  - `pivotRoles`: 3 esimest `related_codes` nimetust;
  - `aiTools`, `roadmap`: mallist.

  Peidetud koodid jäetakse välja.
- [x] **Testid:**
  - `test_profession_shape_and_id`: `onet-15-1252-00`, kategooria, `onetCode`
  - `test_shares_sum_to_100_and_automation_rounded`: `automation` ümardatakse 2 kohani
  - `test_human_edge_is_top3_share_times_one_minus_automation`
  - `test_pivot_roles_are_related_titles_in_order`
  - `test_template_by_soc_major`
  - `test_hidden_codes_excluded`
  - `test_templates_cover_all_22_groups`: loeb päris `templates.json`-i
- [x] **Implementatsioon:** CLI `python tools/onet-import/catalog.py` loeb `hidden_codes` failist `professions.json` (`onetCodes`) ja kirjutab JSON-i (`ensure_ascii=False`, `indent=None`, read sorteeritud `name` järgi).

### Task 2: Käsitsi tehtud ametite `onetCodes` ühte kohta

**Files:**
- Modify: `backend/src/main/resources/ai-readiness/professions.json` (18 ametile `onetCodes`, `other`-ile mitte)
- Modify: `tools/onet-import/blend.py` (loeb vastavused `professions.json`-ist), `tools/onet-import/test_blend.py`
- Delete: `tools/onet-import/curated_onet_codes.json`

- [x] **Testid:** `test_blend.py` fikstuur kasutab `onetCodes` välja `professions.json`-is. Käivitus annab sama `calibration.md` kui enne (4,4).

### Task 3: Backendi mudelid ja kataloogi laadimine

**Files:**
- Modify: `CatalogModels.java`:
  - `ProfessionTask(String name, int share, double automation, boolean observedUse)`;
  - `Profession` lisaväljad `String onetCode`, `List<String> onetCodes`, `List<String> alternateTitles` (null lubatud).
- Modify: `AiReadinessCatalog.java`: laeb `professions-onet.json`, peidab koodid, `featured()` tagastab käsitsi tehtud ametid, valideerib unikaalse `onetCode`-i.
- Test: `backend/src/test/java/ee/kerrete/ainterview/aireadiness/AiReadinessCatalogTest.java`

**Interfaces:**
- Produces:
  - `List<Profession> featured()`: käsitsi tehtud, failis olevas järjekorras;
  - `List<Profession> professions()`: käsitsi tehtud + nähtavad O\*NET-i ametid;
  - `Optional<Profession> profession(String id)`: ka O\*NET-i ametid, peidetud mitte.
- [x] **Testid:**
  - `loadsCuratedAndOnetProfessions`: `featured().size() == 19`, `professions().size() > 900`
  - `hidesOnetCodesCoveredByCuratedProfessions`: `profession("onet-15-1252-00")` on tühi
  - `onetProfessionIsAvailable`: `profession("onet-29-1141-00")` puudub (healthcare katab selle), `profession("onet-35-2014-00")` on olemas
  - `everyProfessionValidates`: osakaalud 100, automation 0..1, humanEdge ja roadmap pole tühjad

### Task 4: Otsing ja ühilduv `/professions`

**Files:**
- Modify: `AiReadinessService.java`, `AiReadinessController.java`, `AssessmentModels.ProfessionSummary` (+ `boolean featured`)
- Test: `AiReadinessSearchTest.java`

**Interfaces:**
- Produces:
  - `List<ProfessionSummary> professions()`: ainult `featured()`;
  - `List<ProfessionSummary> search(String query, int limit)`.

  Järjestus astmete kaupa:
  1. nime täpne vaste;
  2. nimi algab päringuga;
  3. alternatiivnimetuse täpne vaste;
  4. mõni nime sõna algab päringuga;
  5. alternatiivnimetus algab päringuga;
  6. nimi sisaldab päringut;
  7. alternatiivnimetus sisaldab päringut.

  Viigi korral on `featured` ees, siis tähestik. Tühi päring tagastab `featured`. `limit` piiratakse vahemikku 1..50. Võrdlus on väiketähtedes, `Locale.ROOT`.
- Endpoint: `GET /api/ai-readiness/professions/search?q=&limit=20`
- [x] **Testid:**
  - `emptyQueryReturnsFeatured`
  - `softwareDeveloperFindsCuratedFirst`
  - `coderFindsSoftwareDevelopersViaAlternateTitle`, kus esimeste tulemuste seas on mõni `onet-15-*` või käsitsi tehtud `software-developer`
  - `caseInsensitive`
  - `limitIsClamped`
  - `professionsEndpointReturnsOnlyFeatured`

### Task 5: `observedUse` tulemusse

**Files:**
- Modify: `AssessmentModels.TaskExposure` (+ `boolean observedUse`), `AiReadinessScorer.java`, `frontend-risk/src/app/models/ai-readiness.model.ts` (valikuline väli)
- Test: `AiReadinessScorerTest.java`
- [x] **Testid:**
  - `onetProfessionScoresWithObservedUse`: `onet-43-4051-00` puudub (peidetud), seega kasutab test `onet-43-4171-00` (Receptionists). Vähemalt ühel ülesandel on `observedUse == true` ja skoor on 3..97.
  - Käsitsi tehtud ametil on kõik `observedUse == false`.

### Task 6 (eraldi kinnitusega): LLM-i projektilaused (O2)

`write_projects.py`: üks Batch API päring ameti kohta, `claude-opus-5`. Sisend on ameti nimi, 5 suurima osakaaluga ülesannet ja kolme faasi pealkirjad. Väljund on `projects_llm.csv` (`onetsoc_code, phase, project`, kuni 200 märki). `catalog.py` eelistab seda malli projektile. **Käivitub alles pärast uut API võtit ja hinnakinnitust.**
