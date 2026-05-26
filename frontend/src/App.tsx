import { BrowserRouter, Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Button, Dropdown } from 'antd';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import LoginPage from './pages/LoginPage';
import ShowListPage from './pages/ShowListPage';
import ShowDetailPage from './pages/ShowDetailPage';
import OrderListPage from './pages/OrderListPage';
import AdminPage from './pages/AdminPage';

const { Content } = Layout;

/** 导航栏布局 */
function AppLayout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const token = localStorage.getItem('token');

  // 解码 JWT 判断管理员
  let isAdmin = false;
  if (token) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      isAdmin = payload.role === 1;
    } catch {}
  }

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh', background: '#0a0a0f' }}>
      {/* 导航栏 */}
      <header className="nav-header">
        <div className="nav-logo" onClick={() => navigate('/')} style={{ cursor: 'pointer' }}>
          <span className="highlight">TICKET</span>X
        </div>

        <nav style={{ flex: 1, display: 'flex', gap: 8 }}>
          <Button
            type="text"
            onClick={() => navigate('/')}
            style={{
              color: location.pathname === '/' ? '#ff2d6b' : '#8888aa',
              fontSize: 14,
              fontWeight: location.pathname === '/' ? 600 : 400,
            }}
          >
            演出
          </Button>
          <Button
            type="text"
            onClick={() => navigate('/orders')}
            style={{
              color: location.pathname === '/orders' ? '#ff2d6b' : '#8888aa',
              fontSize: 14,
              fontWeight: location.pathname === '/orders' ? 600 : 400,
            }}
          >
            我的订单
          </Button>
          {isAdmin && (
            <Button
              type="text"
              onClick={() => navigate('/admin')}
              style={{
                color: location.pathname === '/admin' ? '#ff2d6b' : '#8888aa',
                fontSize: 14,
                fontWeight: location.pathname === '/admin' ? 600 : 400,
              }}
            >
              管理
            </Button>
          )}
        </nav>

        {token ? (
          <Dropdown menu={{
            items: [{
              key: 'logout',
              icon: <LogoutOutlined />,
              label: '退出登录',
              onClick: handleLogout,
            }]
          }}>
            <Button
              type="text"
              icon={<UserOutlined />}
              style={{ color: '#8888aa', borderRadius: 8 }}
            >
              用户
            </Button>
          </Dropdown>
        ) : (
          <Button
            onClick={() => navigate('/login')}
            style={{
              background: 'transparent',
              border: '1px solid rgba(255,45,107,0.3)',
              color: '#ff2d6b',
              borderRadius: 8,
            }}
          >
            登录
          </Button>
        )}
      </header>

      <Content style={{ minHeight: 'calc(100vh - 64px)' }}>
        {children}
      </Content>

      <footer style={{
        textAlign: 'center',
        padding: '24px',
        color: '#555577',
        fontSize: 12,
        borderTop: '1px solid rgba(255,255,255,0.04)',
      }}>
        TICKETX · 票务系统 ©2026
      </footer>
    </Layout>
  );
}

/** 路由 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<AppLayout><ShowListPage /></AppLayout>} />
        <Route path="/show/:id" element={<AppLayout><ShowDetailPage /></AppLayout>} />
        <Route path="/orders" element={<AppLayout><OrderListPage /></AppLayout>} />
        <Route path="/admin" element={<AppLayout><AdminPage /></AppLayout>} />
      </Routes>
    </BrowserRouter>
  );
}
