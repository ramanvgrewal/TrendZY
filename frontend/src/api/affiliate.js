import axios from './axios';

export const getAffiliateLink = (productId, platform) =>
  axios.get('/api/affiliate/link', { params: { productId, platform } })
  .then(r => r.data.data);

// Fire and forget — don't await this
export const trackClick = (productId, platform, source) =>
  axios.post('/api/affiliate/click', { productId, platform, source })
  .catch(() => {}); // silently fail, never block UI
