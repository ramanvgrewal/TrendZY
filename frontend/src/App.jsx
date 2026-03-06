import { BrowserRouter, Route, Routes } from 'react-router-dom';
import ErrorBoundary from './components/ErrorBoundary';
import Layout from './components/Layout';
import Index from './pages/Index';
import TrendDetailPage from './pages/TrendDetailPage';

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<Index mode="discover" />} />
            <Route path="/rising" element={<Index mode="rising" />} />
            <Route path="/india" element={<Index mode="discover" />} />
            <Route path="/trend/:id" element={<TrendDetailPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
