import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMe } from '../api/user';
import useAuthStore from '../store/authStore';

export const useAuth = () => {
  const store = useAuthStore();

  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
    enabled: !!store.token,
  });

  // Sync user to store when fetched
  useEffect(() => {
    if (meQuery.data && !store.user) {
      store.login(store.token, meQuery.data);
    }
  }, [meQuery.data]);

  return {
    ...store,
    meQuery,
  };
};

