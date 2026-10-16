import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TeamService } from './team.service';
import { environment } from '../../environments/environment';

describe('TeamService', () => {
  let service: TeamService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TeamService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('accepts only well-formed invite codes, in any case', () => {
    expect(TeamService.normalizeCode(' abcdefgh23 ')).toBe('ABCDEFGH23');
    expect(TeamService.normalizeCode('ABCDEFGH1')).toBeNull();
    expect(TeamService.normalizeCode('ABCDEFGH10')).toBeNull();
    expect(TeamService.normalizeCode('')).toBeNull();
    expect(TeamService.normalizeCode(null)).toBeNull();
  });

  it('remembers this browser\'s answer per team', () => {
    expect(service.editToken('ABCDEFGH23')).toBeNull();
    service.rememberAnswer('ABCDEFGH23', 'tok');
    expect(service.editToken('ABCDEFGH23')).toBe('tok');
    expect(service.editToken('ZZZZZZZZ22')).toBeNull();
  });

  it('sends the report token in the path of the API call only', () => {
    service.report('a/b').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/teams/report/a%2Fb`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('creates a team with name and email', () => {
    service.create('Finance', 'owner@example.org').subscribe();
    const req = http.expectOne(`${environment.apiBaseUrl}/api/teams`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Finance', ownerEmail: 'owner@example.org' });
    req.flush({});
  });
});
