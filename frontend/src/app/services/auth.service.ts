
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthUser {
  username: string;
  role: 'ADMIN' | 'EMPLOYEE';
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private http = inject(HttpClient);

  private apiUrl = '/api/auth';

  currentUser = signal<AuthUser | null>(null);

  // Solo en memoria. No guardar en localStorage.
  private basicToken: string | null = null;

  login(username: string, password: string): Observable<AuthUser> {

    const credentials = `${username}:${password}`;
    const bytes = new TextEncoder().encode(credentials);

    const token = btoa(
      Array.from(bytes, byte => String.fromCharCode(byte)).join('')
    );

    return this.http.get<AuthUser>(`${this.apiUrl}/me`, {
      headers: new HttpHeaders({
        Authorization: `Basic ${token}`
      })
    }).pipe(
      tap(user => {
        this.basicToken = token;
        this.currentUser.set(user);
      })
    );
  }

  getAuthorizationHeader(): string | null {
    return this.basicToken === null
      ? null
      : `Basic ${this.basicToken}`;
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  logout(): void {
    this.basicToken = null;
    this.currentUser.set(null);
  }
}
