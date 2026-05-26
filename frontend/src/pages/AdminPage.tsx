import { useEffect, useState } from 'react';
import { Button, message, Modal, Input, Popconfirm, Table, Tag, Card, Statistic } from 'antd';
import { PlusOutlined, DeleteOutlined, DollarOutlined } from '@ant-design/icons';
import {
  getDashboard, adminGetShows, adminCreateShow,
  adminDeleteShow, adminGetOrders, adminCancelOrder,
} from '../api';

const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已退款', color: 'red' },
  4: { text: '已完成', color: 'blue' },
};

export default function AdminPage() {
  const [tab, setTab] = useState<'dashboard' | 'shows' | 'orders'>('dashboard');
  const [dashboard, setDashboard] = useState<any>({});
  const [shows, setShows] = useState<any[]>([]);
  const [orders, setOrders] = useState<any[]>([]);
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    if (tab === 'dashboard') getDashboard().then(r => r.data.code === 200 && setDashboard(r.data.data));
    if (tab === 'shows') adminGetShows().then(r => r.data.code === 200 && setShows(r.data.data));
    if (tab === 'orders') adminGetOrders().then(r => r.data.code === 200 && setOrders(r.data.data));
  }, [tab]);

  const handleCreateShow = async (values: any) => {
    const res = await adminCreateShow(values);
    if (res.data.code === 200) { message.success('创建成功'); setModalOpen(false); setTab('shows'); }
    else message.error(res.data.message);
  };

  const handleCancelOrder = async (id: number) => {
    const res = await adminCancelOrder(id);
    if (res.data.code === 200) { message.success('已退款'); setTab('orders'); }
    else message.error(res.data.message);
  };

  // ========== 新建演出表单 ==========
  const [formData, setFormData] = useState<any>({
    title: '', description: '', venue: '',
    sessions: [{ name: '', startTime: '', categories: [{ name: '', price: 0, totalStock: 0 }] }],
  });

  return (
    <div className="page">
      <div className="section-title fade-up">
        <span className="accent" />
        管理后台
      </div>

      {/* Tab 导航 */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {([
          ['dashboard', '数据看板'],
          ['shows', '演出管理'],
          ['orders', '订单管理'],
        ] as const).map(([key, label]) => (
          <Button
            key={key}
            type={tab === key ? 'primary' : 'default'}
            onClick={() => setTab(key)}
            style={{
              borderRadius: 8,
              background: tab === key ? 'linear-gradient(135deg, #ff2d6b, #ff6b35)' : 'rgba(255,255,255,0.04)',
              border: tab === key ? 'none' : '1px solid rgba(255,255,255,0.06)',
              color: tab === key ? '#fff' : '#8888aa',
            }}
          >
            {label}
          </Button>
        ))}
      </div>

      {/* ===== 数据看板 ===== */}
      {tab === 'dashboard' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
          <Card className="glow-card" style={{ background: 'rgba(255,255,255,0.02)' }}>
            <Statistic title="用户总数" value={dashboard.totalUsers || 0}
              valueStyle={{ color: '#ff2d6b', fontFamily: "'Archivo Black', sans-serif" }} />
          </Card>
          <Card className="glow-card" style={{ background: 'rgba(255,255,255,0.02)' }}>
            <Statistic title="演出总数" value={dashboard.totalShows || 0}
              valueStyle={{ color: '#00d4ff', fontFamily: "'Archivo Black', sans-serif" }} />
          </Card>
          <Card className="glow-card" style={{ background: 'rgba(255,255,255,0.02)' }}>
            <Statistic title="订单总数" value={dashboard.totalOrders || 0}
              valueStyle={{ color: '#ffd700', fontFamily: "'Archivo Black', sans-serif" }} />
          </Card>
          <Card className="glow-card" style={{ background: 'rgba(255,255,255,0.02)' }}>
            <Statistic title="今日订单" value={dashboard.todayOrders || 0}
              valueStyle={{ color: '#52c41a', fontFamily: "'Archivo Black', sans-serif" }} />
          </Card>
          <Card className="glow-card" style={{ background: 'rgba(255,255,255,0.02)' }}>
            <Statistic title="总收入" value={dashboard.totalRevenue || 0} prefix="¥"
              valueStyle={{ color: '#ffd700', fontFamily: "'Archivo Black', sans-serif" }} />
          </Card>
        </div>
      )}

      {/* ===== 演出管理 ===== */}
      {tab === 'shows' && (
        <>
          <Button icon={<PlusOutlined />} onClick={() => setModalOpen(true)}
            style={{ marginBottom: 16, borderRadius: 8 }}>
            新建演出
          </Button>
          <Table
            dataSource={shows} rowKey="id"
            className="glow-card"
            style={{ background: 'rgba(255,255,255,0.02)' }}
            columns={[
              { title: 'ID', dataIndex: 'id', width: 60 },
              { title: '标题', dataIndex: 'title' },
              { title: '场馆', dataIndex: 'venue' },
              { title: '创建时间', dataIndex: 'createTime', render: (t: string) => t?.replace('T', ' ') },
              {
                title: '操作', render: (_, record) => (
                  <Popconfirm title="确认删除？" onConfirm={() => {
                    adminDeleteShow(record.id).then(() => setTab('shows'));
                  }}>
                    <Button danger icon={<DeleteOutlined />} size="small">删除</Button>
                  </Popconfirm>
                ),
              },
            ]}
          />

          {/* 新建演出弹窗 */}
          <Modal title="新建演出" open={modalOpen} onCancel={() => setModalOpen(false)}
            footer={null} width={600}>
            <Input placeholder="演出标题" style={{ marginBottom: 12 }}
              value={formData.title}
              onChange={e => setFormData({ ...formData, title: e.target.value })} />
            <Input placeholder="描述" style={{ marginBottom: 12 }}
              value={formData.description}
              onChange={e => setFormData({ ...formData, description: e.target.value })} />
            <Input placeholder="场馆" style={{ marginBottom: 12 }}
              value={formData.venue}
              onChange={e => setFormData({ ...formData, venue: e.target.value })} />
            <div style={{ marginBottom: 8, color: '#8888aa' }}>场次 + 票档</div>
            {formData.sessions.map((s: any, si: number) => (
              <div key={si} style={{ marginBottom: 12, padding: 12, background: 'rgba(255,255,255,0.03)', borderRadius: 8 }}>
                <Input placeholder="场次名称" value={s.name}
                  onChange={e => {
                    const ns = [...formData.sessions];
                    ns[si].name = e.target.value;
                    setFormData({ ...formData, sessions: ns });
                  }} style={{ marginBottom: 8 }} />
                <Input placeholder="开始时间 (2026-06-15T19:30:00)" value={s.startTime}
                  onChange={e => {
                    const ns = [...formData.sessions];
                    ns[si].startTime = e.target.value;
                    setFormData({ ...formData, sessions: ns });
                  }} style={{ marginBottom: 8 }} />
                {s.categories.map((c: any, ci: number) => (
                  <div key={ci} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
                    <Input placeholder="票档名" value={c.name} style={{ flex: 2 }}
                      onChange={e => {
                        const ns = [...formData.sessions];
                        ns[si].categories[ci].name = e.target.value;
                        setFormData({ ...formData, sessions: ns });
                      }} />
                    <Input placeholder="价格" type="number" value={c.price} style={{ flex: 1 }}
                      onChange={e => {
                        const ns = [...formData.sessions];
                        ns[si].categories[ci].price = Number(e.target.value);
                        setFormData({ ...formData, sessions: ns });
                      }} />
                    <Input placeholder="库存" type="number" value={c.totalStock} style={{ flex: 1 }}
                      onChange={e => {
                        const ns = [...formData.sessions];
                        ns[si].categories[ci].totalStock = Number(e.target.value);
                        setFormData({ ...formData, sessions: ns });
                      }} />
                  </div>
                ))}
                <Button size="small" onClick={() => {
                  const ns = [...formData.sessions];
                  ns[si].categories.push({ name: '', price: 0, totalStock: 0 });
                  setFormData({ ...formData, sessions: ns });
                }}>+ 添加票档</Button>
              </div>
            ))}
            <Button onClick={() => setFormData({
              ...formData,
              sessions: [...formData.sessions, { name: '', startTime: '', categories: [{ name: '', price: 0, totalStock: 0 }] }],
            })}>+ 添加场次</Button>
            <div style={{ marginTop: 16 }}>
              <Button block type="primary" onClick={() => handleCreateShow(formData)}
                style={{ borderRadius: 8, background: 'linear-gradient(135deg, #ff2d6b, #ff6b35)', border: 'none', height: 40 }}>
                创建演出
              </Button>
            </div>
          </Modal>
        </>
      )}

      {/* ===== 订单管理 ===== */}
      {tab === 'orders' && (
        <Table
          dataSource={orders} rowKey="id"
          className="glow-card"
          style={{ background: 'rgba(255,255,255,0.02)' }}
          columns={[
            { title: '订单号', dataIndex: 'orderNo', width: 200, render: (v: string) => v?.slice(0, 18) },
            { title: '用户ID', dataIndex: 'userId', width: 80 },
            { title: '金额', dataIndex: 'totalAmount', render: (v: number) => <span style={{ color: '#ff2d6b' }}>¥{v}</span> },
            {
              title: '状态', dataIndex: 'status', render: (s: number) => {
                const st = STATUS_MAP[s] || { text: '未知', color: 'default' };
                return <Tag color={st.color}>{st.text}</Tag>;
              },
            },
            { title: '时间', dataIndex: 'createTime', render: (t: string) => t?.replace('T', ' ') },
            {
              title: '操作', render: (_, record) =>
                record.status === 0 ? (
                  <Popconfirm title="确认取消并退款？" onConfirm={() => handleCancelOrder(record.id)}>
                    <Button icon={<DollarOutlined />} size="small">退款</Button>
                  </Popconfirm>
                ) : null,
            },
          ]}
        />
      )}
    </div>
  );
}
