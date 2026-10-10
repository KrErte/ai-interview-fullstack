import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WeaknessesCardComponent } from './weaknesses-card.component';

describe('WeaknessesCardComponent', () => {
  let fixture: ComponentFixture<WeaknessesCardComponent>;
  let el: HTMLElement;

  const render = (weaknesses: string[] | null | undefined) => {
    fixture.componentRef.setInput('weaknesses', weaknesses);
    fixture.detectChanges();
  };
  const items = () =>
    Array.from(el.querySelectorAll('p.text-slate-200')).map(p => p.textContent!.trim());
  const gapCount = () => el.querySelector('[data-testid="gap-count"]');

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [WeaknessesCardComponent] }).compileComponents();
    fixture = TestBed.createComponent(WeaknessesCardComponent);
    el = fixture.nativeElement as HTMLElement;
  });

  it("shows 'No major gaps found' for an empty list", () => {
    render([]);
    expect(el.textContent).toContain('No major gaps found');
    expect(items()).toEqual([]);
    expect(gapCount()).toBeNull();
  });

  it('treats null and undefined input as an empty list without throwing', () => {
    expect(() => render(null)).not.toThrow();
    expect(fixture.componentInstance.weaknesses).toEqual([]);
    expect(el.textContent).toContain('No major gaps found');
    expect(() => render(undefined)).not.toThrow();
    expect(el.textContent).toContain('No major gaps found');
  });

  it('renders numbered items and hides the empty state', () => {
    render(['Weak SQL', 'Missing skill: Docker']);
    expect(items()).toEqual(['Weak SQL', 'Missing skill: Docker']);
    expect(el.textContent).not.toContain('No major gaps found');
    expect(el.textContent).toContain('1');
    expect(el.textContent).toContain('2');
  });

  it("labels the count 'Gaps found' and counts every listed entry (incl. missing skills)", () => {
    render(['Weak SQL', 'Missing skill: Docker', 'Missing skill: AWS']);
    expect(el.textContent).toContain('Gaps found');
    expect(el.textContent).not.toContain('Signal Coverage');
    expect(gapCount()!.textContent!.trim()).toBe('3');
  });

  it('shows the first 3 items and toggles the rest with "Show all N gaps"', () => {
    render(['a', 'b', 'c', 'd', 'e']);
    expect(items()).toEqual(['a', 'b', 'c']);
    expect(gapCount()!.textContent!.trim()).toBe('5');
    const button = el.querySelector('button') as HTMLButtonElement;
    expect(button.textContent).toContain('Show all 5 gaps');
    button.click();
    fixture.detectChanges();
    expect(items()).toEqual(['a', 'b', 'c', 'd', 'e']);
    expect(button.textContent).toContain('Show less');
  });
});
