
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SaleItemRequest {
  productId: number;
  quantity: number;
}

export interface SaleRequest {
  items: SaleItemRequest[];
}

export interface SaleItem {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Sale {
  id: number;
  createdAt: string;
  total: number;
  items: SaleItem[];
}

@Injectable({
  providedIn: 'root'
})
export class SaleService {

  private http = inject(HttpClient);
  private apiUrl = '/api/sales';

  getSales(): Observable<Sale[]> {
    return this.http.get<Sale[]>(this.apiUrl);
  }

  getSale(id: number): Observable<Sale> {
    return this.http.get<Sale>(`${this.apiUrl}/${id}`);
  }

  createSale(request: SaleRequest): Observable<Sale> {
    return this.http.post<Sale>(this.apiUrl, request);
  }
}
