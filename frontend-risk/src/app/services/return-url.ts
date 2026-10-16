/** Where /login and /register send the user afterwards; only same-app paths, so a link cannot redirect off-site. */
export const DEFAULT_AFTER_LOGIN = '/ai-test';

export function safeReturnUrl(raw: unknown): string {
  if (typeof raw !== 'string' || !raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\')) {
    return DEFAULT_AFTER_LOGIN;
  }
  return raw;
}
