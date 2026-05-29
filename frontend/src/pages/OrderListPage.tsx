import { useEffect, useState } from 'react';
import { Button, message, Tag } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { getOrderList, payOrder } from '../api';

const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已退款', color: 'red' },
  4: { text: '已完成', color: 'blue' },
};

export default function OrderListPage() {
  const [orders, setOrders] = useState<any[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  useEffect(() => {
    getOrderList().then((res) => {
      if (res.data.code === 200) setOrders(res.data.data);
    });
  }, []);

  const [payingId, setPayingId] = useState<number | null>(null);

  const copy = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('已复制');
  };

  const handlePay = async (orderNo: string, orderId: number) => {
    setPayingId(orderId);
    try {
      const res = await payOrder(orderNo);
      if (res.data.code === 200) {
        message.success('支付成功');
        getOrderList().then((r) => r.data.code === 200 && setOrders(r.data.data));
      } else {
        message.error(res.data.message);
      }
    } catch { message.error('支付失败'); }
    setPayingId(null);
  };

  return (
    <div className="page">
      <div className="section-title fade-up">
        <span className="accent" />
        我的订单
        <span style={{ fontSize: 12, color: '#555577', marginLeft: 12 }}>
          {orders.length} 条记录
        </span>
      </div>

      {orders.length === 0 ? (
        <div className="glow-card fade-up" style={{ padding: 48, textAlign: 'center', color: '#555577' }}>
          暂无订单，快去抢票吧
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {orders.map((order, i) => {
            const status = STATUS_MAP[order.status] || { text: '未知', color: 'default' };
            const expanded = expandedId === order.id;

            return (
              <div
                key={order.id}
                className="glow-card fade-up"
                style={{
                  padding: 24,
                  animationDelay: `${i * 0.06}s`,
                  cursor: 'pointer',
                  borderColor: expanded ? 'rgba(255,45,107,0.3)' : undefined,
                }}
                onClick={() => setExpandedId(expanded ? null : order.id)}
              >
                {/* 订单头部 */}
                <div style={{
                  display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div style={{
                      width: 40, height: 40, borderRadius: 10,
                      background: 'linear-gradient(135deg, rgba(255,45,107,0.2), rgba(124,58,237,0.2))',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      fontSize: 16,
                    }}>
                      🎫
                    </div>
                    <div>
                      <div style={{
                        display: 'flex', alignItems: 'center', gap: 8,
                        fontSize: 13, color: '#8888aa', fontFamily: 'monospace',
                      }}>
                        {order.orderNo?.slice(0, 18)}...
                        <CopyOutlined
                          style={{ fontSize: 12, color: '#555577', cursor: 'copy' }}
                          onClick={(e) => { e.stopPropagation(); copy(order.orderNo); }}
                        />
                      </div>
                      <div style={{ fontSize: 12, color: '#555577', marginTop: 2 }}>
                        {order.createTime?.replace('T', ' ')}
                      </div>
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <span className="price-tag">¥{order.totalAmount}</span>
                    <Tag color={status.color} style={{ borderRadius: 6, margin: 0 }}>
                      {status.text}
                    </Tag>
                  </div>
                </div>

                {/* 展开详情 */}
                {expanded && (
                  <div style={{
                    marginTop: 16, padding: 16,
                    background: 'rgba(255,255,255,0.02)', borderRadius: 10,
                    display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16,
                    fontSize: 13,
                  }}>
                    <div>
                      <div style={{ color: '#555577', marginBottom: 4 }}>订单号</div>
                      <div style={{ color: '#8888aa', fontFamily: 'monospace', fontSize: 12 }}>
                        {order.orderNo}
                      </div>
                    </div>
                    <div>
                      <div style={{ color: '#555577', marginBottom: 4 }}>数量</div>
                      <div style={{ color: '#ccc' }}>{order.quantity} 张</div>
                    </div>
                    <div>
                      <div style={{ color: '#555577', marginBottom: 4 }}>金额</div>
                      <div style={{ color: '#ff2d6b', fontFamily: "'Archivo Black', sans-serif" }}>
                        ¥{order.totalAmount}
                      </div>
                    </div>
                    <div>
                      <div style={{ color: '#555577', marginBottom: 4 }}>票档</div>
                      <div style={{ color: '#ccc' }}>{order.categoryName || `ID: ${order.categoryId}`}</div>
                    </div>
                    <div>
                      <div style={{ color: '#555577', marginBottom: 4 }}>状态</div>
                      <Tag color={status.color} style={{ borderRadius: 6 }}>
                        {status.text}
                      </Tag>
                    </div>
                    <div>
                      <div style={{ color: '#555577', marginBottom: 4 }}>下单时间</div>
                      <div style={{ color: '#8888aa', fontSize: 12 }}>
                        {order.createTime?.replace('T', ' ')}
                      </div>
                    </div>
                    {order.status === 0 && (
                      <div style={{ gridColumn: '1 / -1', marginTop: 8 }}>
                        <Button
                          type="primary"
                          size="small"
                          loading={payingId === order.id}
                          onClick={(e) => { e.stopPropagation(); handlePay(order.orderNo, order.id); }}
                          style={{ background: '#52c41a', border: 'none' }}
                        >
                          模拟支付
                        </Button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
