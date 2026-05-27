import { useEffect, useState } from 'react';
import { Button, message, Modal, Input, Popconfirm, Table, Tag, Card, Statistic, Space } from 'antd';
import { PlusOutlined, DeleteOutlined, DollarOutlined, ExpandOutlined, CompressOutlined } from '@ant-design/icons';
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

const TICKET_ICONS = ['🎤', '🎸', '🎹', '🎪', '🎭', '🎫'];

export default function AdminPage() {
  const [tab, setTab] = useState<'dashboard' | 'shows' | 'orders'>('dashboard');
  const [dashboard, setDashboard] = useState<any>({});
  const [shows, setShows] = useState<any[]>([]);
  const [orders, setOrders] = useState<any[]>([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [expandedShow, setExpandedShow] = useState<number | null>(null);
  const [formData, setFormData] = useState<any>({
    title: '', description: '', venue: '',
    sessions: [{ name: '', startTime: '', categories: [{ name: '', price: 0, totalStock: 0 }] }],
  });

  useEffect(() => {
    if (tab === 'dashboard') getDashboard().then(r => { if (r.data.code === 200) setDashboard(r.data.data); });
    if (tab === 'shows') adminGetShows().then(r => { if (r.data.code === 200) setShows(r.data.data); });
    if (tab === 'orders') adminGetOrders().then(r => { if (r.data.code === 200) setOrders(r.data.data); });
  }, [tab]);

  const refreshShows = () => adminGetShows().then(r => { if (r.data.code === 200) setShows(r.data.data); });

  const handleCreateShow = async () => {
    const res = await adminCreateShow(formData);
    if (res.data.code === 200) {
      message.success('创建成功！演出已上架，库存已写入 Redis');
      setModalOpen(false);
      setFormData({
        title: '', description: '', venue: '',
        sessions: [{ name: '', startTime: '', categories: [{ name: '', price: 0, totalStock: 0 }] }],
      });
      refreshShows();
    } else message.error(res.data.message);
  };

  const handleCancelOrder = async (id: number) => {
    const res = await adminCancelOrder(id);
    if (res.data.code === 200) { message.success('已退款'); setTab('orders'); }
    else message.error(res.data.message);
  };

  // ========== 渲染 ==========

  return (
    <div className="page">
      <div className="section-title fade-up">
        <span className="accent" />
        管理后台
      </div>

      {/* Tab 导航 */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 24 }}>
        {([
          ['dashboard', '📊 数据看板'],
          ['shows', '🎫 演出管理'],
          ['orders', '📋 订单管理'],
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
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 16 }}>
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

      {/* ===== 演出管理 — 卡片形式 ===== */}
      {tab === 'shows' && (
        <>
          <div style={{ display: 'flex', gap: 8, marginBottom: 20, alignItems: 'center' }}>
            <Button icon={<PlusOutlined />} onClick={() => setModalOpen(true)}
              style={{ borderRadius: 8, background: 'linear-gradient(135deg, #ff2d6b, #ff6b35)', border: 'none', color: '#fff', height: 36 }}>
              新建演出
            </Button>
            <span style={{ color: '#555577', fontSize: 12 }}>{shows.length} 场演出</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {shows.map((show, i) => {
              const expanded = expandedShow === show.id;
              return (
                <div
                  key={show.id}
                  className="glow-card fade-up"
                  style={{ padding: 0, overflow: 'hidden', animationDelay: `${i * 0.05}s` }}
                >
                  {/* 卡片头部 */}
                  <div style={{
                    padding: '20px 24px',
                    display: 'flex', alignItems: 'center', gap: 16,
                    background: 'rgba(255,255,255,0.02)',
                    cursor: 'pointer',
                  }} onClick={() => setExpandedShow(expanded ? null : show.id)}>
                    <div style={{
                      width: 48, height: 48, borderRadius: 12, flexShrink: 0,
                      background: `linear-gradient(135deg, ${['#ff2d6b,#ff6b35', '#00d4ff,#7c3aed', '#ffd700,#ff6b35', '#7c3aed,#00d4ff'][i % 4]})`,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 20,
                    }}>
                      {TICKET_ICONS[i % TICKET_ICONS.length]}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 16, fontWeight: 600, color: '#fff', fontFamily: "'Archivo Black', sans-serif" }}>
                        {show.title}
                      </div>
                      <div style={{ fontSize: 12, color: '#8888aa', marginTop: 2 }}>
                        {show.venue} · ID: {show.id} · {show.createTime?.replace('T', ' ')}
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <Tag color="green" style={{ borderRadius: 6 }}>在售</Tag>
                      {expanded ? <CompressOutlined style={{ color: '#8888aa' }} /> : <ExpandOutlined style={{ color: '#8888aa' }} />}
                    </div>
                  </div>

                  {/* 展开：场次 & 票档详情 */}
                  {expanded && (
                    <div style={{ padding: '12px 24px 24px', borderTop: '1px solid rgba(255,255,255,0.04)' }}>
                      <div style={{ color: '#8888aa', fontSize: 13, marginBottom: 8 }}>场次 & 票价</div>
                      {show.sessions?.length > 0 ? show.sessions.map((s: any) => (
                        <div key={s.id} style={{
                          marginBottom: 10, padding: 12, borderRadius: 8,
                          background: 'rgba(255,255,255,0.02)',
                        }}>
                          <div style={{ fontSize: 13, color: '#ccc', marginBottom: 6 }}>
                            {s.name} · {s.startTime?.replace('T', ' ')}
                          </div>
                          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            {s.categories?.map((c: any) => (
                              <span key={c.id} style={{
                                padding: '4px 10px', borderRadius: 6, fontSize: 12,
                                background: 'rgba(255,45,107,0.08)',
                                color: '#ff2d6b', fontFamily: 'monospace',
                              }}>
                                {c.name} ¥{c.price} | 库存 {c.remainStock}/{c.totalStock}
                              </span>
                            ))}
                          </div>
                        </div>
                      )) : (
                        <div style={{ color: '#555577', fontSize: 12 }}>
                          点击卡片展开查看详情（需通过 API 查询场次数据）
                        </div>
                      )}
                      <div style={{ marginTop: 12 }}>
                        <Popconfirm title="确认删除此演出？同时删除所有场次和票档" onConfirm={() => {
                          adminDeleteShow(show.id).then(() => refreshShows());
                        }}>
                          <Button danger icon={<DeleteOutlined />} size="small">删除演出</Button>
                        </Popconfirm>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {/* 新建演出弹窗 */}
          <Modal title="新建演出" open={modalOpen} onCancel={() => setModalOpen(false)}
            footer={null} width={640} destroyOnClose>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <Input placeholder="演出标题（如：薛之谦2026巡演-上海站）" size="large"
                value={formData.title}
                onChange={e => setFormData({ ...formData, title: e.target.value })} />
              <Input placeholder="简短描述（如：天外来物）"
                value={formData.description}
                onChange={e => setFormData({ ...formData, description: e.target.value })} />
              <Input placeholder="场馆（如：上海梅赛德斯奔驰文化中心）"
                value={formData.venue}
                onChange={e => setFormData({ ...formData, venue: e.target.value })} />

              <div style={{ color: '#8888aa', fontSize: 13, fontWeight: 600, marginTop: 4 }}>场次 & 票档</div>
              {formData.sessions.map((s: any, si: number) => (
                <div key={si} style={{
                  padding: 14, borderRadius: 10,
                  background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)',
                }}>
                  <Space style={{ width: '100%', marginBottom: 8 }}>
                    <Input placeholder="场次名称（如：2026-06-15 19:30场）" value={s.name} style={{ flex: 2 }}
                      onChange={e => {
                        const ns = [...formData.sessions];
                        ns[si].name = e.target.value;
                        setFormData({ ...formData, sessions: ns });
                      }} />
                    <Input placeholder="2026-06-15T19:30:00" value={s.startTime} style={{ flex: 1 }}
                      onChange={e => {
                        const ns = [...formData.sessions];
                        ns[si].startTime = e.target.value;
                        setFormData({ ...formData, sessions: ns });
                      }} />
                  </Space>
                  {s.categories.map((c: any, ci: number) => (
                    <Space key={ci} style={{ marginBottom: 6 }}>
                      <Input placeholder="票档名" value={c.name}
                        onChange={e => {
                          const ns = [...formData.sessions];
                          ns[si].categories[ci].name = e.target.value;
                          setFormData({ ...formData, sessions: ns });
                        }} />
                      <Input placeholder="价格" type="number" value={c.price || ''} style={{ width: 100 }}
                        onChange={e => {
                          const ns = [...formData.sessions];
                          ns[si].categories[ci].price = Number(e.target.value);
                          setFormData({ ...formData, sessions: ns });
                        }} />
                      <Input placeholder="库存" type="number" value={c.totalStock || ''} style={{ width: 90 }}
                        onChange={e => {
                          const ns = [...formData.sessions];
                          ns[si].categories[ci].totalStock = Number(e.target.value);
                          setFormData({ ...formData, sessions: ns });
                        }} />
                      <Button type="text" size="small" danger onClick={() => {
                        const ns = [...formData.sessions];
                        ns[si].categories.splice(ci, 1);
                        setFormData({ ...formData, sessions: ns });
                      }}>✕</Button>
                    </Space>
                  ))}
                  <Space>
                    <Button size="small" onClick={() => {
                      const ns = [...formData.sessions];
                      ns[si].categories.push({ name: '', price: 0, totalStock: 0 });
                      setFormData({ ...formData, sessions: ns });
                    }}>+ 票档</Button>
                    <Button size="small" danger onClick={() => {
                      const ns = [...formData.sessions];
                      ns.splice(si, 1);
                      setFormData({ ...formData, sessions: ns.length > 0 ? ns : [{ name: '', startTime: '', categories: [{ name: '', price: 0, totalStock: 0 }] }] });
                    }}>删除场次</Button>
                  </Space>
                </div>
              ))}
              <Button onClick={() => setFormData({
                ...formData,
                sessions: [...formData.sessions, { name: '', startTime: '', categories: [{ name: '', price: 0, totalStock: 0 }] }],
              })} block>+ 添加场次</Button>
              <Button block onClick={handleCreateShow}
                style={{
                  marginTop: 8, height: 42, borderRadius: 10, fontSize: 15, fontWeight: 600,
                  background: 'linear-gradient(135deg, #ff2d6b, #ff6b35)', border: 'none', color: '#fff',
                }}>
                创建演出并上架
              </Button>
            </div>
          </Modal>
        </>
      )}

      {/* ===== 订单管理 ===== */}
      {tab === 'orders' && (
        <Table
          dataSource={orders} rowKey="id"
          className="glow-card" size="small"
          style={{ background: 'rgba(255,255,255,0.02)' }}
          columns={[
            { title: '订单号', dataIndex: 'orderNo', width: 180, render: (v: string) => v?.slice(0, 18) + '...' },
            { title: '用户', dataIndex: 'userId', width: 70 },
            { title: '金额', dataIndex: 'totalAmount', width: 80,
              render: (v: number) => <span style={{ color: '#ff2d6b', fontWeight: 600 }}>¥{v}</span> },
            { title: '状态', dataIndex: 'status', width: 80,
              render: (s: number) => {
                const st = STATUS_MAP[s] || { text: '未知', color: 'default' };
                return <Tag color={st.color}>{st.text}</Tag>;
              } },
            { title: '时间', dataIndex: 'createTime', render: (t: string) => t?.replace('T', ' ') },
            { title: '操作', width: 100,
              render: (_, record) =>
                record.status === 0 ? (
                  <Popconfirm title="确认取消并退款？" onConfirm={() => handleCancelOrder(record.id)}>
                    <Button icon={<DollarOutlined />} size="small">退款</Button>
                  </Popconfirm>
                ) : <span style={{ color: '#555577', fontSize: 12 }}>-</span>,
            },
          ]}
        />
      )}
    </div>
  );
}
