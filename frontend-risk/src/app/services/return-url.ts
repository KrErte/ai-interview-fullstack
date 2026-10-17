/** Where /login and /register send the user afterwards; only same-app paths, so a link cannot redirect off-site. */
export const DEFAULT_AFTER_LOGIN = '/ai-test';

export function safeReturnUrl(raw: unknown): string {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) {
    return DEFAULT_AFTER_LOGIN;
  }
  return raw;
}

/**
 * The current in-app URL with resume=paywall set, for /login after a 401 from billing, so the paywall comes back
 * with the saved answers. Falls back to the paywall on /ai-test for anything unsafe.
 */
export function paywallResumeUrl(currentUrl: unknown): string {
  const safe = safeReturnUrl(currentUrl);
  if (safe !== currentUrl) return '/ai-test?resume=paywall';
  const [pathAndQuery, hash] = safe.split('#');
  const [path, query = ''] = pathAndQuery.split('?');
  const params = new URLSearchParams(query);
  params.set('resume', 'paywall');
  return `${path}?${params.toString()}${hash ? '#' + hash : ''}`;
}
