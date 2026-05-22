import axios from 'axios';

// 后端 API 地址（开发环境）
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
});

// 请求拦截器：每次请求自动带上 token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器：token 过期时跳转登录
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

// ========== API 接口 ==========

/** 登录 */
export const login = (username: string, password: string) =>
  api.post('/user/login', { username, password });

/** 注册 */
export const register = (username: string, password: string, phone: string) =>
  api.post('/user/register', { username, password, phone });

/** 演出列表 */
export const getShowList = () => api.get('/show/list');

/** 演出详情 */
export const getShowDetail = (id: number) => api.get(`/show/${id}`);

/** 场次列表 */
export const getSessions = (showId: number) => api.get(`/show/${showId}/sessions`);

/** 票价档次 */
export const getCategories = (sessionId: number) =>
  api.get(`/show/session/${sessionId}/categories`);

/** 抢票 */
export const buyTicket = (sessionId: number, categoryId: number, quantity: number) =>
  api.post('/ticket/buy', { sessionId, categoryId, quantity });

/** 订单列表 */
export const getOrderList = () => api.get('/order/list');

/** 订单状态 */
export const getOrderStatus = (orderNo: string) =>
  api.get(`/order/status?orderNo=${orderNo}`);

export default api;
