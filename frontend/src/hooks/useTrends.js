import { useQuery } from '@tanstack/react-query';
import {
  getHeroTrend,
  getTrendStats,
  getTrends,
  getTrendById,
  getRelatedTrends,
  getTickerData,
} from '../api/trends';

export const useHeroTrend = () =>
  useQuery({
    queryKey: ['hero'],
    queryFn: getHeroTrend,
  });

export const useTrendStats = () =>
  useQuery({
    queryKey: ['stats'],
    queryFn: getTrendStats,
  });

export const useTierTrends = (tier, vibe) =>
  useQuery({
    queryKey: [tier, vibe],
    queryFn: () => getTrends(tier, vibe === 'All' ? null : vibe),
  });

export const useTrend = (id) =>
  useQuery({
    queryKey: ['trend', id],
    queryFn: () => getTrendById(id),
    enabled: !!id,
  });

export const useRelatedTrends = (id) =>
  useQuery({
    queryKey: ['related', id],
    queryFn: () => getRelatedTrends(id),
    enabled: !!id,
  });

export const useTickerData = () =>
  useQuery({
    queryKey: ['ticker'],
    queryFn: getTickerData,
  });

