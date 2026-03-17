import { useQuery } from '@tanstack/react-query';
import { getMe } from '../api/user';
import useAuthStore from '../store/authStore';

export const useAuth = () => {
  const store = useAuthStore();

  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
    enabled: !!store.token,
    onSuccess: (user) => {
      if (user && !store.user) {
        store.login(store.token, user);
      }
    },
  });

  return {
    ...store,
    meQuery,
  };
};

