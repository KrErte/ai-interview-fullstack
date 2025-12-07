// src/app/candidate-job-matcher/mock-samples.ts

export interface SamplePair {
  name: string;
  email: string;
  cvText: string;
  jobDescription: string;
}

const NAMES = [
  'Kristo Erte',
  'Maria Kask',
  'Tarmo Saar',
  'Liis Mägi',
  'Juhan Tamm'
];

const EMAILS = [
  'kristo.erte@gmail.com',
  'maria.kask@example.com',
  'tarmo.saar@example.com',
  'liis.magi@example.com',
  'juhan.tamm@example.com'
];

const TECH_STACKS = [
  'Java, Spring Boot, PostgreSQL, Docker, Kubernetes',
  'Node.js, NestJS, TypeScript, MongoDB, AWS',
  'C#, .NET, SQL Server, Azure',
  'Python, Django, FastAPI, PostgreSQL',
  'React, Angular, TypeScript, Tailwind CSS'
];

const JOB_TITLES = [
  'Java / Spring Boot back-end arendaja',
  'Full-Stack arendaja (React + Node.js)',
  'Senior .NET arendaja',
  'Python back-end arendaja',
  'Front-end arendaja (React / Angular)'
];

const COMPANIES = [
  'SRINI',
  'Nortal',
  'Pipedrive',
  'Bolt',
  'Wise'
];

const CITY = [
  'Tallinn',
  'Tartu',
  'Pärnu',
  'Remote',
];

function randomOf<T>(arr: T[]): T {
  return arr[Math.floor(Math.random() * arr.length)];
}

export function generateRandomSample(): SamplePair {
  const name = randomOf(NAMES);
  const email = randomOf(EMAILS);
  const stack = randomOf(TECH_STACKS);
  const title = randomOf(JOB_TITLES);
  const company = randomOf(COMPANIES);
  const city = randomOf(CITY);

  const cvText = `
${name} – tarkvaraarendaja
E-post: ${email}

Lühitutvustus:
Kogenud tarkvaraarendaja, kellel on tugev taust tehnoloogiatel: ${stack}.
Töötanud nii väikeettevõtetes kui ka suuremates rahvusvahelistes tiimides.
Harjunud töötama agiilses meeskonnas, kus fookuses on kvaliteetne kood,
testid ja automatiseeritud CI/CD.

Töökogemus:
- ${company} (2022 – praegu) – Full-Stack arendaja, peamised tehnoloogiad: ${stack}.
  Vastutasin uute funktsionaalsuste arendamise, koodireviewde ja
  tootmiskeskkonna stabiilsuse eest.

Oskused:
- ${stack}
- REST API-de disain ja integreerimine
- CI/CD, Docker, GitLab CI
- Mikroteenuste arhitektuur, clean code, SOLID
`.trim();

  const jobDescription = `
Otsime oma ${city} kontorisse kogenud ${title} positsioonile, et liituda ${company} arendustiimiga.

Sinu peamised ülesanded:
- Arendada ja hooldada ärikriitilisi infosüsteeme ${stack} tehnoloogiatega
- Osaleda arhitektuuri- ja disainiaruteludes
- Tagada koodi kvaliteet, testitavus ja jälgitavus
- Teha koostööd nii tooteomanike kui ka teiste arendustiimidega

Nõudmised kandidaadile:
- Mitmeaastane kogemus ${stack} tehnoloogiatega
- Hea arusaam andmebaasidest ja REST API-dest
- Varasem kogemus agiilsete arendusmetoodikatega
- Soov panustada meeskonna töökultuuri ja teadmiste jagamisse

Pakume:
- Võimalust töötada mõjukate projektide kallal
- Paindlikku töökorraldust ja kaasaegset tehnoloogiaparki
- Tugevat tiimi ja arenguvõimalusi, sh koolitused ja konverentsid
`.trim();

  return {
    name,
    email,
    cvText,
    jobDescription
  };
}
