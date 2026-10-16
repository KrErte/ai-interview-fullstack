/**
 * Shareable result link without an account. Everything needed to recalculate the result lives in the
 * URL fragment (/ai-test#share=...): the profession id, the option id of each answer and, if the person
 * set them, their task shares. Browsers never send the fragment to the server, so the answers do not
 * end up in server logs or analytics, and the server stores nothing: opening the link runs the same
 * assessment again.
 */

export interface SharedResult {
  professionId: string;
  answers: Record<string, string>;
  /** Shares in the order of the result's tasks (as the server returns them), when the person set them. */
  shares?: number[];
}

const ID = /^[A-Za-z0-9_-]{1,64}$/;

/** The fragment for a share link, e.g. "share=p:accountant;a:routine.lt20,people.lot;s:30,70". */
export function buildShareFragment(shared: SharedResult): string {
  const answers = Object.entries(shared.answers)
    .filter(([q, o]) => ID.test(q) && ID.test(o))
    .map(([q, o]) => `${q}.${o}`)
    .join(',');
  const parts = [`p:${shared.professionId}`, `a:${answers}`];
  if (shared.shares?.length) {
    parts.push(`s:${shared.shares.join(',')}`);
  }
  return 'share=' + encodeURIComponent(parts.join(';'));
}

/** Reads a share fragment; null for anything that is not a well-formed share link. */
export function parseShareFragment(fragment: string | null | undefined): SharedResult | null {
  const raw = (fragment ?? '').replace(/^#/, '');
  if (!raw.startsWith('share=')) {
    return null;
  }
  let body: string;
  try {
    body = decodeURIComponent(raw.slice('share='.length));
  } catch {
    return null;
  }
  const fields: Record<string, string> = {};
  for (const part of body.split(';')) {
    const i = part.indexOf(':');
    if (i > 0) {
      fields[part.slice(0, i)] = part.slice(i + 1);
    }
  }
  const professionId = fields['p'];
  if (!professionId || !ID.test(professionId)) {
    return null;
  }
  const answers: Record<string, string> = {};
  for (const pair of (fields['a'] ?? '').split(',').filter(Boolean)) {
    const [q, o, ...rest] = pair.split('.');
    if (rest.length || !q || !o || !ID.test(q) || !ID.test(o)) {
      return null;
    }
    answers[q] = o;
  }
  let shares: number[] | undefined;
  if (fields['s']) {
    shares = fields['s'].split(',').map(Number);
    const valid = shares.every(v => Number.isInteger(v) && v >= 0 && v <= 100)
      && shares.reduce((a, b) => a + b, 0) === 100;
    if (!valid) {
      shares = undefined;
    }
  }
  return shares ? { professionId, answers, shares } : { professionId, answers };
}
