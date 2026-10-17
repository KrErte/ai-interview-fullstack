import { answerFor, duration, emptyStory, isComplete, notAiSuggestion, proofTasks, ProofStory } from './prep-answers';
import { TaskExposure } from '../../models/ai-readiness.model';

const TASKS: TaskExposure[] = [
  { name: 'Bookkeeping entries and invoice processing', share: 30, automationPercent: 90 },
  { name: 'Reconciling bank statements and accounts', share: 20, automationPercent: 80 },
  { name: 'Reporting', share: 15, automationPercent: 70 },
  { name: 'Tax and regulatory advice', share: 20, automationPercent: 35 },
  { name: 'Working with clients', share: 15, automationPercent: 20 }
];

function story(over: Partial<ProofStory> = {}): ProofStory {
  return { ...emptyStory('Bookkeeping entries and invoice processing'), tool: 'Excel Copilot', before: 120, after: 45,
    check: 'I compare every total with the bank statement', outcome: '', ...over };
}

describe('prep answers', () => {
  it('picks the three tasks with the largest share times automation', () => {
    expect(proofTasks(TASKS).map(t => t.name)).toEqual([
      'Bookkeeping entries and invoice processing', 'Reconciling bank statements and accounts', 'Reporting']);
  });

  it('writes the answer only from what the user gave, with their own numbers', () => {
    expect(answerFor('Accountant', story({ outcome: 'month-end closes a day earlier' }))).toBe(
      'In my work as an Accountant, I use Excel Copilot for this part of my work: bookkeeping entries and invoice processing. '
      + 'It used to take me about 2 hours; now it takes about 45 minutes, so I save around 1.3 hours each time. '
      + "I don't use the output as it is: I compare every total with the bank statement. "
      + 'Month-end closes a day earlier.');
  });

  it('is honest when it is not faster yet', () => {
    expect(answerFor('Data analyst', story({ before: 30, after: 30 }))).toContain('It takes about as long as before, 30 minutes.');
    expect(answerFor('Data analyst', story({ before: 30, after: 40 }))).toContain('a little longer, 40 minutes instead of 30 minutes');
  });

  it('needs a tool, both times and the check', () => {
    expect(isComplete(story())).toBeTrue();
    expect(answerFor('Accountant', story({ tool: ' ' }))).toBeNull();
    expect(answerFor('Accountant', story({ before: null }))).toBeNull();
    expect(answerFor('Accountant', story({ check: '' }))).toBeNull();
    expect(isComplete(story({ after: -5 }))).toBeFalse();
  });

  it('keeps acronyms and the pronoun I, and formats durations', () => {
    expect(answerFor('Data analyst', story({ task: 'SQL queries' }))).toContain('for this part of my work: SQL queries.');
    expect(answerFor('Cook', story({ task: 'Season and cook food according to recipes.' })))
      .toContain('In my work as a Cook, I use Excel Copilot for this part of my work: season and cook food according to recipes. It used');
    expect(answerFor('Data analyst', story({ check: 'Totals are checked against the ledger' })))
      .toContain('as it is: totals are checked against the ledger.');
    expect(duration(1)).toBe('1 minute');
    expect(duration(60)).toBe('1 hour');
    expect(duration(90)).toBe('1.5 hours');
  });

  it('suggests where not to use AI from the least automatable task and the human edge', () => {
    expect(notAiSuggestion(TASKS, ['Accountability and sign-off'])).toBe(
      'I would not hand “Working with clients” over to AI. That part depends on accountability and sign-off, '
      + 'and a mistake there would cost trust. I use AI around it instead, for preparation and notes, and I check what it gives me.');
    expect(notAiSuggestion([], [])).toBe('');
  });
});
