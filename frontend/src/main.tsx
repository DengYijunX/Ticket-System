import { createRoot } from 'react-dom/client'
import { ConfigProvider, theme } from 'antd'
import App from './App.tsx'
import './styles/theme.css'

createRoot(document.getElementById('root')!).render(
  <ConfigProvider
    theme={{
      algorithm: theme.darkAlgorithm,
      token: {
        colorPrimary: '#ff2d6b',
        colorBgContainer: '#16161f',
        colorBgElevated: '#1c1c2a',
        colorText: '#ffffff',
        colorTextSecondary: '#8888aa',
        fontFamily: "'DM Sans', -apple-system, sans-serif",
        borderRadius: 10,
      },
    }}
  >
    <App />
  </ConfigProvider>
)
