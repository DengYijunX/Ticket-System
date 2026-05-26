import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Button, message } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { getShowDetail, getSessions, getCategories, buyTicket } from '../api';

/**
 * 演出详情页
 *
 * 演出信息 → 场次选择 → 票价档次 → 抢票
 */
export default function ShowDetailPage() {
  const { id } = useParams();
  const [show, setShow] = useState<any>(null);
  const [sessions, setSessions] = useState<any[]>([]);
  const [categories, setCategories] = useState<any[]>([]);
  const [selectedSession, setSelectedSession] = useState<number | null>(null);
  const [buyingId, setBuyingId] = useState<number | null>(null);

  useEffect(() => {
    if (!id) return;
    getShowDetail(Number(id)).then((r) => r.data.code === 200 && setShow(r.data.data));
    getSessions(Number(id)).then((r) => r.data.code === 200 && setSessions(r.data.data));
  }, [id]);

  const handleSessionClick = (sessionId: number) => {
    setSelectedSession(sessionId);
    getCategories(sessionId).then((r) => r.data.code === 200 && setCategories(r.data.data));
  };

  const handleBuy = async (categoryId: number, remainStock: number) => {
    if (remainStock <= 0) { message.warning('已售罄'); return; }
    if (!localStorage.getItem('token')) {
      message.warning('请先登录');
      window.location.href = '/login';
      return;
    }
    setBuyingId(categoryId);
    try {
      const res = await buyTicket(selectedSession!, categoryId, 1);
      if (res.data.code === 200) {
        const orderNo = res.data.data;
        message.success({
          content: `抢票成功！订单号: ${orderNo}`,
          duration: 6,
        });
        // 刷新剩余库存
        getCategories(selectedSession!).then((r) => r.data.code === 200 && setCategories(r.data.data));
      } else {
        message.error(res.data.message);
      }
    } catch { message.error('抢票失败'); }
    setBuyingId(null);
  };

  // 覆盖率：剩余/总库存比例
  const stockPercent = (remain: number, total: number) =>
    total > 0 ? Math.round((remain / total) * 100) : 0;

  return (
    <div className="page">
      {/* 演出主信息 */}
      {show && (
        <div className="glow-card fade-up" style={{ marginBottom: 32, padding: 32, display: 'flex', gap: 32, alignItems: 'center' }}>
          <div style={{
            width: 140, height: 140, borderRadius: 16, flexShrink: 0,
            background: 'linear-gradient(135deg, #ff2d6b 0%, #ff6b35 100%)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 48, boxShadow: '0 8px 32px rgba(255,45,107,0.3)',
          }}>
            🎤
          </div>
          <div>
            <h1 style={{ fontFamily: "'Archivo Black', sans-serif", fontSize: 28, margin: 0 }}>
              {show.title}
            </h1>
            <div style={{ color: '#8888aa', marginTop: 8, fontSize: 14 }}>
              {show.venue}
            </div>
            {show.description && (
              <div style={{ color: '#555577', marginTop: 8, fontSize: 13 }}>
                {show.description}
              </div>
            )}
          </div>
        </div>
      )}

      {/* 场次选择 */}
      <div className="section-title fade-up fade-up-delay-1">
        <span className="accent" />
        选择场次
      </div>
      <div className="session-grid fade-up fade-up-delay-1">
        {sessions.map((s) => (
          <div
            key={s.id}
            className={`session-chip ${selectedSession === s.id ? 'active' : ''}`}
            onClick={() => handleSessionClick(s.id)}
          >
            <div className="session-chip-date">
              {new Date(s.startTime).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })}
            </div>
            <div className="session-chip-label">
              {new Date(s.startTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}
            </div>
          </div>
        ))}
      </div>

      {/* 票价档次 */}
      {selectedSession && (
        <>
          <div className="section-title fade-up fade-up-delay-2">
            <span className="accent" />
            选择票档
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {categories.map((cat, i) => {
              const pct = stockPercent(cat.remainStock, cat.totalStock);
              return (
                <div
                  key={cat.id}
                  className="glow-card fade-up"
                  style={{
                    padding: 24,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    animationDelay: `${i * 0.08}s`,
                  }}
                >
                  {/* 左侧：票档信息 */}
                  <div>
                    <div style={{ fontFamily: "'Archivo Black', sans-serif", fontSize: 16, marginBottom: 4 }}>
                      {cat.name}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className="price-tag">¥{cat.price}</span>
                    </div>
                  </div>

                  {/* 中间：库存条 */}
                  <div style={{ flex: 1, maxWidth: 200, margin: '0 32px' }}>
                    <div style={{
                      height: 4, background: 'rgba(255,255,255,0.06)', borderRadius: 2,
                      overflow: 'hidden', marginBottom: 6,
                    }}>
                      <div style={{
                        width: `${pct}%`, height: '100%',
                        background: pct > 50
                          ? 'linear-gradient(90deg, #52c41a, #73d13d)'
                          : pct > 10
                            ? 'linear-gradient(90deg, #faad14, #ffd700)'
                            : 'linear-gradient(90deg, #ff2d6b, #ff6b35)',
                        borderRadius: 2,
                        transition: 'width 0.5s ease',
                      }} />
                    </div>
                    <div style={{
                      display: 'flex', justifyContent: 'space-between',
                      fontSize: 12, color: '#555577',
                    }}>
                      <span>{cat.remainStock} 张剩余</span>
                      <span>{pct}%</span>
                    </div>
                  </div>

                  {/* 右侧：抢票按钮 */}
                  <Button
                    className="buy-btn"
                    icon={<ShoppingCartOutlined />}
                    loading={buyingId === cat.id}
                    disabled={cat.remainStock <= 0}
                    onClick={() => handleBuy(cat.id, cat.remainStock)}
                  >
                    {cat.remainStock > 0 ? '立即抢票' : '已售罄'}
                  </Button>
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
