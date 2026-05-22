import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Descriptions, Table, Button, Tag, message, Modal, InputNumber, Typography, Spin } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { getShowDetail, getSessions, getCategories, buyTicket } from '../api';

const { Title } = Typography;

/**
 * 演出详情页
 *
 * 展示演出信息、场次列表、票价档次，提供抢票按钮
 */
export default function ShowDetailPage() {
  const { id } = useParams();
  const [show, setShow] = useState<any>(null);
  const [sessions, setSessions] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [selectedSession, setSelectedSession] = useState<number | null>(null);
  const [buying, setBuying] = useState(false);
  const [quantity, setQuantity] = useState(1);

  useEffect(() => {
    if (!id) return;
    getShowDetail(Number(id)).then((r) => r.data.code === 200 && setShow(r.data.data));
    getSessions(Number(id)).then((r) => r.data.code === 200 && setSessions(r.data.data));
  }, [id]);

  // 点击场次 → 加载对应的票价档次
  const handleSessionClick = (sessionId: number) => {
    setSelectedSession(sessionId);
    getCategories(sessionId).then((r) => r.data.code === 200 && setCategories(r.data.data));
  };

  // 抢票
  const handleBuy = async (categoryId: number) => {
    const token = localStorage.getItem('token');
    if (!token) {
      message.warning('请先登录');
      window.location.href = '/login';
      return;
    }

    setBuying(true);
    try {
      const res = await buyTicket(selectedSession!, categoryId, quantity);
      if (res.data.code === 200) {
        message.success('抢票成功！订单处理中...');
      } else {
        message.error(res.data.message);
      }
    } catch {
      message.error('抢票失败，请重试');
    }
    setBuying(false);
  };

  if (!show) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: 24 }}>
      {/* 演出信息 */}
      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', gap: 24 }}>
          <div style={{
            width: 200, height: 200, borderRadius: 8,
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 64, color: '#fff', flexShrink: 0,
          }}>
            🎤
          </div>
          <div>
            <Title level={3}>{show.title}</Title>
            <Descriptions column={1}>
              <Descriptions.Item label="场馆">{show.venue}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color="blue">在售</Tag>
              </Descriptions.Item>
            </Descriptions>
          </div>
        </div>
      </Card>

      {/* 场次列表 */}
      <Title level={4}>选择场次</Title>
      <div style={{ display: 'flex', gap: 12, marginBottom: 24 }}>
        {sessions.map((s) => (
          <Card
            key={s.id}
            hoverable
            size="small"
            style={{
              width: 180,
              border: selectedSession === s.id ? '2px solid #1677ff' : undefined,
            }}
            onClick={() => handleSessionClick(s.id)}
          >
            <div style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 16, fontWeight: 'bold' }}>{s.name}</div>
              <div style={{ color: '#999', fontSize: 12 }}>点击选择</div>
            </div>
          </Card>
        ))}
      </div>

      {/* 票价档次 + 抢票 */}
      {selectedSession && (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
            <Title level={4} style={{ margin: 0 }}>选择票档</Title>
            <span>数量：</span>
            <InputNumber min={1} max={3} value={quantity} onChange={(v) => setQuantity(v || 1)} />
          </div>

          <Table
            dataSource={categories}
            rowKey="id"
            pagination={false}
            columns={[
              { title: '票档', dataIndex: 'name', key: 'name' },
              {
                title: '价格', dataIndex: 'price', key: 'price',
                render: (price: number) => <span style={{ color: '#f50', fontSize: 18, fontWeight: 'bold' }}>¥{price}</span>,
              },
              {
                title: '剩余', dataIndex: 'remainStock', key: 'remainStock',
                render: (stock: number) => (
                  <span style={{ color: stock > 50 ? '#52c41a' : stock > 10 ? '#faad14' : '#f50' }}>
                    {stock} 张
                  </span>
                ),
              },
              {
                title: '操作', key: 'action',
                render: (_: any, record: any) => (
                  <Button
                    type="primary"
                    icon={<ShoppingCartOutlined />}
                    loading={buying}
                    onClick={() => handleBuy(record.id)}
                    disabled={record.remainStock <= 0}
                  >
                    {record.remainStock > 0 ? '立即抢票' : '已售罄'}
                  </Button>
                ),
              },
            ]}
          />
        </>
      )}
    </div>
  );
}
