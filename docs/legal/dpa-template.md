# Data Processing Agreement – team AI report (template)

> **Template, not legal advice.** This is a starting point under Article 28 of the GDPR for the careerrisk.ee team AI report. Have it reviewed before you sign it with a customer. Text in `[brackets]` must be filled in or confirmed.

**Between**

- **Controller:** `[Customer legal name]`, registry code `[...]`, address `[...]` ("the Customer").
- **Processor:** Kristo Erte, trading as careerrisk.ee, `[address]`, contact m6rnaja@gmail.com ("the Processor").

This agreement is part of the order for the team AI report dated `[date]` (the "Service"). If this agreement and the order conflict, this agreement prevails for anything about personal data.

## 1. Subject matter and duration

1.1 The Processor processes personal data on behalf of the Customer only to provide the Service. The Service is an anonymous team report built from the AI exposure and readiness test that the Customer's staff take through the Customer's invite link.

1.2 This agreement applies for as long as the Processor holds personal data for the Customer's team. It ends when that data has been deleted under section 9.

## 2. Nature and purpose of processing

2.1 **Purpose.**
- To store each answer given through the Customer's invite link.
- To calculate a report for the Customer that contains aggregate figures only.
- To let the Customer delete the team and all answers.

2.2 **Operations:**
- collection through the test;
- storage in the Processor's database;
- aggregation into the report;
- deletion.

2.3 **Rules built into the Service.** The Processor does not use the data for any other purpose, including profiling individual staff, marketing or training machine learning models. The Service has these rules built in:
- the report opens only when at least 5 people have answered;
- groups of professions and tasks with fewer than 5 people are merged or left out;
- the report never shows or returns a single person's answers;
- the report does not rank people.

## 3. Types of personal data and data subjects

3.1 **Data subjects:** the Customer's staff and other people the Customer invites to take the test.

3.2 **Data stored for each answer:**
- the profession chosen;
- the answers to the test questions;
- the resulting scores;
- the task shares;
- the date of the answer.

3.3 **Data not stored:** names, email addresses, IP addresses or browser identifiers. The data is pseudonymous. It may still be personal data, for example in a very small team or together with other information the Customer holds.

3.4 **Special categories.** No special categories of personal data (GDPR Article 9) are requested.

## 4. Instructions

4.1 The Processor processes the personal data only on the Customer's documented instructions. These are this agreement, the order and the Customer's use of the Service, including deletion from the report page.

4.2 The Processor tells the Customer without delay if it believes an instruction breaks the GDPR or other data protection law.

## 5. Confidentiality

5.1 Only the Processor and any person who works for it under a written duty of confidentiality may access the personal data, and only as far as needed to provide the Service.

## 6. Security (GDPR Article 32)

6.1 The Processor applies the technical and organisational measures in Annex 1 and keeps them appropriate to the risk.

## 7. Sub-processors

7.1 The Customer gives general authorisation for the sub-processors in Annex 2.

7.2 The Processor tells the Customer at least 30 days before adding or replacing a sub-processor. The Customer may object on reasonable grounds. If no solution is found, the Customer may end the Service and have its data deleted.

7.3 The Processor puts data protection obligations on each sub-processor that are no less protective than this agreement, and stays responsible for them.

## 8. Assistance to the Customer

8.1 **Data subject requests.** Answers carry no identity. The Processor helps the Customer, as far as possible, with requests from data subjects (GDPR Articles 15–22), for example by deleting the whole team on request.

8.2 **Other obligations.** The Processor helps the Customer, as far as the information available to it allows, with:
- security of processing;
- breach notification;
- data protection impact assessments;
- prior consultation (GDPR Articles 32–36).

8.3 **Personal data breaches.** The Processor notifies the Customer without undue delay, and in any case within `[48]` hours, after becoming aware of a breach affecting the Customer's data. The notice includes the information required by GDPR Article 33(3), as far as it is known.

## 9. Deletion and return

9.1 The Customer can delete the team and every answer at any time from the report page. Deletion removes the data from the live database immediately. Backups are kept for 14 days and are then overwritten.

9.2 At the end of the Service, the Processor deletes all of the Customer's personal data within `[30]` days unless the Customer has already deleted it. A copy of the aggregate report can be printed or saved as PDF by the Customer before deletion; the Processor does not keep a copy.

9.3 Data needed for invoicing (the team name and the contact email of the person who ordered the Service) is processed by the Processor as a controller. It is kept as long as accounting law requires.

## 10. Audits and information

10.1 The Processor makes available the information needed to show compliance with this agreement and GDPR Article 28.

10.2 The Processor allows audits by the Customer or an auditor the Customer chooses. Audits need reasonable notice of at least `[30]` days, are limited to once a year unless there has been a breach, and are at the Customer's cost.

## 11. International transfers

11.1 The Processor and the sub-processors in Annex 2 process the data within the European Economic Area. No team data is sent to the language model provider used for the optional personal AI comment. That comment is a separate feature for individuals and is not part of the team report.

## 12. Liability and law

12.1 Liability is as set out in the order and GDPR Article 82.

12.2 This agreement is governed by the laws of Estonia. Disputes are settled by the courts of Estonia, `[Harju County Court]`.

---

## Annex 1 – Technical and organisational measures

**Data minimisation**
- Only test content and the date are stored for each answer.
- No names, emails, IP addresses or browser identifiers are stored.
- The report key is stored only as a SHA-256 hash.

**Aggregation thresholds**
- The report opens only from 5 answers.
- Groups and tasks under 5 people are merged or left out.
- The breakdown is dropped where a small group could be derived by subtraction.
- No API returns single answers.

**Access control**
- The report needs the secret report link (32 random bytes). The link is kept in the URL fragment, so it does not reach server logs or analytics.
- Only the Processor has server access, with key-based logins only.

**Transport and storage**
- HTTPS only.
- The database cannot be reached from the internet.
- Server logs are size-limited and rotated.

**Backups**
- Nightly database backups are kept for 14 days.

**Deletion**
- The Customer can delete the team and all answers from the report page; this takes effect immediately in the live database.

**Testing**
- Automated tests cover the thresholds, the absence of single answers in the report and deletion.

## Annex 2 – Sub-processors

| Sub-processor | Location | Service |
|---|---|---|
| Contabo GmbH | Germany | Server hosting (virtual private server) |
| `[add any others before signing]` | | |
