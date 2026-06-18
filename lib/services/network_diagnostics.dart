import 'package:flutter/material.dart';
import 'package:connectivity_plus/connectivity_plus.dart';

/// Monitors network connectivity status
/// Mirrors Android NetworkConnectivityObserver pattern
class NetworkDiagnostics extends ChangeNotifier {
  final Connectivity _connectivity = Connectivity();
  bool _isOnline = true;

  bool get isOnline => _isOnline;

  NetworkDiagnostics() {
    _initializeConnectivity();
    _connectivity.onConnectivityChanged.listen(_handleConnectivityChange);
  }

  /// Initialize connectivity status on app start
  Future<void> _initializeConnectivity() async {
    try {
      final result = await _connectivity.checkConnectivity();
      _updateStatus(result);
    } catch (e) {
      // Assume online if check fails
      _isOnline = true;
    }
  }

  /// Handle connectivity changes
  void _handleConnectivityChange(ConnectivityResult result) {
    _updateStatus(result);
  }

  /// Update online status based on connectivity result
  void _updateStatus(ConnectivityResult result) {
    final wasOnline = _isOnline;

    // Consider online if any active transport exists (WiFi, mobile, Ethernet, VPN)
    // Note: ConnectivityResult.none means no active connections
    _isOnline = result != ConnectivityResult.none;

    if (wasOnline != _isOnline) {
      notifyListeners();
    }
  }

  /// Get detailed connectivity info (for diagnostics)
  Future<String> getDiagnosticsInfo() async {
    try {
      final connectivity = await _connectivity.checkConnectivity();
      final connectivityStr = _getConnectivityString(connectivity);

      return '''
Network Diagnostics:
- Status: ${_isOnline ? 'Online' : 'Offline'}
- Connectivity: $connectivityStr
- Timestamp: ${DateTime.now().toIso8601String()}
      ''';
    } catch (e) {
      return 'Failed to get connectivity info: $e';
    }
  }

  String _getConnectivityString(ConnectivityResult result) {
    switch (result) {
      case ConnectivityResult.mobile:
        return 'Mobile (Cellular)';
      case ConnectivityResult.wifi:
        return 'WiFi';
      case ConnectivityResult.ethernet:
        return 'Ethernet';
      case ConnectivityResult.vpn:
        return 'VPN';
      case ConnectivityResult.bluetooth:
        return 'Bluetooth';
      case ConnectivityResult.none:
        return 'None';
      default:
        return 'Unknown';
    }
  }

  @override
  void dispose() {
    // Cleanup if needed
    super.dispose();
  }
}
