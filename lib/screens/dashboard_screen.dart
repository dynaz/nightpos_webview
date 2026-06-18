import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../theme/theme.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: NightPOSColors.nightBlack,
      appBar: AppBar(
        title: const Text('NightPOS Soho'),
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => context.go('/dashboard/settings'),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildMenuCard(
            context,
            icon: Icons.shopping_cart,
            title: 'Open POS',
            description: 'Open the storefront sales screen',
            onTap: () {
              // TODO: Navigate to POS webview
              context.go(
                '/dashboard/webview',
                extra: {
                  'title': 'POS',
                  'url': 'https://soho.nightpos.com/pos/ui',
                },
              );
            },
          ),
          const SizedBox(height: 12),
          _buildMenuCard(
            context,
            icon: Icons.bar_chart,
            title: 'Reports',
            description: 'View sales reports and summaries',
            onTap: () {
              context.go(
                '/dashboard/webview',
                extra: {
                  'title': 'Reports',
                  'url': 'https://soho.nightpos.com/web',
                },
              );
            },
          ),
          const SizedBox(height: 12),
          _buildMenuCard(
            context,
            icon: Icons.people,
            title: 'Customers',
            description: 'Manage customer information',
            onTap: () {
              context.go(
                '/dashboard/webview',
                extra: {
                  'title': 'Customers',
                  'url': 'https://soho.nightpos.com/web',
                },
              );
            },
          ),
          const SizedBox(height: 12),
          _buildMenuCard(
            context,
            icon: Icons.logout,
            title: 'Log Out',
            description: 'Clear session and log out',
            onTap: () {
              // TODO: Implement logout
            },
            isDestructive: true,
          ),
        ],
      ),
    );
  }

  Widget _buildMenuCard(
    BuildContext context, {
    required IconData icon,
    required String title,
    required String description,
    required VoidCallback onTap,
    bool isDestructive = false,
  }) {
    return Card(
      color: NightPOSColors.surface,
      child: ListTile(
        leading: Icon(
          icon,
          color: isDestructive ? NightPOSColors.errorRed : NightPOSColors.neonPurple,
        ),
        title: Text(title),
        subtitle: Text(description, style: const TextStyle(color: NightPOSColors.textSecondary)),
        onTap: onTap,
        trailing: const Icon(Icons.arrow_forward_ios, size: 16, color: NightPOSColors.textSecondary),
      ),
    );
  }
}
