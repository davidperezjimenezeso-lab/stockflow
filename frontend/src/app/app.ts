import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ProductService } from './services/product.service';
import { Product } from './models/product';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {

  private productService = inject(ProductService);

  products = signal<Product[]>([]);

  error = signal('');
  success = signal('');
  saving = signal(false);
  deletingId = signal<number | null>(null);
  editingId = signal<number | null>(null);

  newProduct = {
    name: '',
    price: 0,
    stock: 0
  };

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.productService.getProducts().subscribe({
      next: (data) => {
        this.products.set(data);
        this.error.set('');
      },
      error: (err) => {
        console.error('Error al cargar productos:', err);
        this.error.set('No se pudieron cargar los productos');
      }
    });
  }

  saveProduct(): void {

    if (this.saving()) return;

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const productData = { ...this.newProduct };
    const id = this.editingId();

    const request = id === null
      ? this.productService.createProduct(productData)
      : this.productService.updateProduct(id, productData);

    request.subscribe({
      next: (product) => {

        if (id === null) {
          this.products.update(products => [...products, product]);
          this.success.set('Producto creado correctamente');
        } else {
          this.products.update(products =>
            products.map(p => p.id === product.id ? product : p)
          );
          this.success.set('Producto actualizado correctamente');
        }

        this.cancelEdit();
        this.saving.set(false);
      },
      error: (err) => {
        console.error('Error al guardar producto:', err);
        this.error.set('No se pudo guardar el producto');
        this.saving.set(false);
      }
    });
  }

  editProduct(product: Product): void {

    this.editingId.set(product.id);

    this.newProduct = {
      name: product.name,
      price: product.price,
      stock: product.stock
    };

    this.error.set('');
    this.success.set('');
  }

  cancelEdit(): void {

    this.editingId.set(null);

    this.newProduct = {
      name: '',
      price: 0,
      stock: 0
    };
  }

  deleteProduct(product: Product): void {

    if (this.deletingId() !== null || this.saving()) return;

    const confirmed = window.confirm(
      `¿Seguro que quieres eliminar "${product.name}"?`
    );

    if (!confirmed) return;

    this.deletingId.set(product.id);
    this.error.set('');
    this.success.set('');

    this.productService.deleteProduct(product.id).subscribe({
      next: () => {

        this.products.update(products =>
          products.filter(p => p.id !== product.id)
        );

        if (this.editingId() === product.id) {
          this.cancelEdit();
        }

        this.success.set('Producto eliminado correctamente');
        this.deletingId.set(null);
      },
      error: (err) => {
        console.error('Error al eliminar producto:', err);
        this.error.set('No se pudo eliminar el producto');
        this.deletingId.set(null);
      }
    });
  }
}