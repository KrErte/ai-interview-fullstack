import { DEFAULT_AFTER_LOGIN, safeReturnUrl } from './return-url';
import { paywallResumeUrl } from './return-url';

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

describe('paywallResumeUrl', () => {
  it('adds resume=paywall to the current in-app url and rejects unsafe ones', () => {
    expect(paywallResumeUrl('/ai-test')).toBe('/ai-test?resume=paywall');
    expect(paywallResumeUrl('/ai-test?resume=roadmap&x=1')).toBe('/ai-test?resume=paywall&x=1');
    expect(paywallResumeUrl('//evil.example')).toBe('/ai-test?resume=paywall');
    expect(paywallResumeUrl(undefined)).toBe('/ai-test?resume=paywall');
  });
});

describe('paywallResumeUrl fallback', () => {
  for (const unsafe of ['//evil.example/x', 'https://evil.example', 'javascript:alert(1)', '/\\evil', '', 42]) {
    it(`unsafe ${JSON.stringify(unsafe)} gives /ai-test?resume=paywall`, () => {
      expect(paywallResumeUrl(unsafe)).toBe('/ai-test?resume=paywall');
    });
  }
});
