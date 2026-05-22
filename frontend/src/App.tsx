import { BrowserRouter, Routes, Route, Link, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Button, Dropdown } from 'antd';
import { HomeOutlined, OrderedListOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons';
import LoginPage from './pages/LoginPage';
import ShowListPage from './pages/ShowListPage';
import ShowDetailPage from './pages/ShowDetailPage';
import OrderListPage from './pages/OrderListPage';

const { Header, Content, Footer } = Layout;

/**
 * 导航栏布局组件
 * 所有需要导航栏的页面都包在这个组件里
 */
function AppLayout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();
  const token = localStorage.getItem('token');

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const menuItems = [
    { key: '/', icon: <HomeOutlined />, label: <Link to="/">首页</Link> },
    { key: '/orders', icon: <OrderedListOutlined />, label: <Link to="/orders">我的订单</Link> },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', padding: '0 24px' }}>
        <div style={{ color: '#fff', fontSize: 20, fontWeight: 'bold', marginRight: 40 }}>
          🎫 票务系统
        </div>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[location.pathname]}
          items={menuItems}
          style={{ flex: 1 }}
        />
        {token ? (
          <Dropdown menu={{
            items: [{
              key: 'logout',
              icon: <LogoutOutlined />,
              label: '退出登录',
              onClick: handleLogout,
            }]
          }}>
            <Button type="text" style={{ color: '#fff' }}>
              <UserOutlined /> 用户
            </Button>
          </Dropdown>
        ) : (
          <Button type="link" style={{ color: '#fff' }} onClick={() => navigate('/login')}>
            登录
          </Button>
        )}
      </Header>
      <Content style={{ padding: 24 }}>
        {children}
      </Content>
      <Footer style={{ textAlign: 'center' }}>
        票务系统 ©2026
      </Footer>
    </Layout>
  );
}

/**
 * 路由配置
 *
 * /login      → 登录/注册（不带导航栏）
 * /           → 演出列表（首页）
 * /show/:id   → 演出详情 + 抢票
 * /orders     → 订单列表
 */
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<AppLayout><ShowListPage /></AppLayout>} />
        <Route path="/show/:id" element={<AppLayout><ShowDetailPage /></AppLayout>} />
        <Route path="/orders" element={<AppLayout><OrderListPage /></AppLayout>} />
      </Routes>
    </BrowserRouter>
  );
}
