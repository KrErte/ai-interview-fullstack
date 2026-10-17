import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/** Products that are not sold yet; people can join a waitlist (POST /api/waitlist, public). */
export type WaitlistProduct = 'interview-prep';

@Injectable({ providedIn: 'root' })
export class WaitlistService {
  private readonly url = `${environment.apiBaseUrl}/api/waitlist`;

  constructor(private http: HttpClient) {}

  join(email: string, product: WaitlistProduct, professionId: string | null, source: string): Observable<void> {
    return this.http.post<void>(this.url, { email: email.trim(), product, professionId, source });
  }
}
