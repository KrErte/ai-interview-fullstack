# O\*NET import, etapp 1: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Korratav Pythoni skript, mis loob O\*NET 29.3 ja Anthropic Economic Indexi (AEI) põhjal ametite, ülesannete, alternatiivnimetuste ja seotud ametite CSV-d. Automatiseeritavust selles etapis ei ole.

**Architecture:** `tools/onet-import/` sisaldab:
- `fetch.py`: laeb toorandmed lukustatud versioonidega kausta `.cache/`;
- `build.py`: puhtad funktsioonid, mis loevad toorandmed ja kirjutavad sorteeritud CSV-d kausta `tools/onet-import/out/` (commititakse);
- `test_build.py`: väikeste fikstuuridega unittest, võrguta.

Rakenduse koodi see etapp ei muuda.

**Tech Stack:** Python 3 (ainult standardteek: `csv`, `zipfile`, `urllib`, `unittest`).

**Spec:** `docs/superpowers/specs/2026-10-13-onet-professions-design.md` (etapp 1, punktid 3.1, 3.2, 4.1, 4.2)

## Global Constraints

- O\*NET versioon **29.3**: `https://www.onetcenter.org/dl_files/database/db_29_3_text.zip`
- AEI lukustatud commit **`4e5f69fab13a9c3f7d0dcc31a6f893a9818738f6`**. Failid: `robot_exposure/robot_exposure_tasks.csv` (`time_share`, `physical`) ja `labor_market_impacts/task_penetration.csv` (`penetration`).
- `share_pct` on täisarv ja summa ameti kohta on **täpselt 100** (suurima jäägi meetod). Seda nõuab `AiReadinessCatalog.validate()` etapis 3.
- Kõik failid on UTF-8. CSV kirjutatakse `newline=''`-ga, et Windows ei lisaks CRLF-e. Read on sorteeritud stabiilse diffi jaoks.
- Toorandmed (`.cache/`) ei lähe gitti, väljund (`out/`) läheb.

**Kõrvalekalle spetsist:** spetsis on üks fail `onet_tasks.csv`. Väljund jaguneb kolmeks failiks, sest alternatiivnimetusi on 56 560 ja seotud ameteid kuni 20 ameti kohta. Ülesannete faili jääb ainult ülesannete info.

## Review Focus

1. **Ülesande tekst erineb AEI-s ja O\*NET-is tühikute poolest**, siis `penetration` ei seostu ja jääb vaikselt tühjaks. Võrdlus tehakse pärast `strip()`-i ja seostamata ridade arv trükitakse (oodatult umbes 96 / 18 796).
2. **Väga väikese `time_share`-ga ülesanne saab `share_pct = 0`**, aga summa peab ikka olema 100. Test katab selle.
3. **AEI-s on amet, mida O\*NET 29.3-s pole, või vastupidi.** Skript peatub vea ja koodiga, mitte ei jäta vaikselt vahele.
4. **Kodeering:** mitte-ASCII märgid (nt `é`, `–`) peavad väljundis säilima. Test katab selle.
5. **Korduv käivitus annab baidi pealt sama väljundi** (determinism). Test katab selle.

---

### Task 1: Puhtad abifunktsioonid

**Files:**
- Create: `tools/onet-import/build.py`
- Test: `tools/onet-import/test_build.py`

**Interfaces:**
- Produces:
  - `largest_remainder(shares: list[float], total: int = 100) -> list[int]`
  - `soc_major(code: str) -> tuple[str, str]`, nt `'15-1252.00'` → `('15', 'Computer and Mathematical')`. Kõik 22 SOC 2018 peagruppi on konstandis `SOC_MAJOR_GROUPS`.

- [x] **Step 1: Testid**
  - `largest_remainder([0.5, 0.3, 0.2]) == [50, 30, 20]`
  - `largest_remainder([1/3]*3)`: summa on 100 ja `sorted(...) == [33, 33, 34]`
  - `largest_remainder([0.996, 0.004])`: summa on 100
  - `largest_remainder([0.999, 0.001])`: summa on 100 ja viimane on 0
  - `soc_major('15-1252.00') == ('15', 'Computer and Mathematical')`
  - `soc_major('29-1141.00')[1] == 'Healthcare Practitioners and Technical'`
  - tundmatu grupp `'99-…'` annab `ValueError`
- [x] **Step 2:** `python -m unittest tools/onet-import/test_build.py`. Oodatult FAIL.
- [x] **Step 3:** Kirjuta funktsioonid. Suurima jäägi meetodis jagatakse jäägid kahanevas järjekorras, viigi korral otsustab väiksem indeks (determinism).
- [x] **Step 4:** Testid on PASS.

### Task 2: `build()`: toorandmed → kolm CSV-d

**Files:**
- Modify: `tools/onet-import/build.py`
- Test: `tools/onet-import/test_build.py` (fikstuurid kirjutatakse testis `tempfile`-i: mini-zip ja mini-CSV-d)

**Interfaces:**
- Consumes: Task 1 funktsioonid.
- Produces: `build(onet_zip: Path, aei_tasks_csv: Path, aei_penetration_csv: Path, out_dir: Path) -> dict`. Tagastab statistika `{"occupations": int, "tasks": int, "penetration_unmatched": int}` ja kirjutab:
  - `onet_occupations.csv`: `onetsoc_code,title,soc_major,soc_major_title,task_count,related_codes`. `related_codes` on Primary-Short koodid `;`-ga eraldatuna, `Index` järjekorras. Ainult ametid, millel on ülesanded.
  - `onet_tasks.csv`: `onetsoc_code,task_id,task_statement,task_type,time_share,share_pct,physical,penetration`. `penetration` on tühi, kui teksti vastet ei leitud.
  - `onet_alternate_titles.csv`: `onetsoc_code,alternate_title,short_title`. `short_title` jääb tühjaks, kui allikas on `n/a`.
- [x] **Step 1: Testid** (fikstuur: 2 ametit, millest ühel 3 ülesannet, teisel 2, ja üks amet ilma ülesanneteta)
  - `test_build_writes_three_files_sorted`: failid on olemas ja read on sorteeritud `onetsoc_code`, seejärel `task_id` järgi
  - `test_shares_sum_to_100_per_occupation`
  - `test_occupation_without_tasks_is_excluded`
  - `test_related_codes_primary_short_in_index_order`
  - `test_penetration_matched_after_strip`: AEI tekstil on lõputühik ja vaste leitakse; teisel ülesandel vastet pole ja väli on tühi; `stats['penetration_unmatched'] == 1`
  - `test_task_in_aei_missing_from_onet_raises`: `ValueError`, sõnumis on `task_id`
  - `test_non_ascii_preserved`: `'Café – menu'` jõuab väljundisse muutmata
  - `test_deterministic`: kaks käivitust annavad baidi pealt samad failid
- [x] **Step 2:** Testid on FAIL.
- [x] **Step 3:** Kirjuta `build()`. Ülesannete hulga alus on O\*NET *Task Statements*; `time_share` ja `physical` tulevad AEI-st `task_id` järgi. Ameti nimi ja kirjeldus tulevad *Occupation Data*-st.
- [x] **Step 4:** Testid on PASS.

### Task 3: `fetch.py`, CLI ja päris käivitus

**Files:**
- Create: `tools/onet-import/fetch.py`, `tools/onet-import/README.md`, `tools/onet-import/out/*.csv` (genereeritud)
- Modify: `.gitignore` (lisa `tools/onet-import/.cache/`)

**Interfaces:**
- Consumes: `build()`.
- Produces:
  - `python tools/onet-import/fetch.py` laeb puuduvad failid kausta `.cache/` (olemasolevaid uuesti ei lae);
  - `python tools/onet-import/build.py` käivitab `build()` vaikimisi teedega ja trükib statistika.

- [x] **Step 1:** Kirjuta `fetch.py`. URL-id on Global Constraints'is, AEI kujul `https://huggingface.co/datasets/Anthropic/EconomicIndex/resolve/<sha>/<path>`.
- [x] **Step 2:** Lisa `build.py`-sse `if __name__ == "__main__":`. Käivita mõlemad. **Oodatav:** `occupations: 923`, `tasks: 18796`, `penetration_unmatched` umbes 96.
- [x] **Step 3: Sisukontroll väljundist**
  - `15-1252.00` (Software Developers) on olemas;
  - iga ameti `share_pct` summa on 100;
  - `related_codes` ei ole tühi vähemalt 90% ametitel.
- [x] **Step 4:** Kirjuta README: mis, kust, litsentsid (O\*NET CC BY 4.0, AEI CC BY), kuidas käivitada ja väljundfailide veerud.
- [x] **Step 5:** Kõik testid on PASS. Commit (spec, plaan, skript, testid, `out/`, `.gitignore`).
