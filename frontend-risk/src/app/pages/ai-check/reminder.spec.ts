import { buildReminderIcs, escapeText, fold, nextSessionStart, REMINDER_WEEKS } from './reminder';

describe('weekly reminder', () => {
  it('starts on the next Monday at 18:00, never today', () => {
    const fromWednesday = nextSessionStart(new Date(2026, 9, 14, 9, 0)); // Wed 14 Oct 2026
    expect(fromWednesday.getDay()).toBe(1);
    expect(fromWednesday.getDate()).toBe(19);
    expect(fromWednesday.getHours()).toBe(18);

    const fromMonday = nextSessionStart(new Date(2026, 9, 19, 8, 0));
    expect(fromMonday.getDate()).toBe(26);
  });

  it('builds one weekly event with an alarm', () => {
    const ics = buildReminderIcs({
      professionName: 'Cooks, Restaurant',
      firstStep: 'Learn your kitchen display system',
      planUrl: 'https://example.org/ai-test?view=saved',
      start: new Date(2026, 9, 19, 18, 0),
      minutes: 60
    }, new Date(Date.UTC(2026, 9, 15, 20, 0, 0)));
    const unfolded = ics.replace(/\r\n /g, '');
    expect(ics.startsWith('BEGIN:VCALENDAR\r\n')).toBeTrue();
    expect(ics.endsWith('END:VCALENDAR\r\n')).toBeTrue();
    expect(unfolded).toContain('DTSTART:20261019T180000');
    expect(unfolded).toContain('DTSTAMP:20261015T200000Z');
    expect(unfolded).toContain(`RRULE:FREQ=WEEKLY;COUNT=${REMINDER_WEEKS}`);
    expect(unfolded).toContain('DURATION:PT60M');
    expect(unfolded).toContain('SUMMARY:AI practice: Cooks\\, Restaurant');
    expect(unfolded).toContain('BEGIN:VALARM');
    expect(ics.split('\r\n').every(line => new TextEncoder().encode(line).length <= 75)).toBeTrue();
  });

  it('escapes calendar text', () => {
    expect(escapeText('a, b; c\\d\ne')).toBe('a\\, b\\; c\\\\d\\ne');
  });

  it('folds long lines without splitting characters', () => {
    const folded = fold('DESCRIPTION:' + 'ä'.repeat(80));
    const lines = folded.split('\r\n');
    expect(lines.length).toBeGreaterThan(1);
    expect(lines.every(l => new TextEncoder().encode(l).length <= 75)).toBeTrue();
    expect(folded.replace(/\r\n /g, '')).toBe('DESCRIPTION:' + 'ä'.repeat(80));
  });
});
