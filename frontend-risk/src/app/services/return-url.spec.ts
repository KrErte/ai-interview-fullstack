import { DEFAULT_AFTER_LOGIN, safeReturnUrl } from './return-url';

describe('safeReturnUrl', () => {
  it('keeps same-app paths with their query', () => {
    expect(safeReturnUrl('/ai-test?resume=paywall')).toBe('/ai-test?resume=paywall');
  });
  it('falls back for missing, absolute, protocol-relative or backslash urls', () => {
    for (const bad of [undefined, null, '', 'https://evil.example', '//evil.example', '/\\evil.example', 42]) {
      expect(safeReturnUrl(bad)).toBe(DEFAULT_AFTER_LOGIN);
    }
  });
});
