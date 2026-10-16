import { buildShareFragment, parseShareFragment } from './share-link';

describe('share link', () => {
  it('round-trips profession, answers and shares', () => {
    const shared = { professionId: 'onet-43-9021-00', answers: { routine: 'lt20', ai_usage: 'daily' }, shares: [30, 70] };
    const fragment = buildShareFragment(shared);
    expect(fragment.startsWith('share=')).toBeTrue();
    expect(parseShareFragment('#' + fragment)).toEqual(shared);
  });

  it('works without shares', () => {
    const shared = { professionId: 'accountant', answers: { hours: '3' } };
    expect(parseShareFragment(buildShareFragment(shared))).toEqual(shared);
  });

  it('rejects anything that is not a well-formed share link', () => {
    expect(parseShareFragment(null)).toBeNull();
    expect(parseShareFragment('')).toBeNull();
    expect(parseShareFragment('#token-for-a-team-report')).toBeNull();
    expect(parseShareFragment('share=' + encodeURIComponent('a:routine.lt20'))).toBeNull();
    expect(parseShareFragment('share=' + encodeURIComponent('p:<script>;a:'))).toBeNull();
    expect(parseShareFragment('share=' + encodeURIComponent('p:x;a:routine.lt20.extra'))).toBeNull();
    expect(parseShareFragment('share=%E0%A4%A')).toBeNull();
  });

  it('drops shares that do not add up to 100 but keeps the rest', () => {
    const parsed = parseShareFragment('share=' + encodeURIComponent('p:x;a:routine.lt20;s:50,60'));
    expect(parsed).toEqual({ professionId: 'x', answers: { routine: 'lt20' } });
  });

  it('carries only ids, never free text', () => {
    const fragment = buildShareFragment({ professionId: 'x', answers: { routine: 'lt20', 'bad key!': 'v', ok: 'bad value!' } });
    expect(decodeURIComponent(fragment)).toBe('share=p:x;a:routine.lt20');
  });
});
