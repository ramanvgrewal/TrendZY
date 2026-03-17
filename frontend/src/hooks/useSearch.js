import { useQuery } from '@tanstack/react-query';
import {
  searchProducts,
  getAutocomplete,
  getTrendingQueries,
} from '../api/search';

export const useTrendingQueries = () =>
  useQuery({
    queryKey: ['trendingQueries'],
    queryFn: getTrendingQueries,
  });

export const useAutocomplete = (q) =>
  useQuery({
    queryKey: ['autocomplete', q],
    queryFn: () => getAutocomplete(q),
    enabled: q && q.length >= 2,
  });

export const useSearchResults = (query, type, vibe, enabled) =>
  useQuery({
    queryKey: ['search', query, type, vibe],
    queryFn: () => searchProducts(query, type, vibe),
    enabled,
  });

