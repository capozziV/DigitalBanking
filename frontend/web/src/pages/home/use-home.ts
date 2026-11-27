import { useQuery } from '@tanstack/react-query';
import type { AccountSummary, DailyLimit, Entry } from '@/lib/api-types';
import { bff, BffError } from '@/lib/bff-client';

export interface HomeDTO {
  account: AccountSummary | null;
  dailyLimit: DailyLimit | null;
  latestEntries: Entry[];
}

export const NO_ACCOUNT: HomeDTO = { account: null, dailyLimit: null, latestEntries: [] };

/**
 * `/bff/home`, shared by Home and My account under one query key, so opening one after the
 * other reuses the data instead of a second request. Without an account in the session there is
 * nothing to ask for. No cache past the screens: the next person to sign in on this tab must not
 * see this balance.
 */
export function useHome(hasAccount: boolean) {
  return useQuery({
    queryKey: ['home'],
    enabled: hasAccount,
    retry: false,
    gcTime: 0,
    queryFn: async (): Promise<HomeDTO> => {
      try {
        return await bff<HomeDTO>('/bff/home');
      } catch (caught) {
        if (caught instanceof BffError && caught.status === 404) return NO_ACCOUNT;
        throw caught;
      }
    },
  });
}
