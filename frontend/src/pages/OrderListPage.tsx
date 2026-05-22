import { useEffect, useState } from 'react';
import { Table, Tag, Typography, Card } from 'antd';
import { getOrderList } from '../api';

const { Title } = Typography;

/** 订单状态映射 */
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已退款', color: 'red' },
  4: { text: '已完成', color: 'blue' },
};

/** 订单列表页 */
export default function OrderListPage() {
  const [orders, setOrders] = useState<any[]>([]);

  useEffect(() => {
    getOrderList().then((res) => {
      if (res.data.code === 200) {
        setOrders(res.data.data);
      }
    });
  }, []);

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: 24 }}>
      <Title level={2} style={{ marginBottom: 24 }}>📋 我的订单</Title>

      <Card>
        <Table
          dataSource={orders}
          rowKey="id"
          pagination={false}
          columns={[
            {
              title: '订单号', dataIndex: 'orderNo', key: 'orderNo',
              width: 200,
            },
            {
              title: '数量', dataIndex: 'quantity', key: 'quantity',
            },
            {
              title: '金额', dataIndex: 'totalAmount', key: 'totalAmount',
              render: (amount: number) => <span style={{ color: '#f50' }}>¥{amount}</span>,
            },
            {
              title: '状态', dataIndex: 'status', key: 'status',
              render: (status: number) => {
                const s = STATUS_MAP[status] || { text: '未知', color: 'default' };
                return <Tag color={s.color}>{s.text}</Tag>;
              },
            },
            {
              title: '下单时间', dataIndex: 'createTime', key: 'createTime',
              render: (t: string) => t?.replace('T', ' '),
            },
          ]}
        />
      </Card>
    </div>
  );
}
