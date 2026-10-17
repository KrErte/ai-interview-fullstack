import { TaskExposure } from '../../models/ai-readiness.model';

/** One real example the user gives for a task: the tool, the time before and now, what they check, what came of it. */
export interface ProofStory {
  task: string;
  tool: string;
  before: number | null;
  after: number | null;
  check: string;
  outcome: string;
}

/** The tasks to build answers for: the three with the largest share of the week times automation. */
export function proofTasks(tasks: TaskExposure[], count = 3): TaskExposure[] {
  return [...tasks]
    .sort((a, b) => b.share * b.automationPercent - a.share * a.automationPercent)
    .slice(0, count);
}

export function emptyStory(task: string, tool = ''): ProofStory {
  return { task, tool, before: null, after: null, check: '', outcome: '' };
}

/** A story has enough to write an answer: a tool, both times and what the user checks. */
export function isComplete(s: ProofStory): boolean {
  return !!s.tool.trim() && isMinutes(s.before) && isMinutes(s.after) && !!s.check.trim();
}

/**
 * The interview answer written from the user's own example (tool, judgment, outcome), with their own numbers.
 * Null until the story is complete. Nothing is invented: every fact comes from what the user typed.
 */
export function answerFor(profession: string, s: ProofStory): string | null {
  if (!isComplete(s)) {
    return null;
  }
  const before = s.before as number;
  const after = s.after as number;
  const parts = [`In my work as ${article(profession)} ${profession}, I use ${s.tool.trim()} for this part of my work: ${lowerFirst(withoutFinalStop(s.task))}.`];
  if (after < before) {
    parts.push(`It used to take me about ${duration(before)}; now it takes about ${duration(after)}, so I save around ${duration(before - after)} each time.`);
  } else if (after === before) {
    parts.push(`It takes about as long as before, ${duration(after)}.`);
  } else {
    parts.push(`For now it takes a little longer, ${duration(after)} instead of ${duration(before)}, while I build the routine.`);
  }
  parts.push(`I don't use the output as it is: ${sentence(lowerFirst(s.check.trim()))}`);
  if (s.outcome.trim()) {
    parts.push(sentence(upperFirst(s.outcome.trim())));
  }
  return parts.join(' ');
}

/** A starting point for "Where would you not use AI?", from the least automatable task and the human edge. */
export function notAiSuggestion(tasks: TaskExposure[], humanEdge: string[]): string {
  if (!tasks.length) {
    return '';
  }
  const human = [...tasks].sort((a, b) => a.automationPercent - b.automationPercent || b.share - a.share)[0];
  const edge = humanEdge[0] ? ` That part depends on ${lowerFirst(humanEdge[0])}, and a mistake there would cost trust.` : '';
  return `I would not hand “${withoutFinalStop(human.name)}” over to AI.${edge} I use AI around it instead, for preparation and notes, and I check what it gives me.`;
}

/** "45 minutes", "1 hour", "1.5 hours". */
export function duration(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} ${minutes === 1 ? 'minute' : 'minutes'}`;
  }
  const hours = Math.round((minutes / 60) * 10) / 10;
  return `${hours} ${hours === 1 ? 'hour' : 'hours'}`;
}

/** O*NET task names are sentences ending with a full stop. */
function withoutFinalStop(s: string): string {
  return s.endsWith('.') ? s.slice(0, -1) : s;
}

function isMinutes(v: number | null): boolean {
  return v !== null && Number.isFinite(v) && v >= 0 && v <= 100000;
}

function article(word: string): string {
  return /^[aeiou]/i.test(word) ? 'an' : 'a';
}

/** Lower-case the first letter, except for acronyms ("SQL queries") and the pronoun ("I check ..."). */
function lowerFirst(s: string): string {
  if (/^I(\s|')/.test(s)) {
    return s;
  }
  return s.length > 1 && s[1] === s[1].toLowerCase() ? s[0].toLowerCase() + s.slice(1) : s;
}

function upperFirst(s: string): string {
  return s ? s[0].toUpperCase() + s.slice(1) : s;
}

function sentence(s: string): string {
  return /[.!?]$/.test(s) ? s : `${s}.`;
}
