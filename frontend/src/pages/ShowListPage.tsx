import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getShowList } from '../api';

/** 演出列表 — 暗色舞台风格卡片 */
export default function ShowListPage() {
  const [shows, setShows] = useState<any[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    getShowList().then((res) => {
      if (res.data.code === 200) setShows(res.data.data);
    });
  }, []);

  // 封面配色方案
  const gradients = [
    'linear-gradient(135deg, #ff2d6b 0%, #ff6b35 100%)',
    'linear-gradient(135deg, #00d4ff 0%, #7c3aed 100%)',
    'linear-gradient(135deg, #ffd700 0%, #ff6b35 100%)',
    'linear-gradient(135deg, #7c3aed 0%, #00d4ff 100%)',
  ];

  return (
    <div className="page">
      <div className="fade-up">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 32 }}>
          <div>
            <div className="page-title">正在热售</div>
            <div style={{ color: '#555577', fontSize: 14 }}>
              {shows.length} 场演出即将开抢
            </div>
          </div>
          <div style={{ color: '#555577', fontSize: 12 }}>
            {new Date().toLocaleDateString('zh-CN')}
          </div>
        </div>
      </div>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
        gap: 24,
      }}>
        {shows.map((show, i) => {
          const gradient = gradients[i % gradients.length];
          const emojis = ['🎤', '🎸', '🎹', '🎪'];
          return (
            <div
              key={show.id}
              className="show-card fade-up"
              style={{ animationDelay: `${i * 0.1}s` }}
              onClick={() => navigate(`/show/${show.id}`)}
            >
              {/* 封面 */}
              <div className="show-card-cover" style={{ background: gradient }}>
                <span style={{ filter: 'drop-shadow(0 4px 20px rgba(0,0,0,0.3))' }}>
                  {emojis[i % emojis.length]}
                </span>
              </div>

              {/* 内容 */}
              <div className="show-card-body">
                <div className="show-card-title">{show.title}</div>
                <div className="show-card-venue">{show.venue}</div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  fontSize: 12,
                  color: '#ff2d6b',
                }}>
                  <span style={{
                    width: 6, height: 6, borderRadius: '50%',
                    background: '#ff2d6b', display: 'inline-block',
                  }} />
                  即将开抢
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
