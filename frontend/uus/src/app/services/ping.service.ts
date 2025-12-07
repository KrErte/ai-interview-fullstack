import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PingService {
  constructor(private http: HttpClient) {}
  ping(): Observable<string> {
    return this.http.get(`${environment.apiBaseUrl}/ping`, { responseType: 'text' });
  }
}
