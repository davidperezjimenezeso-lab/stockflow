export interface Product {
  id: number;
  name: string;
  price: number;
  stock: number;
  categoryId: number | null;
categoryName: string | null;
}