import { useState, useCallback } from 'react';
import { useAuth } from '@/common/hooks/useAuth';
import { useCart } from '@/common/hooks/useCart';
import { SetupPage } from '@/modules/auth/SetupPage';
import { WelcomePage } from '@/modules/welcome/WelcomePage';
import { MenuPage } from '@/modules/menu/MenuPage';
import { CartPage } from '@/modules/cart/CartPage';
import { OrderConfirmPage } from '@/modules/order/OrderConfirmPage';
import { OrderSuccessPage } from '@/modules/order/OrderSuccessPage';
import { OrderListPage } from '@/modules/order/OrderListPage';
import { SplitBillPage } from '@/modules/split-bill/SplitBillPage';
import type { OrderResponse } from '@/common/types';

type Page = 'setup' | 'welcome' | 'menu' | 'cart' | 'order-confirm' | 'order-success' | 'order-list' | 'split-bill';

const WELCOME_SEEN_KEY = 'welcomeSeen';

function App() {
  const auth = useAuth();
  const cart = useCart();
  const [page, setPage] = useState<Page>('setup');
  const [orderNumber, setOrderNumber] = useState('');
  const [splitBillData, setSplitBillData] = useState<{ orders: OrderResponse[]; sessionTotal: number } | null>(null);

  // Determine initial page after auth
  const handleAuthSuccess = useCallback(() => {
    const seen = localStorage.getItem(WELCOME_SEEN_KEY);
    if (auth.isNewSession || !seen) {
      setPage('welcome');
    } else {
      setPage('menu');
    }
  }, [auth.isNewSession]);

  // If auth state changes to authenticated, navigate
  if (auth.isAuthenticated && page === 'setup') {
    handleAuthSuccess();
  }

  // Loading state
  if (auth.isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center space-y-2">
          <div className="text-4xl">🍽️</div>
          <p className="text-muted-foreground">연결 중...</p>
        </div>
      </div>
    );
  }

  // Not authenticated
  if (!auth.isAuthenticated) {
    return <SetupPage onLogin={auth.login} error={auth.error} isLoading={auth.isLoading} />;
  }

  switch (page) {
    case 'welcome':
      return (
        <WelcomePage
          storeName={auth.storeName}
          tableNumber={auth.tableNumber}
          onStart={() => {
            localStorage.setItem(WELCOME_SEEN_KEY, 'true');
            setPage('menu');
          }}
        />
      );

    case 'menu':
      return (
        <MenuPage
          cart={cart.items}
          onAddToCart={cart.addItem}
          onGoToCart={() => setPage('cart')}
          onGoToOrders={() => setPage('order-list')}
          totalCount={cart.totalCount}
          totalAmount={cart.totalAmount}
        />
      );

    case 'cart':
      return (
        <CartPage
          items={cart.items}
          totalAmount={cart.totalAmount}
          onUpdateQuantity={cart.updateQuantity}
          onRemoveItem={cart.removeItem}
          onClearCart={cart.clearCart}
          onGoBack={() => setPage('menu')}
          onOrder={() => setPage('order-confirm')}
        />
      );

    case 'order-confirm':
      return (
        <OrderConfirmPage
          items={cart.items}
          totalAmount={cart.totalAmount}
          onBack={() => setPage('cart')}
          onOrderSuccess={(num) => {
            setOrderNumber(num);
            cart.clearCart();
            setPage('order-success');
          }}
        />
      );

    case 'order-success':
      return (
        <OrderSuccessPage
          orderNumber={orderNumber}
          onGoToMenu={() => setPage('menu')}
        />
      );

    case 'order-list':
      return (
        <OrderListPage
          onGoBack={() => setPage('menu')}
          onGoToSplitBill={(orders, sessionTotal) => {
            setSplitBillData({ orders, sessionTotal });
            setPage('split-bill');
          }}
        />
      );

    case 'split-bill':
      return splitBillData ? (
        <SplitBillPage
          orders={splitBillData.orders}
          sessionTotal={splitBillData.sessionTotal}
          onGoBack={() => setPage('order-list')}
        />
      ) : null;

    default:
      return null;
  }
}

export default App;
