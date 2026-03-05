// Auth
export interface TableLoginRequest {
  storeCode: string;
  tableNumber: number;
  password: string;
}

export interface TableLoginResponse {
  token: string;
  storeName: string;
  storeCode: string;
  tableNumber: number;
  sessionId: number;
  isNewSession: boolean;
}

// Menu
export interface Category {
  categoryId: number;
  name: string;
  menus: MenuItem[];
}

export interface MenuItem {
  menuId: number;
  name: string;
  price: number;
  description: string | null;
  imageUrl: string | null;
}

export interface MenuResponse {
  categories: Category[];
}

// Cart
export interface CartItem {
  menuId: number;
  name: string;
  price: number;
  quantity: number;
  imageUrl: string | null;
}

// Order
export interface CreateOrderRequest {
  items: { menuId: number; quantity: number }[];
}

export interface OrderItemResponse {
  orderItemId?: number;
  menuId?: number;
  menuName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface OrderResponse {
  orderId: number;
  orderNumber: string;
  status: 'PENDING' | 'PREPARING' | 'COMPLETED';
  totalAmount: number;
  items: OrderItemResponse[];
  createdAt: string;
}

export interface OrderListResponse {
  sessionId: number;
  orders: OrderResponse[];
  sessionTotalAmount: number;
}

export interface CreateOrderResponse {
  orderId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  items: OrderItemResponse[];
  createdAt: string;
}
