import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import '../theme/theme.dart';
import '../providers/app_state.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({Key? key}) : super(key: key);

  void _handleLogout(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: NightPOSColors.surface,
        title: const Text('Log Out'),
        content: const Text('Do you want to clear the session and log out?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              context.read<AppState>().logout();
              context.go('/login');
            },
            child: const Text('Log Out', style: TextStyle(color: NightPOSColors.errorRed)),
          ),
        ],
      ),
    );
  }

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
            tooltip: 'Settings',
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const SizedBox(height: 8),
          _buildMenuCard(
            context,
            icon: Icons.shopping_cart,
            title: 'Open POS',
            description: 'Open the storefront sales screen',
            onTap: () {
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
            icon: Icons.inventory,
            title: 'Products',
            description: 'Manage products and listings',
            onTap: () {
              context.go(
                '/dashboard/webview',
                extra: {
                  'title': 'Products',
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
            onTap: () => _handleLogout(context),
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
      elevation: 1,
      margin: EdgeInsets.zero,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              Icon(
                icon,
                color: isDestructive ? NightPOSColors.errorRed : NightPOSColors.neonPurple,
                size: 28,
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      description,
                      style: Theme.of(context).textTheme.bodySmall,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Icon(
                Icons.arrow_forward_ios,
                size: 16,
                color: NightPOSColors.textSecondary,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
