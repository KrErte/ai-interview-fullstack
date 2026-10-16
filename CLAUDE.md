# careerrisk.ee (endine nimi Futureproof) – projekti kontekst

- Toode: AI asendatavuse test + isiklikud AI õppimise roadmapid (eesti keel esmane).
- Backend: Java 17, Spring Boot 3.3, Spring Security + JWT, JPA, Liquibase, PostgreSQL (H2 lokaalselt), Gradle – kaust `backend/`.
- Frontend: Angular 19 standalone + Tailwind – kaust `frontend-risk/` (vana `frontend/` ära puutu).
- Ametite/küsimuste/roadmapide sisu: `backend/src/main/resources/ai-readiness/*.json`.
- Käivitus: `docker compose up -d --build` → http://localhost:4200/ai-test
- Testid: `cd backend && ./gradlew test`, frontend build: `cd frontend-risk && npm run build`.

## Meeskonnareeglid (botid)
- Väikesed, ülevaadatavad muudatused. Üks ülesanne = üks fookusega diff.
- Ära leiuta faile, API-sid ega tulemusi. Kui pole koodi näinud, loe see enne läbi.
- DB muudatused ainult uute Liquibase changeset'idega. Saladused ainult env muutujatest.
- Iga agendi vastus lõpeb plokiga:
  STATUS: DONE | BLOCKED | NEEDS_REVIEW
  SUMMARY: <1–3 lauset>
  HANDOFF: <kes järgmisena ja mida vajab>
