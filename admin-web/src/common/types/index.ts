export interface AdminLoginResponse {
  token: string;
  storeName: string;
  storeCode: string;
  adminId: number;
  username: string;
}

export interface SuperAdminLoginResponse {
  token: string;
  superAdminId: number;
  username: string;
}

export interface DashboardTable {
  tableId: number;
  tableNumber: number;
  sessionId: number | null;
  sessionStatus: string | null;
  totalOrderAmount: number;
  orderCount: number;
  recentOrders: DashboardOrder[];
}

export interface DashboardOrder {
  orderId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  itemSummary: string;
  createdAt: string;
}

export interface DashboardResponse {
  storeId: number;
  tables: DashboardTable[];
}

export interface OrderItem {
  menuName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  orderId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
}

export interface HistorySession {
  sessionId: number;
  startedAt: string;
  completedAt: string;
  totalAmount: number;
  orders: Order[];
}

export interface TableHistoryResponse {
  tableId: number;
  tableNumber: number;
  sessions: HistorySession[];
}

export interface MenuCategory {
  categoryId: number;
  name: string;
  menus: MenuItem[];
}

export interface MenuItem {
  menuId: number;
  name: string;
  price: number;
  description: string;
  imageUrl: string | null;
  displayOrder: number;
}

export interface AdminUser {
  adminId: number;
  username: string;
  loginAttempts: number;
  locked: boolean;
  createdAt: string;
}

export interface AuditLog {
  logId: number;
  performerUsername: string;
  targetAdminId: number;
  targetUsername: string;
  storeId: number;
  storeName: string;
  actionType: string;
  performedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type SseOrderEvent = {
  eventType: string;
  orderId: number;
  orderNumber: string;
  tableNumber: number;
  totalAmount?: number;
  itemCount?: number;
  itemSummary?: string;
  previousStatus?: string;
  newStatus?: string;
  deletedAmount?: number;
  createdAt?: string;
  updatedAt?: string;
  deletedAt?: string;
};

export type SseSessionEvent = {
  eventType: string;
  tableId: number;
  tableNumber: number;
  sessionId: number;
  completedAt: string;
  totalOrderAmount: number;
  orderCount: number;
};
