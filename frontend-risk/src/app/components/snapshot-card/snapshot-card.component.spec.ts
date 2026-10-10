import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  SnapshotCardComponent,
  getJobMatchBand,
  getJobMatchLabel,
  getJobMatchColorClass
} from './snapshot-card.component';
import { toMatchPercent } from '../../services/risk.service';

describe('job match band helpers', () => {
  const cases: Array<[number, string, string, string]> = [
    [100, 'strong', 'Strong match', 'text-emerald-400'],
    [70, 'strong', 'Strong match', 'text-emerald-400'],
    [69, 'partial', 'Partial match', 'text-amber-400'],
    [40, 'partial', 'Partial match', 'text-amber-400'],
    [39, 'weak', 'Weak match', 'text-rose-400'],
    [0, 'weak', 'Weak match', 'text-rose-400']
  ];

  for (const [score, band, label, color] of cases) {
    it(`${score} -> ${band} / "${label}" / ${color}`, () => {
      expect(getJobMatchBand(score)).toBe(band as ReturnType<typeof getJobMatchBand>);
      expect(getJobMatchLabel(score)).toBe(label);
      expect(getJobMatchColorClass(score)).toBe(color);
    });
  }
});

describe('SnapshotCardComponent (rendered)', () => {
  let fixture: ComponentFixture<SnapshotCardComponent>;
  let el: HTMLElement;

  const render = (matchPercent: number | null | undefined) => {
    fixture.componentRef.setInput('matchPercent', matchPercent);
    fixture.detectChanges();
  };
  /** The score block: role="img" element inside the "Job match" column. */
  const scoreBox = () => el.querySelector('[role="img"]') as HTMLElement;
  const digits = () => scoreBox().querySelector('span')!.textContent!.trim();
  const caption = () => (scoreBox().nextElementSibling as HTMLElement).textContent!.trim();
  const aria = () => scoreBox().getAttribute('aria-label');

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [SnapshotCardComponent] }).compileComponents();
    fixture = TestBed.createComponent(SnapshotCardComponent);
    el = fixture.nativeElement as HTMLElement;
    fixture.componentRef.setInput('confidence', 0.8);
    fixture.componentRef.setInput('signalsCount', 7);
    fixture.componentRef.setInput('coverage', 50);
  });

  it('renders the score digits, band label and aria-label', () => {
    render(85);
    expect(digits()).toBe('85');
    expect(scoreBox().textContent).toContain('%');
    expect(caption()).toBe('Strong match');
    expect(aria()).toBe('Job match: 85%, Strong match');
    expect(scoreBox().querySelector('span')!.classList).toContain('text-emerald-400');
  });

  it('renders a partial match', () => {
    render(55);
    expect(digits()).toBe('55');
    expect(caption()).toBe('Partial match');
    expect(aria()).toBe('Job match: 55%, Partial match');
  });

  it('rounds before banding so the number and label agree (69.5 -> 70% Strong match)', () => {
    render(69.5);
    expect(digits()).toBe('70');
    expect(caption()).toBe('Strong match');
    expect(aria()).toBe('Job match: 70%, Strong match');
  });

  it('does not re-introduce float error when given a noisy percent (28.499999999999996 -> 29)', () => {
    render(0.285 * 100);
    expect(digits()).toBe('29');
    expect(aria()).toBe('Job match: 29%, Weak match');
    render(0.145 * 100);
    expect(digits()).toBe('15');
  });

  it('does not double-round: 69.49 -> 69% Partial match', () => {
    render(69.49);
    expect(digits()).toBe('69');
    expect(aria()).toBe('Job match: 69%, Partial match');
  });

  // Service -> card: the card must show exactly what RiskService computed.
  const pipeline: Array<[number, string, string]> = [
    [0.145, '15', 'Weak match'],
    [0.285, '29', 'Weak match'],
    [0.565, '57', 'Partial match'],
    [0.575, '58', 'Partial match'],
    [0.695, '70', 'Strong match'],
    [0.6949, '69', 'Partial match'],
    [0.3949, '39', 'Weak match'],
    [0.395, '40', 'Partial match']
  ];
  for (const [raw, shown, label] of pipeline) {
    it(`raw score ${raw} -> service ${shown} -> card "${shown}% ${label}"`, () => {
      const pct = toMatchPercent(raw);
      expect(String(pct)).toBe(shown);
      render(pct);
      expect(digits()).toBe(shown);
      expect(caption()).toBe(label);
      expect(aria()).toBe(`Job match: ${shown}%, ${label}`);
    });
  }

  it('clamps out-of-range inputs to 0-100', () => {
    render(150);
    expect(digits()).toBe('100');
    expect(aria()).toBe('Job match: 100%, Strong match');
    render(-5);
    expect(digits()).toBe('0');
    expect(aria()).toBe('Job match: 0%, Weak match');
  });

  it('shows the empty state for null (no digits, no band label)', () => {
    render(null);
    expect(digits()).toBe('—');
    expect(aria()).toBe('Job match: not available');
    expect(caption()).toBe("We couldn't calculate a match for this job.");
    const text = el.textContent!;
    expect(text).not.toMatch(/Strong match|Partial match|Weak match/);
    expect(text).not.toContain('0%,');
  });

  it('shows the empty state for undefined and NaN as well', () => {
    render(undefined);
    expect(aria()).toBe('Job match: not available');
    render(NaN);
    expect(aria()).toBe('Job match: not available');
  });

  it('treats a real 0 as a score: 0% Weak match, not the empty state', () => {
    render(0);
    expect(digits()).toBe('0');
    expect(caption()).toBe('Weak match');
    expect(aria()).toBe('Job match: 0%, Weak match');
    expect(el.textContent).not.toContain("We couldn't calculate a match");
  });

  it('renders confidence and signal count', () => {
    render(85);
    expect(el.textContent).toContain('80.0%');
    expect(el.textContent).toContain('Based on 7 signals');
  });
});
