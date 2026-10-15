/**
 * A weekly calendar reminder (.ics) for practising the roadmap. Built in the browser, so it needs no
 * account and sends nothing anywhere. One event a week at the same local time, for WEEKS weeks.
 */

export const REMINDER_WEEKS = 12;

export interface ReminderInput {
  professionName: string;
  /** What to do in the first session, e.g. the "start this week" step. */
  firstStep: string;
  /** Where to find the plan again. */
  planUrl: string;
  /** First session; the reminder repeats weekly at this local time. */
  start: Date;
  /** Minutes per session. */
  minutes: number;
}

/** The next Monday at 18:00 local time, at least one day ahead. */
export function nextSessionStart(now: Date): Date {
  const start = new Date(now);
  start.setHours(18, 0, 0, 0);
  const daysToMonday = (8 - start.getDay()) % 7 || 7;
  start.setDate(start.getDate() + daysToMonday);
  return start;
}

export function buildReminderIcs(input: ReminderInput, now: Date = new Date()): string {
  const description = [
    `First session: ${input.firstStep}`,
    '',
    `Your plan: ${input.planUrl}`
  ].join('\n');
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//AI roadmap//Weekly practice//EN',
    'CALSCALE:GREGORIAN',
    'BEGIN:VEVENT',
    `UID:${now.getTime()}-${Math.random().toString(36).slice(2)}@ai-roadmap`,
    `DTSTAMP:${utcStamp(now)}`,
    // floating local time: the session stays at 18:00 wherever the calendar is
    `DTSTART:${localStamp(input.start)}`,
    `DURATION:PT${Math.max(15, Math.round(input.minutes))}M`,
    `RRULE:FREQ=WEEKLY;COUNT=${REMINDER_WEEKS}`,
    `SUMMARY:${escapeText(`AI practice: ${input.professionName}`)}`,
    `DESCRIPTION:${escapeText(description)}`,
    'BEGIN:VALARM',
    'ACTION:DISPLAY',
    'TRIGGER:-PT15M',
    `DESCRIPTION:${escapeText('AI practice in 15 minutes')}`,
    'END:VALARM',
    'END:VEVENT',
    'END:VCALENDAR'
  ];
  return lines.map(fold).join('\r\n') + '\r\n';
}

/** RFC 5545 text: backslash, semicolon and comma are escaped, line breaks become \n. */
export function escapeText(text: string): string {
  return text
    .replace(/\\/g, '\\\\')
    .replace(/;/g, '\\;')
    .replace(/,/g, '\\,')
    .replace(/\r?\n/g, '\\n');
}

/** RFC 5545 lines are at most 75 octets; longer ones continue on lines starting with a space. */
export function fold(line: string): string {
  const encoder = new TextEncoder();
  const parts: string[] = [];
  let current = '';
  let size = 0;
  for (const ch of line) {
    const bytes = encoder.encode(ch).length;
    const limit = parts.length === 0 ? 75 : 74;
    if (size + bytes > limit) {
      parts.push(current);
      current = '';
      size = 0;
    }
    current += ch;
    size += bytes;
  }
  parts.push(current);
  return parts.join('\r\n ');
}

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

function localStamp(d: Date): string {
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T${pad(d.getHours())}${pad(d.getMinutes())}00`;
}

function utcStamp(d: Date): string {
  return `${d.getUTCFullYear()}${pad(d.getUTCMonth() + 1)}${pad(d.getUTCDate())}T${pad(d.getUTCHours())}${pad(d.getUTCMinutes())}${pad(d.getUTCSeconds())}Z`;
}
