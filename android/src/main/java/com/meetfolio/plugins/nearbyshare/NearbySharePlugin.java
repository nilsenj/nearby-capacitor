package com.meetfolio.plugins.nearbyshare;

import android.Manifest;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@CapacitorPlugin(
    name = "NearbyShare",
    permissions = {
        @Permission(
            alias = "nearby",
            strings = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                "android.permission.NEARBY_WIFI_DEVICES"
            }
        )
    }
)
public class NearbySharePlugin extends Plugin {
    private static final String TAG = "NearbyShare";
    // Service ID will be passed dynamically or fallback to generic
    private static final String DEFAULT_SERVICE_ID = "com.meetfolio.app.nearby"; // Keep as fallback for now
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    private ConnectionsClient connectionsClient;
    private final Map<String, String> endpoints = new HashMap<>();
    private String advertisingPayload;
    private boolean advertising;
    private boolean discovering;
    private String connectedEndpointId;
    private String pendingEndpointId;
    private String pendingEndpointName;

    @Override
    public void load() {
        try {
            connectionsClient = Nearby.getConnectionsClient(getContext());
            Log.d(TAG, "NearbySharePlugin loaded successfully. ConnectionsClient initialized.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ConnectionsClient", e);
        }
    }

    @PluginMethod
    public void isSupported(PluginCall call) {
        int status = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(getContext());
        boolean supported = status == ConnectionResult.SUCCESS;
        
        Log.d(TAG, "isSupported check - Status code: " + status + ", Supported: " + supported);
        if (!supported) {
            String errorMsg = GoogleApiAvailability.getInstance().getErrorString(status);
            Log.w(TAG, "Google Play Services not available: " + errorMsg);
        }
        
        JSObject result = new JSObject();
        result.put("supported", supported);
        call.resolve(result);
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        PermissionState state = getPermissionState("nearby");
        Log.d(TAG, "requestPermissions called. Current state: " + state);
        
        if (state == PermissionState.GRANTED) {
            Log.d(TAG, "Permissions already granted.");
            call.resolve();
            return;
        }
        
        Log.d(TAG, "Requesting Nearby permissions...");
        requestAllPermissions(call, "permissionsCallback");
    }

    @PermissionCallback
    private void permissionsCallback(PluginCall call) {
        PermissionState state = getPermissionState("nearby");
        Log.d(TAG, "permissionsCallback - Final state: " + state);
        
        if (state == PermissionState.GRANTED) {
            Log.d(TAG, "Permissions granted successfully.");
            call.resolve();
        } else {
            Log.w(TAG, "Permissions denied by user.");
            call.reject("Permissions denied. Location and Bluetooth permissions are required for Nearby Share.");
        }
    }

    @PluginMethod
    public void startAdvertising(PluginCall call) {
        Log.d(TAG, "startAdvertising called");
        
        if (!ensurePermissions(call)) {
            Log.w(TAG, "startAdvertising failed: permissions not granted");
            return;
        }
        
        JSObject data = call.getObject("data");
        if (data == null) {
            Log.e(TAG, "startAdvertising failed: missing data");
            call.reject("Missing data object.");
            return;
        }
        
        String endpointName = call.getString("endpointName", "Nearby Device");
        String serviceId = call.getString("serviceId", DEFAULT_SERVICE_ID);
        
        Log.d(TAG, "Starting advertising with endpointName: " + endpointName + " serviceId: " + serviceId);

        advertisingPayload = data.toString();

        stopAll();
        pendingEndpointId = null;
        pendingEndpointName = null;

        AdvertisingOptions options = new AdvertisingOptions.Builder()
            .setStrategy(STRATEGY)
            .build();
        connectionsClient
            .startAdvertising(endpointName, serviceId, connectionLifecycleCallback, options)
            .addOnSuccessListener(
                (unused) -> {
                    advertising = true;
                    Log.i(TAG, "Advertising started successfully");
                    notifyStatus("advertising");
                    call.resolve();
                }
            )
            .addOnFailureListener(
                (error) -> {
                    Log.e(TAG, "Advertising failed: " + error.getMessage(), error);
                    notifyError("Advertising failed: " + error.getMessage());
                    call.reject("Failed to start advertising. " + error.getMessage());
                }
            );
    }

    @PluginMethod
    public void startDiscovery(PluginCall call) {
        Log.d(TAG, "startDiscovery called");
        
        if (!ensurePermissions(call)) {
            Log.w(TAG, "startDiscovery failed: permissions not granted");
            return;
        }
        
        // Don't call stopAll() - we want advertising and discovery to run simultaneously
        DiscoveryOptions options = new DiscoveryOptions.Builder()
            .setStrategy(STRATEGY)
            .build();

        String serviceId = call.getString("serviceId", DEFAULT_SERVICE_ID);

        connectionsClient
            .startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener(
                (unused) -> {
                    discovering = true;
                    Log.i(TAG, "Discovery started successfully");
                    notifyStatus("discovering");
                    call.resolve();
                }
            )
            .addOnFailureListener(
                (error) -> {
                    Log.e(TAG, "Discovery failed: " + error.getMessage(), error);
                    notifyError("Discovery failed: " + error.getMessage());
                    call.reject("Discovery failed: " + error.getMessage());
                }
            );
    }

    @PluginMethod
    public void requestConnection(PluginCall call) {
        if (!ensurePermissions(call)) {
            return;
        }
        String endpointId = call.getString("endpointId");
        if (endpointId == null || endpointId.trim().isEmpty()) {
            call.reject("Missing endpointId.");
            return;
        }
        String localName = call.getString("localName", "Nearby User");
        notifyStatus("connecting");
        connectionsClient
            .requestConnection(localName, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener((unused) -> call.resolve())
            .addOnFailureListener(
                (error) -> {
                    Log.e(TAG, "Connection request failed", error);
                    notifyError("Connection request failed.");
                    call.reject("Connection request failed.");
                }
            );
    }

    @PluginMethod
    public void sendPayload(PluginCall call) {
        String endpointId = call.getString("endpointId");
        String payload = call.getString("payload");
        if (endpointId == null || payload == null) {
            call.reject("Missing endpointId or payload.");
            return;
        }
        connectionsClient.sendPayload(
            endpointId,
            Payload.fromBytes(payload.getBytes(StandardCharsets.UTF_8))
        );
        call.resolve();
    }

    @PluginMethod
    public void acceptConnection(PluginCall call) {
        String endpointId = call.getString("endpointId");
        if (endpointId == null || endpointId.trim().isEmpty()) {
            endpointId = pendingEndpointId;
        }
        if (endpointId == null) {
            call.reject("Missing endpointId.");
            return;
        }
        try {
            connectionsClient.acceptConnection(endpointId, payloadCallback);
            pendingEndpointId = null;
            pendingEndpointName = null;
            call.resolve();
        } catch (Exception ex) {
            call.reject("Failed to accept connection.");
        }
    }

    @PluginMethod
    public void rejectConnection(PluginCall call) {
        String endpointId = call.getString("endpointId");
        if (endpointId == null || endpointId.trim().isEmpty()) {
            endpointId = pendingEndpointId;
        }
        if (endpointId == null) {
            call.reject("Missing endpointId.");
            return;
        }
        try {
            connectionsClient.rejectConnection(endpointId);
            pendingEndpointId = null;
            pendingEndpointName = null;
            call.resolve();
        } catch (Exception ex) {
            call.reject("Failed to reject connection.");
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        stopAll();
        call.resolve();
    }

    private boolean ensurePermissions(PluginCall call) {
        if (getPermissionState("nearby") == PermissionState.GRANTED) {
            return true;
        }
        call.reject("Permissions not granted.");
        return false;
    }

    private void stopAll() {
        endpoints.clear();
        advertising = false;
        discovering = false;
        connectedEndpointId = null;
        pendingEndpointId = null;
        pendingEndpointName = null;
        try {
            connectionsClient.stopDiscovery();
            connectionsClient.stopAdvertising();
            connectionsClient.stopAllEndpoints();
        } catch (Exception ignored) {
        }
        notifyStatus("stopped");
    }

    private void notifyStatus(String status) {
        JSObject payload = new JSObject();
        payload.put("status", status);
        notifyListeners("nearbyStatus", payload);
    }

    private void notifyError(String message) {
        JSObject payload = new JSObject();
        payload.put("message", message);
        notifyListeners("nearbyError", payload);
    }

    private final EndpointDiscoveryCallback discoveryCallback =
        new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                endpoints.put(endpointId, info.getEndpointName());
                JSObject payload = new JSObject();
                payload.put("endpointId", endpointId);
                payload.put("endpointName", info.getEndpointName());
                notifyListeners("nearbyEndpointFound", payload);
            }

            @Override
            public void onEndpointLost(String endpointId) {
                endpoints.remove(endpointId);
                JSObject payload = new JSObject();
                payload.put("endpointId", endpointId);
                notifyListeners("nearbyEndpointLost", payload);
            }
        };

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
        new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                pendingEndpointId = endpointId;
                pendingEndpointName = connectionInfo.getEndpointName();
                JSObject payload = new JSObject();
                payload.put("endpointId", endpointId);
                payload.put("endpointName", connectionInfo.getEndpointName());
                notifyListeners("nearbyConnectionInitiated", payload);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution result) {
                if (result.getStatus().isSuccess()) {
                    connectedEndpointId = endpointId;
                    pendingEndpointId = null;
                    pendingEndpointName = null;
                    notifyStatus("connected");
                    JSObject payload = new JSObject();
                    payload.put("endpointId", endpointId);
                    payload.put("endpointName", endpoints.get(endpointId));
                    notifyListeners("nearbyConnected", payload);
                    if (advertisingPayload != null && advertising) {
                        connectionsClient.sendPayload(
                            endpointId,
                            Payload.fromBytes(advertisingPayload.getBytes(StandardCharsets.UTF_8))
                        );
                    }
                } else {
                    notifyError("Connection failed.");
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                if (endpointId != null && endpointId.equals(connectedEndpointId)) {
                    connectedEndpointId = null;
                }
                JSObject payload = new JSObject();
                payload.put("endpointId", endpointId);
                notifyListeners("nearbyDisconnected", payload);
                notifyStatus("disconnected");
            }
        };

    private final PayloadCallback payloadCallback =
        new PayloadCallback() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                if (payload.getType() != Payload.Type.BYTES) {
                    return;
                }
                byte[] bytes = payload.asBytes();
                if (bytes == null) {
                    return;
                }
                String content = new String(bytes, StandardCharsets.UTF_8);
                JSObject data = new JSObject();
                data.put("endpointId", endpointId);
                data.put("payload", content);
                notifyListeners("nearbyPayload", data);
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                // no-op for now
            }
        };
}
