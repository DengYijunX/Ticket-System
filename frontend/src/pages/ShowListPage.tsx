import { useEffect, useState } from 'react';
import { Card, Row, Col, Tag, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { getShowList } from '../api';

const { Title, Text } = Typography;

/** 演出卡片列表页 */
export default function ShowListPage() {
  const [shows, setShows] = useState<any[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    getShowList().then((res) => {
      if (res.data.code === 200) {
        setShows(res.data.data);
      }
    });
  }, []);

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px' }}>
      <Title level={2} style={{ marginBottom: 24 }}>🎵 正在热售</Title>
      <Row gutter={[24, 24]}>
        {shows.map((show) => (
          <Col key={show.id} xs={24} sm={12} md={8}>
            <Card
              hoverable
              onClick={() => navigate(`/show/${show.id}`)}
              cover={
                <div style={{
                  height: 180,
                  background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#fff',
                  fontSize: 48,
                }}>
                  🎤
                </div>
              }
            >
              <Card.Meta
                title={show.title}
                description={
                  <>
                    <Text type="secondary">{show.venue}</Text>
                    <br />
                    <Tag color="blue" style={{ marginTop: 8 }}>
                      即将开抢
                    </Tag>
                  </>
                }
              />
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
