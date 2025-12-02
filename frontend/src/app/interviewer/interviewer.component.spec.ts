import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { InterviewerComponent } from './interviewer.component';
import { InterviewSessionService } from '../services/interview-session.service';
import { of, throwError } from 'rxjs';

describe('InterviewerComponent', () => {
  let component: InterviewerComponent;
  let fixture: any;
  let sessionServiceSpy: jasmine.SpyObj<InterviewSessionService>;

  beforeEach(async () => {
    sessionServiceSpy = jasmine.createSpyObj('InterviewSessionService', ['createSession']);

    await TestBed.configureTestingModule({
      imports: [InterviewerComponent],
      providers: [
        { provide: InterviewSessionService, useValue: sessionServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InterviewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should show error when email is empty', () => {
    component.email = '';
    component.startTraining();

    expect(component.error).toBe('Palun sisesta e-posti aadress.');
    expect(sessionServiceSpy.createSession).not.toHaveBeenCalled();
  });

  it('should call service when email is present', () => {
    sessionServiceSpy.createSession.and.returnValue(of({}));

    component.email = 'test@example.com';
    component.startTraining();

    expect(sessionServiceSpy.createSession).toHaveBeenCalledWith({
      email: 'test@example.com',
      targetRole: component.targetRole,
      style: component.interviewerStyle
    });
  });

  it('should handle backend error', () => {
    sessionServiceSpy.createSession.and.returnValue(throwError(() => new Error('fail')));

    component.email = 'test@example.com';
    component.startTraining();

    expect(component.error).toBeTruthy();
    expect(component.loading).toBeFalse();
  });
});
