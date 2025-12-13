# Interview Intelligence — UX Microcopy (v1)

---

## 1. State Microcopy

### A) Before 3 Answers (No Fit Yet)

| Element | Estonian | English fallback |
|---------|----------|------------------|
| **Label** | Tutvume sinuga | Getting to know you |
| **Tooltip** | Sobivuse arvutamiseks vajame veel mõnda vastust. | We need a few more answers to calculate fit. |
| **Icon** | ◐ (half-circle) or subtle pulse dot | |

---

### B) Fit Available (Percentage Shown)

| Element | Estonian | English fallback |
|---------|----------|------------------|
| **Label** | Sobivus: XX% | Fit: XX% |
| **Tooltip** | See hinnang põhineb sinu senistel vastustel. | This score is based on your answers so far. |
| **Icon** | ● (solid circle) or small gauge icon | |

---

### C) Fit Improving

| Element | Estonian | English fallback |
|---------|----------|------------------|
| **Label** | Tõusuteel ↑ | Improving ↑ |
| **Tooltip** | Sinu viimased vastused on skoori tõstnud. | Your recent answers have raised your score. |
| **Icon** | ↗ or subtle upward chevron | |

---

### D) Fit Flat

| Element | Estonian | English fallback |
|---------|----------|------------------|
| **Label** | Stabiilne → | Steady → |
| **Tooltip** | Sinu skoor on püsinud ühtlasel tasemel. | Your score has remained consistent. |
| **Icon** | → or horizontal line | |

---

### E) Fit Declining

| Element | Estonian | English fallback |
|---------|----------|------------------|
| **Label** | Vajab tähelepanu | Needs attention |
| **Tooltip** | Proovi järgmistes vastustes olla konkreetsem. | Try to be more specific in your next answers. |
| **Icon** | ◑ (half-filled) — avoid red or downward arrows | |

---

## 2. "No Data Yet" Variants (3 Options)

These replace the fit percentage before 3 questions are answered.

| # | Estonian | English | Intent |
|---|----------|---------|--------|
| **1** | Vastused kogunemas… | Answers collecting… | Progress-oriented |
| **2** | Veel õpime sind tundma | Still getting to know you | Warm, human |
| **3** | Hindamine algab peagi | Assessment starts soon | Anticipatory, neutral |

**Usage note:** All three avoid percentages, error language, and judgment. Variant 2 is warmest; Variant 1 is most neutral; Variant 3 sets expectation.

---

## 3. Confidence Badge Concept

### Name Alternatives

| Estonian | English | Tone |
|----------|---------|------|
| Usaldustase | Confidence Level | Formal |
| Täpsusaste | Precision Level | Technical |
| Hinnangu kindlus | Score Certainty | Descriptive |
| Andmesügavus | Data Depth | Neutral-modern |
| Profiili tugevus | Profile Strength | Candidate-friendly ✓ |

**Recommended:** *Profiili tugevus* (Profile Strength) — frames the badge as the candidate's progress, not system judgment.

---

### Rules (Based on Answered Questions)

| Answered Questions | Badge Level | Visual |
|--------------------|-------------|--------|
| 0–2 | — (no badge shown) | — |
| 3–4 | Esialgne (Initial) | ◔ 25% ring |
| 5–6 | Arenev (Developing) | ◑ 50% ring |
| 7–9 | Tugev (Strong) | ◕ 75% ring |
| 10 | Täielik (Complete) | ● full ring |

---

### User-Facing Explanation (1 sentence)

**Estonian:**  
> Mida rohkem küsimustele vastad, seda täpsem on sinu sobivuse hinnang.

**English:**  
> The more questions you answer, the more accurate your fit score becomes.

---

## 4. Optional: UI Animation When Fit % First Appears

**Concept:** *"Soft Reveal"*

When the user answers question 3 and fit % becomes available:

1. The placeholder text ("Vastused kogunemas…") fades out (200ms ease-out)
2. A subtle radial wipe reveals the percentage from center outward
3. The number counts up from 0 to actual value (400ms, ease-in-out)
4. A single soft pulse on the badge border confirms the update

**Why this works:**
- Feels earned, not abrupt
- Draws attention without alarm
- Reinforces that the system is "waking up" with enough data

---

## 5. Quick Reference Card

| State | Label (EE) | Icon |
|-------|------------|------|
| No data | Tutvume sinuga | ◐ |
| Fit shown | Sobivus: XX% | ● |
| Improving | Tõusuteel ↑ | ↗ |
| Flat | Stabiilne → | → |
| Declining | Vajab tähelepanu | ◑ |

---

*Ready for implementation. All copy fits within standard badge constraints (desktop ≤120px, mobile ≤90px).*
