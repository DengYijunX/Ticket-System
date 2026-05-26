import { useState } from 'react';
import { Form, Input, Button, message } from 'antd';
import { UserOutlined, LockOutlined, PhoneOutlined } from '@ant-design/icons';
import { login, register } from '../api';
import { useNavigate } from 'react-router-dom';

/**
 * 登录/注册页面
 *
 * 暗色背景 + 霓虹灯光圈 + 极简表单
 */
export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [isLogin, setIsLogin] = useState(true);
  const navigate = useNavigate();

  const handleLogin = async (values: { username: string; password: string }) => {
    setLoading(true);
    try {
      const res = await login(values.username, values.password);
      if (res.data.code === 200) {
        localStorage.setItem('token', res.data.data);
        message.success('登录成功');
        navigate('/');
      } else {
        message.error(res.data.message);
      }
    } catch { message.error('登录失败'); }
    setLoading(false);
  };

  const handleRegister = async (values: { username: string; password: string; phone: string }) => {
    setLoading(true);
    try {
      const res = await register(values.username, values.password, values.phone);
      if (res.data.code === 200) {
        localStorage.setItem('token', res.data.data);
        message.success('注册成功');
        navigate('/');
      } else {
        message.error(res.data.message);
      }
    } catch { message.error('注册失败'); }
    setLoading(false);
  };

  return (
    <div className="login-container">
      <div className="login-card fade-up">
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{ fontSize: 12, color: '#555577', letterSpacing: 4, marginBottom: 8 }}>
            TICKETX
          </div>
          <h1>
            <span className="highlight">{isLogin ? '登' : '注'}</span>录
          </h1>
        </div>

        {isLogin ? (
          <Form onFinish={handleLogin} layout="vertical" size="large">
            <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input
                prefix={<UserOutlined style={{ color: '#555577' }} />}
                placeholder="用户名"
                variant="borderless"
                style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 8, color: '#fff' }}
              />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password
                prefix={<LockOutlined style={{ color: '#555577' }} />}
                placeholder="密码"
                variant="borderless"
                style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 8 }}
              />
            </Form.Item>
            <Button
              htmlType="submit"
              loading={loading}
              block
              className="buy-btn"
              style={{ height: 44, fontSize: 14 }}
            >
              登录
            </Button>
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Button type="link" onClick={() => setIsLogin(false)} style={{ color: '#555577' }}>
                没有账号？去注册
              </Button>
            </div>
          </Form>
        ) : (
          <Form onFinish={handleRegister} layout="vertical" size="large">
            <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
              <Input
                prefix={<UserOutlined style={{ color: '#555577' }} />}
                placeholder="用户名"
                variant="borderless"
                style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 8, color: '#fff' }}
              />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
              <Input.Password
                prefix={<LockOutlined style={{ color: '#555577' }} />}
                placeholder="密码"
                variant="borderless"
                style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 8 }}
              />
            </Form.Item>
            <Form.Item name="phone" rules={[{ required: true, message: '请输入手机号' }]}>
              <Input
                prefix={<PhoneOutlined style={{ color: '#555577' }} />}
                placeholder="手机号"
                variant="borderless"
                style={{ background: 'rgba(255,255,255,0.04)', borderRadius: 8, color: '#fff' }}
              />
            </Form.Item>
            <Button
              htmlType="submit"
              loading={loading}
              block
              className="buy-btn"
              style={{ height: 44, fontSize: 14 }}
            >
              注册
            </Button>
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Button type="link" onClick={() => setIsLogin(true)} style={{ color: '#555577' }}>
                已有账号？去登录
              </Button>
            </div>
          </Form>
        )}
      </div>
    </div>
  );
}
