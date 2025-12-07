import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent, HttpClientTestingModule]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should store email and mark as logged in on login and load progress', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    app.userEmail = 'test@example.com';
    app.login();

    const req = httpMock.expectOne(
      r => r.method === 'GET' && r.url === 'http://localhost:8080/api/interview/progress'
    );
    expect(req.request.params.get('email')).toBe('test@example.com');

    req.flush({
      totalAnswers: 0,
      averageScore: 0,
      lastScore: null,
      bestScore: null
    });

    expect(app.loggedIn).toBeTrue();
    expect(app.progress?.totalAnswers).toBe(0);
  });

  it('should call backend on generateQuestions when logged in', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    app.loggedIn = true;
    app.userEmail = 'test@example.com';
    app.resumeText = 'Test CV';
    app.role = 'backend developer';
    app.techInput = 'Java';
    app.generateQuestions();

    const req = httpMock.expectOne('http://localhost:8080/api/interview/questions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.email).toBe('test@example.com');

    req.flush([{
      text: 'Example question',
      difficulty: 'MEDIUM',
      tags: ['backend']
    }]);

    expect(app.questions.length).toBe(1);
  });

  it('should call backend on evaluateAnswer when logged in and then refresh progress', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;

    app.loggedIn = true;
    app.userEmail = 'test@example.com';
    app.selectedQuestionText = 'Räägi oma projektist';
    app.answerText = 'Näiteks viimases projektis vastutasin mina ...';
    app.role = 'backend developer';
    app.techInput = 'Java, Spring';

    app.evaluateAnswer();

    const evalReq = httpMock.expectOne('http://localhost:8080/api/interview/evaluate-answer');
    expect(evalReq.request.method).toBe('POST');
    expect(evalReq.request.body.email).toBe('test@example.com');

    evalReq.flush({
      score: 75,
      verdict: 'Hea vastus',
      summary: 'OK',
      improvementTips: []
    });

    const progressReq = httpMock.expectOne(
      r => r.method === 'GET' && r.url === 'http://localhost:8080/api/interview/progress'
    );
    expect(progressReq.request.params.get('email')).toBe('test@example.com');

    progressReq.flush({
      totalAnswers: 1,
      averageScore: 75,
      lastScore: 75,
      bestScore: 75
    });

    expect(app.evaluation?.score).toBe(75);
    expect(app.progress?.totalAnswers).toBe(1);
  });
});
