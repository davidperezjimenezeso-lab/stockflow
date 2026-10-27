import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StockMovement {
  id: number;
  productId: number;
  productName: string;
  movementType: 'IN' | 'OUT';
  quantity: number;
  resultingStock: number;
  createdAt: string;
}

export interface StockMovementRequest {
  productId: number;
  movementType: 'IN' | 'OUT';
  quantity: number;
}

@Injectable({
  providedIn: 'root'
})
export class StockMovementService {

  private http = inject(HttpClient);

  private apiUrl = '/api/stock-movements';

  getMovements(): Observable<StockMovement[]> {
    return this.http.get<StockMovement[]>(this.apiUrl);
  }

  createMovement(
    movement: StockMovementRequest
  ): Observable<StockMovement> {
    return this.http.post<StockMovement>(this.apiUrl, movement);
  }
}