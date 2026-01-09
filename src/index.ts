import { registerPlugin } from '@capacitor/core';

import type { NearbySharePlugin } from './definitions';

const NearbyShare = registerPlugin<NearbySharePlugin>('NearbyShare', {
  web: () => import('./web').then(m => new m.NearbyShareWeb()),
});

export * from './definitions';
export { NearbyShare };
