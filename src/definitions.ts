import { registerPlugin, PluginListenerHandle } from '@capacitor/core';

export type NearbyStatus =
  | 'idle'
  | 'advertising'
  | 'discovering'
  | 'connecting'
  | 'connected'
  | 'disconnected'
  | 'stopped';

export type NearbyEndpoint = {
  endpointId: string;
  endpointName?: string;
};

export interface NearbySharePlugin {
  isSupported(): Promise<{ supported: boolean }>;
  requestPermissions(): Promise<void>;
  startAdvertising(options: {
    data: any; // Generic data to share - can be any JSON-serializable object
    endpointName?: string; // Display name shown to other devices
    serviceId: string; // Unique service ID (e.g., com.example.app.nearby)
  }): Promise<void>;
  startDiscovery(options: {
    serviceId: string; // Unique service ID (e.g., com.example.app.nearby)
  }): Promise<void>;
  requestConnection(options: { endpointId: string; localName?: string }): Promise<void>;
  sendPayload(options: { endpointId: string; payload: string }): Promise<void>;
  acceptConnection(options?: { endpointId?: string }): Promise<void>;
  rejectConnection(options?: { endpointId?: string }): Promise<void>;
  stop(): Promise<void>;
}

export const NearbyShare = registerPlugin<NearbySharePlugin>('NearbyShare');

