# AI Interview Mentor – Workflow Guide

## Overview
Selles projektis töötab AI koos sinuga nii:
- Claude → suur pilt, ideed, arhitektuur
- GPT 5.1 → keerulised tehnilised otsused, süsteemid, refaktoring
- Cursor 4 agenti → päris failimuudatused, debug, build-fix

---

## 1. Architect phase (Claude → GPT)
Kasuta Claude’i:
- uute funktsioonide ideed
- UI/UX mõtlemine
- arhitektuuri suur pilt
- kuidas süsteemi laiendada

Kui vajad täpsemat või range loogikaga disaini → kasuta GPT 5.1.

---

## 2. Implement phase (Cursor – Backend, Frontend)
Kasuta Cursorit päris tööks:
- kõik failid
- kõik build fixid
- kõik commitid
- kõik Angular ja Java muudatused

Töö järjekord:
1. Architect agent → lugegu featuur läbi  
2. Backend agent → teeb backend faili muudatused  
3. Frontend agent → teeb frontend muudatused  
4. QA agent → kontrollib et kõik töötab

---

## 3. Debug phase
Korra:
- Cursor QA agent (või GPT kui error segane)
- Copy alati kogu error ja anna Cursor QA agentile

---

## 4. Local run
Backend:
