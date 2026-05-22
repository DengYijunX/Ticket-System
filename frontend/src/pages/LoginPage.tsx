import { useState } from 'react';
import { Form, Input, Button, Card, message, Tabs } from 'antd';
import { UserOutlined, LockOutlined, PhoneOutlined } from '@ant-design/icons';
import { login, register } from '../api';
import { useNavigate } from 'react-router-dom';

/**
 * 登录/注册页面
 *
 * 两个模式：登录 / 注册，通过 Tabs 切换
 */
export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // 处理登录
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
    } catch {
      message.error('登录失败');
    }
    setLoading(false);
  };

  // 处理注册
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
    } catch {
      message.error('注册失败');
    }
    setLoading(false);
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <Card style={{ width: 400, borderRadius: 8 }}>
        <h2 style={{ textAlign: 'center', marginBottom: 24 }}>票务系统</h2>
        <Tabs
          centered
          items={[
            {
              key: 'login',
              label: '登录',
              children: (
                <Form onFinish={handleLogin} layout="vertical">
                  <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                    <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
                  </Form.Item>
                  <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                    <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block size="large">
                    登录
                  </Button>
                </Form>
              ),
            },
            {
              key: 'register',
              label: '注册',
              children: (
                <Form onFinish={handleRegister} layout="vertical">
                  <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                    <Input prefix={<UserOutlined />} placeholder="用户名" size="large" />
                  </Form.Item>
                  <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                    <Input.Password prefix={<LockOutlined />} placeholder="密码" size="large" />
                  </Form.Item>
                  <Form.Item name="phone" rules={[{ required: true, message: '请输入手机号' }]}>
                    <Input prefix={<PhoneOutlined />} placeholder="手机号" size="large" />
                  </Form.Item>
                  <Button type="primary" htmlType="submit" loading={loading} block size="large">
                    注册
                  </Button>
                </Form>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}
