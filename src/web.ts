import { WebPlugin, PluginListenerHandle } from '@capacitor/core';

import type { NearbySharePlugin } from './definitions';

export class NearbyShareWeb extends WebPlugin implements NearbySharePlugin {
  async isSupported(): Promise<{ supported: boolean }> {
    return { supported: false };
  }

  async requestPermissions(): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async startAdvertising(_options: {
    data: any;
    endpointName?: string;
    serviceId: string;
  }): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async startDiscovery(_options: { serviceId: string }): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async requestConnection(_options: { endpointId: string; localName?: string }): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async sendPayload(_options: { endpointId: string; payload: string }): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async acceptConnection(_options?: { endpointId?: string }): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async rejectConnection(_options?: { endpointId?: string }): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }

  async stop(): Promise<void> {
    throw this.unimplemented('Not implemented on web.');
  }
}
