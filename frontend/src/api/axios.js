import axios from 'axios';

const instance = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 15000,
});

// Request interceptor: attach JWT if exists
instance.interceptors.request.use(config => {
  const token = localStorage.getItem('trendzy_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor: on 401 → clear token → redirect /
instance.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('trendzy_token');
      window.location.href = '/';
    }
    return Promise.reject(err);
  }
);

export default instance;
