# AI asendatavuse test: testimise kokkuvõte

**Kuupäev:** 2026-10-13
**Versioon:** `main` @ `8254629` (pärast PR #37), lokaalne Docker (`docker compose up`)
**Ulatus:** kõik 918 ametit, 1 386 vastusekombinatsiooni, üksikküsimuste mõju, valideerimine, otsing ja jõudlus API kaudu, lisaks brauserikontroll (PR #37)
**Testiskript:** süstemaatiline API test (`api_test.py`, kokku umbes 3 300 päringut, 31 s)

## Kokkuvõte

| Valdkond | Tulemus |
|---|---|
| Kõik ametid (918) hinnatavad | ✅ 918 / 918, 0 viga |
| Skoori loogika (vahemik, riskiastmed, valem) | ✅ 0 viga 2 304 tulemuses |
| Vastusekombinatsioonid (63 ametit × 22) | ✅ 0 viga |
| Iga küsimuse mõju suund | ✅ kõik ootuspärased |
| Determinism | ✅ sama sisend annab sama tulemuse |
| Valideerimine (vigased päringud) | ✅ kõik annavad 400 |
| Jõudlus | ✅ mediaan 5,5 ms, p95 27 ms |
| Otsing: käsitsi tehtud ametite taha peidetud O\*NET-i nimed | ✅ parandatud (vt punkt 6) |
| Otsingu järjestus mõne päringu puhul | ⚠️ kosmeetiline |

## 1. Kõik ametid ilma vastusteta

Iga 918 ameti kohta kontrolliti:
- skoor on 3..97;
- riskiaste vastab lävedele 30 / 50 / 70;
- `skoor = ülesannete eksponeeritus + isiklik korrektsioon`;
- ülesannete osakaalud summeeruvad 100-ks;
- eksponeeritus on arvutatav ülesannetest;
- ülesanded on automatiseeritavuse järjekorras;
- õppetee etapid on õiges järjekorras ja nädalate summa klapib;
- inimlik eelis, tööriistad, seotud rollid ja 3-faasiline plaan on olemas;
- tekstides pole tühje välju ega katkiseid märke.

**Tulemus: 0 viga.**

| Näitaja | Väärtus |
|---|---|
| Riskiastmed (ilma vastusteta) | LOW 414 · MODERATE 351 · HIGH 151 · VERY HIGH 2 |
| Ülesannete eksponeeritus | min 7 · mediaan 33 · max 74 |
| Ülesandeid ameti kohta | 4–40 |
| Ametid, kus vähemalt üht ülesannet tehakse juba AI-ga | 460 / 918 |
| Madalaimad | Dishwashers 7, Helpers–Roofers 8, Maids and Housekeeping Cleaners 8 |
| Kõrgeimad | Correspondence Clerks 74, Translator 70, Telephone Operators 69, Copywriter 69 |

> **Tähelepanek (toode, mitte viga):** ilma vastusteta jõuab „Very high“ astmesse ainult 2 ametit, sest ülesannete tasemel on meie hinnang konservatiivne. Isiklikud vastused nihutavad skoori kuni ±30 punkti, nii et „Very high“ tuleb sagedamini välja vastustega. ReplacedByAI näitab 230 „critical“ ametit, sest nende aluseks on 2017. aasta Frey & Osborne metoodika.

## 2. Vastusekombinatsioonid

**Valim:** 19 käsitsi tehtud ametit ja 2 juhuslikku O\*NET-i ametit igast 22 valdkonnast, kokku 63 ametit. Iga ameti kohta tehti:
- **madalaima riskiga persona:** rutiin alla 20%, inimkontakt on töö tuum, füüsiline töö, vastutus, üle 10 aasta kogemust, AI igapäevane kasutaja jne;
- **kõrgeima riskiga persona:** vastupidi;
- **20 juhuslikku vastusekomplekti.**

Kokku 1 386 päringut, **0 viga**.

| Kontroll | Tulemus |
|---|---|
| Madala riski persona korrektsioon | alati −30 (piir), oodatud |
| Kõrge riski persona korrektsioon | alati +30 (piir), oodatud |
| AI valmisolek, madal / kõrge persona | 90 / 10, oodatud |
| Skoori vahe kahe persona vahel | 35–60 punkti (mediaan 60) |

Vahe on alla 60 ainult ametitel, kus skoor jõuab alampiirini 3 või ülempiirini 97.

## 3. Iga küsimuse mõju (amet: Accountant, muud vastused neutraalsed)

| Küsimus | Mõju skoorile | Mõju valmisolekule | Hinnang |
|---|---|---|---|
| Rutiinse töö osakaal | 61 → 79 (rohkem rutiini, kõrgem risk) | – | ✅ |
| Inimkontakt | 71 → 57 | – | ✅ |
| Füüsiline töö | 73 → 60 | – | ✅ |
| Otsustamine ja vastutus | 73 → 59 | – | ✅ |
| Kogemus | 71 → 61 | – | ✅ |
| Ainulaadne valdkonnateadmine | 71 → 61 | – | ✅ |
| AI kasutamise sagedus | 70 → 61 | 18 → 58 | ✅ |
| AI oskuste sügavus | 69 → 62 | 18 → 53 | ✅ |
| Uute tööriistade omaksvõtt | 70 → 64 | 20 → 35 | ✅ |
| Tööandja kasutab AI-d | 65 → 71 | 28 → 38 | ✅ teadlik valik: kui tööandja võtab AI kasutusele, kasvab risk |
| Õppimiseks aega nädalas | – | – | ✅ õppetee 98 → 34 → 21 → 13 nädalat |

Tundide valikud kasutavad vahemiku konservatiivset väärtust: „3–6 h“ arvestatakse 5 tunnina, „üle 6 h“ 8 tunnina. See on mõistlik.

## 4. Determinism ja osalised vastused

- Sama päring kaks korda annab identse tulemuse. ✅
- API aktsepteerib osalisi vastuseid (puuduv vastus = neutraalne). Frontend nõuab kõiki 11 vastust, nii et kasutajale see ei paista. ✅

## 5. Valideerimine

| Vigane päring | Vastus |
|---|---|
| Tundmatu amet | 400 `Unknown profession 'nope'` ✅ |
| Peidetud O\*NET-i duplikaat (`onet-15-1252-00`) | 400 ✅ |
| Tundmatu vastusevariant | 400 `Unknown option 'nope' for question 'routine'` ✅ |
| Tundmatu küsimus | 400 `Unknown question 'nope'` ✅ |
| Tühi `professionId`, puuduvad `answers`, tühi keha | 400 ✅ |

> ⚠️ **Väike ebaühtlus:** viimased kolm tagastavad Springi vaikimisi veakeha (`timestamp`, `status`, `path`), mitte rakenduse `{"error": "..."}` kuju. Kasutajale see ei paista, sest frontend saadab alati korrektse keha.

**AI kommentaar (`personalize: true`) ilma võtmeta:** vastus on 200 ja kommentaari pole, ülejäänud tulemus on olemas, nagu disainitud. ✅

## 6. Otsing

| Päring | Esimesed tulemused | Hinnang |
|---|---|---|
| barista | Baristas | ✅ |
| cashier | Cashiers | ✅ |
| hairdresser | Hairdressers, Hairstylists, and Cosmetologists | ✅ |
| plumber | Plumbers, Pipefitters, and Steamfitters | ✅ |
| pilot | Airline Pilots…, Commercial Pilots | ✅ |
| truck driver | Freight Forwarders, **Heavy and Tractor-Trailer Truck Drivers** | ⚠️ õige amet teisel kohal |
| lawyer, accountant, translator, software developer | käsitsi tehtud amet esimesena | ✅ |
| nurse / NURSE | sama tulemus (tõstutundetu) | ✅ |
| xyzzy | tühi, frontend pakub „Other“ | ✅ |
| tühi või tühikud | populaarsed ametid | ✅ |
| limit 0 / 500 | piiratakse 1 / 50 | ✅ |
| **registered nurse** | Health Education Specialists, Nurse Midwives… | ❌ |
| **software engineer** | Computer and Information Systems Managers… | ❌ |
| **data scientist** | Biofuels Processing Technicians | ❌ |
| **family doctor** | (tühi) | ❌ |
| coder | Medical Records Specialists, Computer Programmers | ⚠️ O\*NET-is on „Coder“ meditsiinikodeerija nimetus |

> ✅ **Parandatud** (haru `fix/search-hidden-titles`). Käsitsi tehtud ametid saavad nüüd otsingunimedeks nende O\*NET-i ametite nimed, mida need peidavad (`professions-aliases.json`). Mitmesõnaline päring leiab ameti ka siis, kui sõnad on nimes teises järjekorras. Kordustest:
>
> | Päring | Esimene tulemus |
> |---|---|
> | registered nurse | Healthcare worker (doctor, nurse) |
> | software engineer | Software developer |
> | data scientist | Data analyst |
> | family doctor | Healthcare worker (doctor, nurse) |
> | doctor | Healthcare worker (doctor, nurse) (enne: Allergists and Immunologists) |
>
> Kogu API test korrati üle: 918 ametit ja 1 386 kombinatsiooni, 0 viga.

### ❌ Viga: peidetud O\*NET-i ametite nimed ei ole otsitavad

**Põhjus:** 24 O\*NET-i ametit on peidetud, sest käsitsi tehtud amet katab need (nt *Registered Nurses* → „Healthcare worker“, *Data Scientists* → „Data analyst“, *Software Developers* → „Software developer“). Peitmisega kaovad ka nende nimed ja alternatiivnimetused (nt „Software Engineer“, „RN“). Käsitsi tehtud ametitel endal alternatiivnimetusi pole.

**Mõju:** sagedased päringud nagu „registered nurse“, „software engineer“, „data scientist“ ja „family doctor“ ei leia õiget ametit. Kasutaja valib vale ameti või „Other“.

**Parandus (väike):** `catalog.py` kirjutab iga käsitsi tehtud ameti juurde selle `onetCodes` ametite nimed ja alternatiivnimetused, ning backend lisab need käsitsi tehtud ameti `alternateTitles` hulka. Lisaks tuleb test „registered nurse“, „software engineer“, „data scientist“ ja „family doctor“ päringutele.

### ⚠️ Järjestus

Täpne alternatiivnimetuse vaste on praegu ees sellest, kui mõni nime sõna algab päringuga. Seetõttu tuleb nt „doctor“ peale esimesena *Allergists and Immunologists*, mitte „Healthcare worker (doctor, nurse)“. Võib kaaluda, et käsitsi tehtud ametid oleksid sama astme sees alati ees. See juba nii on, aga need jäävad madalamale astmele.

## 7. Jõudlus (lokaalne Docker)

| Päring | Mediaan | p95 | Max |
|---|---|---|---|
| `POST /assess` (918 ametit) | 5,5 ms | 26,7 ms | 326 ms (esimene päring, JVM soojenemine) |
| `GET /professions/search` | umbes 15–35 ms | | |

Backendi käivitus võtab 918 ametiga umbes 16 s (enne 13 s).

## 8. Brauserikontroll (PR #37)

1440 px ja 390 px laiusel kontrolliti:
- otsing, ka kiire trükkimisega;
- valdkonnafilter ja „Showing 50 of 107“;
- tühi tulemus koos „Other“ nupuga;
- terve test O\*NET-i ametiga (Receptionists);
- 6 ülesannet ja „+12 more tasks“, laiendamine;
- märgised „AI already used“ ja allikaviited;
- „Take the test again“;
- horisontaalset ülevoolu pole.

## Soovitused tähtsuse järjekorras

1. ~~Paranda peidetud ametite otsing (punkt 6).~~ Tehtud.
2. Kaalu, kas ainult ülesannete järgi tulev skooride jaotus on toote jaoks sobiv: 2 / 918 on „Very high“ ilma vastusteta.
3. Ühtlusta 400-vigade keha `{"error": ...}` kujule.
4. Uuendustel ära taaskäivita backendi koos frontendiga, et vältida umbes 16 s katkestust („Could not load the test“).
