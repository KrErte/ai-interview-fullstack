import { AssessmentResult, RiskBand } from '../../models/ai-readiness.model';

/** Task name -> share of the week (integers, total 100). */
export type TaskShares = Record<string, number>;

export function sharesOf(r: AssessmentResult): TaskShares {
  return Object.fromEntries(r.tasks.map(t => [t.name, t.share]));
}

/**
 * Sets one task's share and rescales the others in proportion so the week still adds up to 100.
 * Results are integers; the points lost to rounding go to the largest other share. When all
 * other shares are 0, the remainder is spread evenly over them.
 */
export function rescaleShares(shares: TaskShares, name: string, value: number): TaskShares {
  const names = Object.keys(shares);
  if (!(name in shares) || names.length < 2) {
    return { ...shares };
  }
  const own = Math.max(0, Math.min(100, Math.round(value)));
  const others = names.filter(n => n !== name);
  const rest = 100 - own;
  const othersTotal = others.reduce((sum, n) => sum + shares[n], 0);
  const result: TaskShares = { [name]: own };
  for (const n of others) {
    result[n] = othersTotal > 0
      ? Math.floor((shares[n] * rest) / othersTotal)
      : Math.floor(rest / others.length);
  }
  const leftover = rest - others.reduce((sum, n) => sum + result[n], 0);
  if (leftover > 0) {
    // the largest other share takes the rounding remainder (first one on a tie)
    const largest = others.reduce((best, n) => (result[n] > result[best] ? n : best), others[0]);
    result[largest] += leftover;
  }
  // keep the original key order, so the request and storage stay stable
  return Object.fromEntries(names.map(n => [n, result[n]]));
}

/** Same cut-offs as AiReadinessScorer.band(). */
export function bandFor(score: number): RiskBand {
  if (score < 30) {
    return 'LOW';
  }
  if (score < 50) {
    return 'MODERATE';
  }
  return score < 70 ? 'HIGH' : 'VERY_HIGH';
}

/**
 * Local preview of a result with new shares, using the server's formula: exposure =
 * Σ share × automation, plus the unchanged adjustment from the answers, clamped to 3..97.
 * Automation is only known here as a rounded percent, so the preview can differ from the
 * server by a point; the server's answer replaces it.
 */
export function previewResult(r: AssessmentResult, shares: TaskShares): AssessmentResult {
  const tasks = r.tasks.map(t => ({ ...t, share: shares[t.name] ?? t.share }));
  const exposure = Math.round(tasks.reduce((sum, t) => sum + (t.share * t.automationPercent) / 100, 0));
  const replaceability = Math.max(3, Math.min(97, exposure + r.personalAdjustment));
  return { ...r, tasks, taskExposureScore: exposure, replaceabilityScore: replaceability, band: bandFor(replaceability) };
}
