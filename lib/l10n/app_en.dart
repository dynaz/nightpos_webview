/// English strings for NightPOS
/// Mirrors Android res/values-en/strings.xml
const Map<String, String> enStrings = {
  // Dashboard
  'app_name': 'NightPOS Soho',
  'menu_open_pos': 'Open POS',
  'menu_open_pos_desc': 'Open the storefront sales screen',
  'menu_reports': 'Reports',
  'menu_reports_desc': 'View sales reports and summaries',
  'menu_customers': 'Customers',
  'menu_customers_desc': 'Manage customer information',
  'menu_products': 'Products',
  'menu_products_desc': 'Manage products and listings',
  'menu_discount_loyalty': 'Discount & Loyalty',
  'menu_discount_loyalty_desc': 'Manage discount and loyalty programs',
  'menu_gift_cards': 'Gift Card & eWallet',
  'menu_gift_cards_desc': 'Manage gift cards and eWallets',
  'menu_employees': 'Employees',
  'menu_employees_desc': 'Manage employee records',
  'menu_settings': 'Settings',
  'menu_settings_desc': 'Customize app settings',
  'menu_logout': 'Log Out',
  'menu_logout_desc': 'Clear session and log out',

  'dashboard_title': 'NightPOS Soho',
  'dashboard_subtitle': 'Point of sale system for restaurants and nightclubs',

  // Logout dialog
  'logout_dialog_title': 'Log Out',
  'logout_dialog_message': 'Do you want to clear the session and log out?',
  'logout_confirm': 'Log Out',
  'logout_cancel': 'Cancel',
  'logout_success': 'Logged out successfully',

  // WebView
  'webview_loading': 'Loading…',
  'webview_exit_pos_title': 'Exit POS Screen',
  'webview_exit_pos_message': 'Do you want to exit the POS screen and return to the main menu?',
  'webview_exit_confirm': 'Exit',
  'webview_exit_cancel': 'Stay',

  // WebView errors
  'webview_error_title': 'Failed to Load Page',
  'webview_error_message': 'A connection error occurred. Please try again.',
  'webview_error_timeout_message': 'Loading is taking too long. Please check your connection and try again.',
  'webview_error_detail': 'Detail: %s',

  // Offline
  'offline_title': 'No Internet Connection',
  'offline_message': 'Please check your network connection and try again',
  'offline_retry': 'Retry',
  'offline_auto_reconnect': 'Automatically checking connection…',

  // Settings
  'settings_title': 'Settings',
  'settings_section_general': 'General',
  'settings_section_pos': 'POS Screen',
  'settings_section_about': 'About',

  'settings_server_url': 'POS Server Address',
  'settings_server_url_desc': 'The address of the connected Odoo POS system',

  'settings_kiosk_mode': 'Kiosk Mode',
  'settings_kiosk_mode_desc': 'Hide the system bars and lock the screen while using the POS screen',

  'settings_keep_screen_on': 'Keep Screen On',
  'settings_keep_screen_on_desc': "The screen won't turn off while the POS screen is open",

  'settings_auto_reopen_pos': 'Auto-reopen POS',
  'settings_auto_reopen_pos_desc': "Immediately reopen the POS screen if it's accidentally closed (Kiosk mode)",

  'settings_clear_data': 'Clear Cache & Cookies',
  'settings_clear_data_desc': 'Delete all stored WebView data',
  'settings_clear_data_success': 'Data cleared successfully',

  'settings_version': 'App Version',

  // Language
  'settings_language': 'Language',
  'settings_language_desc': "Choose the app's display language",
  'language_dialog_title': 'Select Language',
  'language_system_default': 'System default',
  'language_thai': 'ไทย (Thai)',
  'language_english': 'English',

  // Actions
  'action_back': 'Back',
  'action_home': 'Home',
  'action_reload': 'Reload',
  'action_close': 'Close',
  'action_save': 'Save',

  // Kiosk exit
  'kiosk_exit_title': 'Exit Kiosk Mode',
  'kiosk_exit_message': 'Do you want to exit the app? This will close the POS system',
  'kiosk_exit_confirm': 'Exit App',
  'kiosk_exit_cancel': 'Cancel',

  // Permissions
  'permission_camera_rationale': 'The app needs camera access to use scanning and image upload features in the POS screen',
  'permission_denied': 'Permission denied. Some features may not work',

  // Splash
  'splash_loading': 'Starting up…',

  // Diagnostics
  'diagnostics_title': 'Diagnostics',
  'diagnostics_copy': 'Copy',
  'diagnostics_copied': 'Diagnostics info copied',

  // Login
  'login_title': 'Log In',
  'login_server_url': 'Server URL',
  'login_username': 'Username',
  'login_password': 'Password',
  'login_button': 'Log In',
  'login_support': 'Contact support for account access',
};
