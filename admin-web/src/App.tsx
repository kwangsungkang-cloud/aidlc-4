import { Routes, Route, Navigate, useNavigate, Link, useLocation } from 'react-router-dom';
import { useAuth } from '@/common/hooks/useAuth';
import LoginPage from '@/modules/auth/LoginPage';
import DashboardPage from '@/modules/dashboard/DashboardPage';
import OrderManagementPage from '@/modules/order-management/OrderManagementPage';
import TableManagementPage from '@/modules/table-management/TableManagementPage';
import MenuManagementPage from '@/modules/menu-management/MenuManagementPage';
import AdminManagementPage from '@/modules/admin-management/AdminManagementPage';
import { Button } from '@/common/components/ui/button';

function NavBar({ storeName, username, role, onLogout }: {
  storeName: string | null; username: string | null; role: string | null; onLogout: () => void;
}) {
  const location = useLocation();
  const isActive = (path: string) => location.pathname === path;

  const adminLinks = [
    { path: '/dashboard', label: '대시보드' },
    { path: '/orders', label: '주문 관리' },
    { path: '/tables', label: '테이블 관리' },
    { path: '/menus', label: '메뉴 관리' },
  ];

  const superAdminLinks = [
    { path: '/admin-management', label: '관리자 관리' },
  ];

  const links = role === 'super-admin' ? superAdminLinks : adminLinks;

  return (
    <nav className="border-b bg-background px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-6">
        <span className="font-semibold text-lg">
          {storeName || '슈퍼 관리자'}
        </span>
        <div className="flex gap-1">
          {links.map((link) => (
            <Link key={link.path} to={link.path}>
              <Button variant={isActive(link.path) ? 'default' : 'ghost'} size="sm">
                {link.label}
              </Button>
            </Link>
          ))}
        </div>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-sm text-muted-foreground">{username}</span>
        <Button variant="outline" size="sm" onClick={onLogout}>로그아웃</Button>
      </div>
    </nav>
  );
}

export default function App() {
  const { isLoggedIn, role, storeName, username, loginAsAdmin, loginAsSuperAdmin, logout } = useAuth();
  const navigate = useNavigate();

  const handleAdminLogin = (token: string, storeName: string, username: string) => {
    loginAsAdmin(token, storeName, username);
    navigate('/dashboard');
  };

  const handleSuperAdminLogin = (token: string, username: string) => {
    loginAsSuperAdmin(token, username);
    navigate('/admin-management');
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isLoggedIn) {
    return (
      <Routes>
        <Route path="/login" element={<LoginPage onAdminLogin={handleAdminLogin} onSuperAdminLogin={handleSuperAdminLogin} />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    );
  }

  const defaultPath = role === 'super-admin' ? '/admin-management' : '/dashboard';

  return (
    <div className="min-h-screen">
      <NavBar storeName={storeName} username={username} role={role} onLogout={handleLogout} />
      <Routes>
        <Route path="/dashboard" element={<DashboardPage onSelectTable={() => navigate('/orders')} />} />
        <Route path="/orders" element={<OrderManagementPage />} />
        <Route path="/tables" element={<TableManagementPage />} />
        <Route path="/menus" element={<MenuManagementPage />} />
        <Route path="/admin-management" element={<AdminManagementPage />} />
        <Route path="*" element={<Navigate to={defaultPath} replace />} />
      </Routes>
    </div>
  );
}
