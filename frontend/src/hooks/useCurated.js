import { useQuery } from '@tanstack/react-query';
import {
  getCurated,
  getCuratedById,
  getCuratedBrands,
  getFeaturedCurated,
} from '../api/curated';

export const useCuratedList = (vibe, brand, page = 0, size = 20) =>
  useQuery({
    queryKey: ['curated', vibe, brand, page, size],
    queryFn: () =>
      getCurated(
        vibe === 'All' ? null : vibe,
        brand || null,
        page,
        size,
      ),
  });

export const useCuratedItem = (id) =>
  useQuery({
    queryKey: ['curated', id],
    queryFn: () => getCuratedById(id),
    enabled: !!id,
  });

export const useCuratedBrands = () =>
  useQuery({
    queryKey: ['brands'],
    queryFn: getCuratedBrands,
  });

export const useFeaturedCurated = () =>
  useQuery({
    queryKey: ['featured'],
    queryFn: getFeaturedCurated,
  });

