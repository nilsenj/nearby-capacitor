# Capacitor Nearby Share - Angular Example

This example demonstrates how to integrate `@meetfolio/capacitor-nearby-share` into an Angular application.

## Key Concepts

1.  **NgZone**: Capacitor events fire outside the Angular zone. You must wrap callbacks in `this.zone.run()` to trigger change detection.
2.  **Lifecycle**: Clean up listeners in `ngOnDestroy` to prevent memory leaks.

## Implementation

```typescript
import { Component, NgZone, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { NearbyShare } from '@meetfolio/capacitor-nearby-share';
import { PluginListenerHandle } from '@capacitor/core';

@Component({
  selector: 'app-nearby-share',
  template: `
    <div class="nearby-container">
      <h2>Nearby Share Demo</h2>

      <div class="actions">
        <button (click)="startSharing()" [disabled]="isAdvertising">
          {{ isAdvertising ? 'Sharing...' : 'Start Sharing' }}
        </button>
        <button (click)="stop()" *ngIf="isAdvertising">Stop</button>
      </div>

      <div class="devices-list" *ngIf="devices.length > 0">
        <h3>Discovered Devices</h3>
        <div *ngFor="let device of devices" class="device-item">
          <span>{{ device.endpointName }}</span>
          <button (click)="connect(device.endpointId)">Connect</button>
        </div>
      </div>

      <div class="logs">
        <div *ngFor="let log of logs">{{ log }}</div>
      </div>
    </div>
  `,
})
export class NearbyComponent implements OnDestroy {
  devices: any[] = [];
  logs: string[] = [];
  isAdvertising = false;
  private listeners: PluginListenerHandle[] = [];

  constructor(private zone: NgZone, private cdr: ChangeDetectorRef) {}

  async startSharing() {
    try {
      this.addLog('Starting Nearby Share...');

      // 1. Start Advertising
      // Make this device discoverable to others
      await NearbyShare.startAdvertising({
        data: { type: 'profile', name: 'Angular User' },
        endpointName: 'Angular Device',
        serviceId: 'com.myapp.share', // Must match discovery serviceId
      });
      this.isAdvertising = true;

      // 2. Start Discovery
      // Look for other devices advertising the same serviceId
      await NearbyShare.startDiscovery({
        serviceId: 'com.myapp.share',
      });

      // 3. Register Event Listeners
      await this.registerListeners();

      this.addLog('Ready to discover devices!');
    } catch (err) {
      console.error(err);
      this.addLog('Error starting share: ' + err.message);
    }
  }

  async connect(endpointId: string) {
    this.addLog(`Requesting connection to ${endpointId}...`);
    await NearbyShare.requestConnection({
      endpointId,
      localName: 'Angular Device',
    });
  }

  async stop() {
    await NearbyShare.stop();
    this.devices = [];
    this.isAdvertising = false;
    this.addLog('Stopped Nearby Share');
  }

  private async registerListeners() {
    // DEVICE FOUND
    const found = await NearbyShare.addListener(
      'nearbyEndpointFound',
      (event) => {
        this.zone.run(() => {
          this.addLog(`Found device: ${event.endpointName}`);
          // Avoid duplicates
          if (!this.devices.find((d) => d.endpointId === event.endpointId)) {
            this.devices.push(event);
            this.cdr.detectChanges();
          }
        });
      }
    );
    this.listeners.push(found);

    // DEVICE LOST
    const lost = await NearbyShare.addListener(
      'nearbyEndpointLost',
      (event) => {
        this.zone.run(() => {
          this.addLog(`Lost device: ${event.endpointId}`);
          this.devices = this.devices.filter(
            (d) => d.endpointId !== event.endpointId
          );
          this.cdr.detectChanges();
        });
      }
    );
    this.listeners.push(lost);

    // CONNECTION INITIATED
    // Automatically accept connections for this demo
    const initiated = await NearbyShare.addListener(
      'nearbyConnectionInitiated',
      (event) => {
        this.zone.run(async () => {
          this.addLog(`Connection initiated from ${event.endpointName}`);
          await NearbyShare.acceptConnection({ endpointId: event.endpointId });
        });
      }
    );
    this.listeners.push(initiated);

    // CONNECTED
    const connected = await NearbyShare.addListener(
      'nearbyConnected',
      (event) => {
        this.zone.run(() => {
          this.addLog(`Connected to ${event.endpointId}!`);
        });
      }
    );
    this.listeners.push(connected);
  }

  private addLog(msg: string) {
    this.logs.unshift(`${new Date().toLocaleTimeString()} - ${msg}`);
  }

  async ngOnDestroy() {
    // Always cleanup plugins and listeners
    this.listeners.forEach((l) => l.remove());
    this.listeners = [];
    await NearbyShare.stop();
  }
}
```
