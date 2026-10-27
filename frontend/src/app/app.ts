
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { AuthService, AuthUser } from './services/auth.service';
import { ProductService } from './services/product.service';
import { CategoryService, Category } from './services/category.service';
import { Product } from './models/product';
import { SaleService, Sale, SaleItemRequest } from './services/sale.service';
import {
  StockMovementService,
  StockMovement,
  StockMovementRequest
} from './services/stock-movement.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {

  // SERVICIOS
  private productService = inject(ProductService);
  private categoryService = inject(CategoryService);
  private movementService = inject(StockMovementService);
  private saleService = inject(SaleService);


  authService = inject(AuthService);

loginUsername = '';
loginPassword = '';

loggingIn = signal(false);
loginError = signal('');

isAuthenticated = computed(() =>
  this.authService.currentUser() !== null
);

isAdmin = computed(() =>
  this.authService.currentUser()?.role === 'ADMIN'
);
sales = signal<Sale[]>([]);
cart = signal<SaleItemRequest[]>([]);
savingSale = signal(false);

selectedSaleProductId = 0;
selectedSaleQuantity = 1;

cartTotal = computed(() => {
  return this.cart().reduce((total, item) => {
    const product = this.products().find(p => p.id === item.productId);
    return total + (product?.price ?? 0) * item.quantity;
  }, 0);
});


  // STOCK-030B: Navegación entre secciones
  activeSection = signal<
    'dashboard' | 'products' | 'movements' | 'sales'
  >('dashboard');

  navigateTo(
    section: 'dashboard' | 'products' | 'movements' | 'sales'
  ): void {
    this.activeSection.set(section);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }



  // STOCK-030: Estadísticas del dashboard

  totalProducts = computed(() => this.products().length);

  totalStockUnits = computed(() =>
    this.products().reduce(
      (total, product) => total + product.stock,
      0
    )
  );

  inventoryValue = computed(() =>
    this.products().reduce(
      (total, product) => total + product.price * product.stock,
      0
    )
  );

  lowStockProducts = computed(() =>
    this.products().filter(product => product.stock <= 5)
  );

  totalSalesAmount = computed(() =>
    this.sales().reduce(
      (total, sale) => total + sale.total,
      0
    )
  );

  totalSalesCount = computed(() => this.sales().length);

  // DATOS
  products = signal<Product[]>([]);
  categories = signal<Category[]>([]);
  movements = signal<StockMovement[]>([]);

  // ESTADOS
  error = signal('');
  success = signal('');

  saving = signal(false);
  savingMovement = signal(false);

  deletingId = signal<number | null>(null);
  editingId = signal<number | null>(null);

  // FILTROS
  filterCategoryId = signal<number | null>(null);

  filteredProducts = computed(() => {
    const categoryId = this.filterCategoryId();

    return categoryId === null
      ? this.products()
      : this.products().filter(p => p.categoryId === categoryId);
  });

  // FORMULARIO DE PRODUCTOS
  newProduct = {
    name: '',
    price: 0,
    stock: 0,
    categoryId: null as number | null
  };

  // FORMULARIO DE MOVIMIENTOS
  newMovement: StockMovementRequest = {
    productId: 0,
    movementType: 'IN',
    quantity: 1
  };

  // INICIALIZACIÓN
  ngOnInit(): void {

  }

  // =========================
  // PRODUCTOS
  // =========================

  loadProducts(): void {
    this.productService.getProducts().subscribe({
      next: (data: Product[]) => {
        this.products.set(data);
        this.error.set('');
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar productos:', err);
        this.error.set('No se pudieron cargar los productos');
      }
    });
  }


loadSales(): void {
  this.saleService.getSales().subscribe({
    next: (data: Sale[]) => {
      this.sales.set(data);
    },
    error: (err: HttpErrorResponse) => {
      console.error('Error al cargar ventas:', err);
      this.error.set('No se pudo cargar el historial de ventas');
    }
  });
}

addToCart(): void {
  const productId = Number(this.selectedSaleProductId);
  const quantity = Number(this.selectedSaleQuantity);

  const product = this.products().find(p => p.id === productId);

  if (!product || !Number.isInteger(quantity) || quantity <= 0) {
    this.error.set('Selecciona un producto y una cantidad válida');
    return;
  }

  const existing = this.cart().find(item => item.productId === productId);
  const totalQuantity = (existing?.quantity ?? 0) + quantity;

  if (totalQuantity > product.stock) {
    this.error.set('No hay suficiente stock para añadir esa cantidad');
    return;
  }

  this.cart.update(items => {
    if (existing) {
      return items.map(item =>
        item.productId === productId
          ? { ...item, quantity: totalQuantity }
          : item
      );
    }

    return [...items, { productId, quantity }];
  });

  this.error.set('');
  this.selectedSaleProductId = 0;
  this.selectedSaleQuantity = 1;
}

removeFromCart(productId: number): void {
  this.cart.update(items =>
    items.filter(item => item.productId !== productId)
  );
}

getCartProductName(productId: number): string {
  return this.products().find(p => p.id === productId)?.name ?? 'Producto desconocido';
}

getCartProductPrice(productId: number): number {
  return this.products().find(p => p.id === productId)?.price ?? 0;
}

saveSale(): void {
  if (this.savingSale() || this.cart().length === 0) return;

  this.savingSale.set(true);
  this.error.set('');
  this.success.set('');

  this.saleService.createSale({ items: this.cart() }).subscribe({
    next: (sale: Sale) => {
      this.cart.set([]);
      this.selectedSaleProductId = 0;
      this.selectedSaleQuantity = 1;

      this.loadProducts();
      this.loadMovements();
      this.loadSales();

      this.success.set(
        `Venta #${sale.id} registrada correctamente. Total: ${sale.total.toFixed(2)} €`
      );

      this.savingSale.set(false);
    },
    error: (err: HttpErrorResponse) => {
      console.error('Error al registrar venta:', err);

      this.error.set(
        err.status === 400
          ? 'Venta rechazada: comprueba las existencias y las cantidades'
          : err.status === 404
            ? 'Uno de los productos ya no existe'
            : 'No se pudo registrar la venta'
      );

      this.savingSale.set(false);
    }
  });
}


  loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next: (data: Category[]) => {
        this.categories.set(data);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar categorías:', err);
        this.error.set('No se pudieron cargar las categorías');
      }
    });
  }

  saveProduct(): void {
    if (this.saving() || this.savingMovement()) return;

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const productData = {
      ...this.newProduct,
      categoryId: this.newProduct.categoryId === null
        ? null
        : Number(this.newProduct.categoryId)
    };

    const id = this.editingId();

    const request = id === null
      ? this.productService.createProduct(productData)
      : this.productService.updateProduct(id, productData);

    request.subscribe({
      next: (product: Product) => {
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
      error: (err: HttpErrorResponse) => {
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
      stock: product.stock,
      categoryId: product.categoryId
    };

    this.error.set('');
    this.success.set('');
  }

  cancelEdit(): void {
    this.editingId.set(null);

    this.newProduct = {
      name: '',
      price: 0,
      stock: 0,
      categoryId: null
    };
  }

  deleteProduct(product: Product): void {
    if (
      this.deletingId() !== null ||
      this.saving() ||
      this.savingMovement()
    ) return;

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
      error: (err: HttpErrorResponse) => {
        console.error('Error al eliminar producto:', err);

        this.error.set(
          err.status === 409 || err.status === 500
            ? 'No se puede eliminar un producto con movimientos de inventario'
            : 'No se pudo eliminar el producto'
        );

        this.deletingId.set(null);
      }
    });
  }

  changeCategoryFilter(value: string): void {
    this.filterCategoryId.set(
      value === '' ? null : Number(value)
    );
  }

  // =========================
  // MOVIMIENTOS DE INVENTARIO
  // =========================

  loadMovements(): void {
    this.movementService.getMovements().subscribe({
      next: (data: StockMovement[]) => {
        this.movements.set(data);
      },
      error: (err: HttpErrorResponse) => {
        console.error('Error al cargar movimientos:', err);
        this.error.set('No se pudo cargar el historial de inventario');
      }
    });
  }

  saveMovement(): void {
    if (this.savingMovement() || this.saving()) return;

    this.savingMovement.set(true);
    this.error.set('');
    this.success.set('');

    const request: StockMovementRequest = {
      productId: Number(this.newMovement.productId),
      movementType: this.newMovement.movementType,
      quantity: Number(this.newMovement.quantity)
    };

    if (
      !Number.isInteger(request.productId) ||
      request.productId <= 0 ||
      !Number.isInteger(request.quantity) ||
      request.quantity <= 0
    ) {
      this.error.set('Selecciona un producto y una cantidad válida');
      this.savingMovement.set(false);
      return;
    }

    this.movementService.createMovement(request).subscribe({
      next: (movement: StockMovement) => {

        // Actualiza inmediatamente el stock visible
        this.products.update(products =>
          products.map(product =>
            product.id === movement.productId
              ? { ...product, stock: movement.resultingStock }
              : product
          )
        );

        // Añade el movimiento al historial
        this.movements.update(movements => [movement, ...movements]);

        // Sincroniza los datos con PostgreSQL
        this.loadProducts();
        this.loadMovements();

        this.newMovement = {
          productId: 0,
          movementType: 'IN',
          quantity: 1
        };

        this.success.set('Movimiento registrado correctamente');
        this.savingMovement.set(false);
      },

      error: (err: HttpErrorResponse) => {
        console.error('Error al registrar movimiento:', err);

        this.error.set(
          err.status === 400
            ? 'Movimiento no válido o stock insuficiente'
            : err.status === 404
              ? 'El producto seleccionado no existe'
              : 'No se pudo registrar el movimiento'
        );

        this.savingMovement.set(false);
      }

    });

  }

login(): void {
  if (this.loggingIn()) return;

  this.loggingIn.set(true);
  this.loginError.set('');

  this.authService.login(
    this.loginUsername.trim(),
    this.loginPassword
  ).subscribe({
    next: (user: AuthUser) => {
      this.loginPassword = '';

      this.loadProducts();
      this.loadCategories();
      this.loadMovements();
      this.loadSales();

      this.loggingIn.set(false);
    },
    error: (err: HttpErrorResponse) => {
      console.error('Error de autenticación:', err.status);

      this.loginError.set(
        err.status === 401
          ? 'Usuario o contraseña incorrectos'
          : 'No se pudo conectar con el servidor'
      );

      this.loggingIn.set(false);
    }
  });
}

logout(): void {
  this.authService.logout();

  this.loginUsername = '';
  this.loginPassword = '';

  this.products.set([]);
  this.categories.set([]);
  this.movements.set([]);
  this.sales.set([]);
  this.cart.set([]);

  this.cancelEdit();

  this.error.set('');
  this.success.set('');
  this.loginError.set('');
}

}
