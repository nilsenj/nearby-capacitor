# Capacitor Nearby Share Plugin

A Capacitor plugin that enables peer-to-peer profile sharing using Android's Nearby Connections API.

## Features

- 🔍 **Device Discovery** - Automatically discover nearby devices
- 📡 **Bluetooth & WiFi** - Uses both for reliable connections
- 🔒 **Secure** - Encrypted peer-to-peer connections
- ⚡ **Fast** - Direct device-to-device transfer
- 🎯 **Simple API** - Easy to integrate

## Installation

```bash
npm install @meetfolio/capacitor-nearby-share
npx cap sync
```

## Android Setup

### Permissions

Add the following permissions to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

### Dependencies

The plugin requires Google Play Services. This is automatically included via the plugin's `build.gradle`:

```gradle
implementation "com.google.android.gms:play-services-nearby:18.5.0"
```

## Usage

### Import

```typescript
import { NearbyShare } from '@meetfolio/capacitor-nearby-share';
```

### Check Support

```typescript
const { supported } = await NearbyShare.isSupported();
if (supported) {
  console.log('Nearby Share is available!');
}
```

### Request Permissions

```typescript
try {
  await NearbyShare.requestPermissions();
  console.log('Permissions granted');
} catch (error) {
  console.error('Permissions denied:', error);
}
```

### Start Advertising

Make your device discoverable and start sharing any data:

```typescript
try {
  await NearbyShare.startAdvertising({
    data: {
      type: 'profile',
      url: 'https://example.com/profile/johndoe',
      name: 'John Doe',
      // ... any other data you want to share
    },
    endpointName: 'John Doe', // Display name shown to other devices
    serviceId: 'com.example.app.nearby', // Unique service ID for your app
  });
  console.log('Advertising started');
} catch (error) {
  console.error('Failed to start advertising:', error);
}
```

**Note**: The `data` field can contain any JSON-serializable object. Structure it however you need for your use case.

### Start Discovery

Discover nearby advertising devices:

```typescript
await NearbyShare.startDiscovery({
  serviceId: 'com.example.app.nearby', // Unique service ID matching the advertiser
});

// Listen for discovered endpoints
NearbyShare.addListener('nearbyEndpointFound', (data) => {
  console.log('Found device:', data.endpointId, data.endpointName);
});
```

### Connect to Device

```typescript
await NearbyShare.requestConnection({
  endpointId: 'discovered-endpoint-id',
  localName: 'My Device Name',
});

// Listen for connection events
NearbyShare.addListener('nearbyConnectionInitiated', (data) => {
  console.log('Connection initiated with:', data.endpointName);
});

NearbyShare.addListener('nearbyConnected', (data) => {
  console.log('Connected to:', data.endpointId);
});
```

### Send Data

```typescript
await NearbyShare.sendPayload({
  endpointId: 'connected-endpoint-id',
  payload: JSON.stringify({ profileUrl: 'https://...' }),
});
```

### Receive Data

```typescript
NearbyShare.addListener('nearbyPayload', (data) => {
  console.log('Received from:', data.endpointId);
  const payload = JSON.parse(data.payload);
  console.log('Payload:', payload);
});
```

### Stop Sharing

```typescript
await NearbyShare.stop();
```

## API Reference

### Methods

#### `isSupported()`

Check if Nearby Share is supported on the device.

**Returns:** `Promise<{ supported: boolean }>`

---

#### `requestPermissions()`

Request necessary permissions for Nearby Share.

**Returns:** `Promise<void>`

**Throws:** Error if permissions are denied

---

#### `startAdvertising(options)`

Start advertising to make device discoverable.

**Parameters:**

- `options.data` (any, required) - Any JSON-serializable object to share
- `options.serviceId` (string, required) - Unique service ID (e.g., "com.example.app.nearby")
- `options.endpointName` (string, optional) - Display name for this device (default: "Nearby Device")

**Returns:** `Promise<void>`

**Example**:

```typescript
await NearbyShare.startAdvertising({
  data: { type: 'profile', url: 'https://...', name: '...' },
  endpointName: 'John Doe',
  serviceId: 'com.example.app.nearby',
});
```

---

#### `startDiscovery(options)`

Start discovering nearby advertising devices.

**Parameters:**

- `options.serviceId` (string, required) - Unique service ID matching the advertiser

**Returns:** `Promise<void>`

---

#### `requestConnection(options)`

Request connection to a discovered device.

**Parameters:**

- `options.endpointId` (string, required) - ID of endpoint to connect to
- `options.localName` (string, optional) - Local device name

**Returns:** `Promise<void>`

---

#### `sendPayload(options)`

Send data to a connected device.

**Parameters:**

- `options.endpointId` (string, required) - Connected endpoint ID
- `options.payload` (string, required) - Data to send

**Returns:** `Promise<void>`

---

#### `acceptConnection(options?)`

Accept an incoming connection request.

**Parameters:**

- `options.endpointId` (string, optional) - Endpoint ID (uses pending if omitted)

**Returns:** `Promise<void>`

---

#### `rejectConnection(options?)`

Reject an incoming connection request.

**Parameters:**

- `options.endpointId` (string, optional) - Endpoint ID (uses pending if omitted)

**Returns:** `Promise<void>`

---

#### `stop()`

Stop all advertising and discovery.

**Returns:** `Promise<void>`

---

### Events

Listen to events using `addListener`:

#### `nearbyStatus`

**Data:** `{ status: NearbyStatus }`

Status values: `'idle'`, `'advertising'`, `'discovering'`, `'connecting'`, `'connected'`, `'disconnected'`, `'stopped'`

---

#### `nearbyEndpointFound`

Fired when a nearby device is discovered.

**Data:** `{ endpointId: string, endpointName?: string }`

---

#### `nearbyEndpointLost`

Fired when a device is no longer nearby.

**Data:** `{ endpointId: string }`

---

#### `nearbyConnectionInitiated`

Fired when a connection is being established.

**Data:** `{ endpointId: string, endpointName?: string }`

---

#### `nearbyConnected`

Fired when successfully connected to a device.

**Data:** `{ endpointId: string, endpointName?: string }`

---

#### `nearbyDisconnected`

Fired when disconnected from a device.

**Data:** `{ endpointId?: string }`

---

#### `nearbyPayload`

Fired when data is received.

**Data:** `{ endpointId: string, payload: string }`

---

#### `nearbyError`

Fired when an error occurs.

**Data:** `{ message: string }`

---

## Example Implementation

See the [example implementation](./example) for a complete working example with UI.

## Requirements

- **Android**: API 21+ (Android 5.0+)
- **Google Play Services**: Must be installed on device
- **Capacitor**: 6.0.0+

## iOS Support

iOS is not currently supported as Nearby Connections is Android-only. For iOS, consider using MultipeerConnectivity framework.

## Troubleshooting

### Plugin not detected

Make sure to run `npx cap sync` after installation.

### Permissions denied

Check that all required permissions are in `AndroidManifest.xml` and that location services are enabled on the device.

### Connection fails

- Ensure both devices have WiFi and Bluetooth enabled
- Check that devices are in close proximity (within ~100m)
- Verify Google Play Services is up to date

## License

MIT

## Credits

Created for [Meetfolio](https://meetfolio.app) - Professional networking made simple.
