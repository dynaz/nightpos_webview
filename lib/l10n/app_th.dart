/// Thai strings for NightPOS
/// Mirrors Android res/values/strings.xml (Thai is default)
const Map<String, String> thStrings = {
  // Dashboard
  'app_name': 'NightPOS Soho',
  'menu_open_pos': 'เปิดขาย',
  'menu_open_pos_desc': 'เปิดหน้าจอขายหน้าร้าน',
  'menu_reports': 'รายงาน',
  'menu_reports_desc': 'ดูรายงานยอดขายและสรุปผล',
  'menu_customers': 'ลูกค้า',
  'menu_customers_desc': 'จัดการข้อมูลลูกค้า',
  'menu_products': 'สินค้า',
  'menu_products_desc': 'จัดการสินค้าและรายการ',
  'menu_discount_loyalty': 'ส่วนลด & สะสมแต้ม',
  'menu_discount_loyalty_desc': 'จัดการโปรแกรมส่วนลดและสะสมแต้ม',
  'menu_gift_cards': 'Gift Card & eWallet',
  'menu_gift_cards_desc': 'จัดการ Gift Card และ eWallet',
  'menu_employees': 'พนักงาน',
  'menu_employees_desc': 'จัดการข้อมูลพนักงาน',
  'menu_settings': 'ตั้งค่า',
  'menu_settings_desc': 'ปรับแต่งการตั้งค่าแอป',
  'menu_logout': 'ออกจากระบบ',
  'menu_logout_desc': 'ล้างเซสชันและออกจากระบบ',

  'dashboard_title': 'NightPOS Soho',
  'dashboard_subtitle': 'ระบบจุดขายสำหรับร้านอาหารและไนท์คลับ',

  // Logout dialog
  'logout_dialog_title': 'ออกจากระบบ',
  'logout_dialog_message': 'คุณต้องการล้างเซสชันและออกจากระบบหรือไม่?',
  'logout_confirm': 'ออกจากระบบ',
  'logout_cancel': 'ยกเลิก',
  'logout_success': 'ออกจากระบบเรียบร้อยแล้ว',

  // WebView
  'webview_loading': 'กำลังโหลด…',
  'webview_exit_pos_title': 'ออกจากหน้าขาย',
  'webview_exit_pos_message': 'คุณต้องการออกจากหน้าขายและกลับสู่เมนูหลักหรือไม่?',
  'webview_exit_confirm': 'ออก',
  'webview_exit_cancel': 'อยู่ต่อ',

  // WebView errors
  'webview_error_title': 'ไม่สามารถโหลดหน้านี้ได้',
  'webview_error_message': 'เกิดข้อผิดพลาดในการเชื่อมต่อ กรุณาลองใหม่อีกครั้ง',
  'webview_error_timeout_message': 'การโหลดใช้เวลานานเกินไป กรุณาตรวจสอบการเชื่อมต่อแล้วลองใหม่',
  'webview_error_detail': 'รายละเอียด: %s',

  // Offline
  'offline_title': 'ไม่มีการเชื่อมต่ออินเทอร์เน็ต',
  'offline_message': 'กรุณาตรวจสอบการเชื่อมต่อเครือข่ายของคุณแล้วลองอีกครั้ง',
  'offline_retry': 'ลองอีกครั้ง',
  'offline_auto_reconnect': 'กำลังตรวจสอบการเชื่อมต่อโดยอัตโนมัติ…',

  // Settings
  'settings_title': 'ตั้งค่า',
  'settings_section_general': 'ทั่วไป',
  'settings_section_pos': 'หน้าจอขาย',
  'settings_section_about': 'เกี่ยวกับ',

  'settings_server_url': 'ที่อยู่เซิร์ฟเวอร์ POS',
  'settings_server_url_desc': 'ที่อยู่ของระบบ Odoo POS ที่เชื่อมต่อ',

  'settings_kiosk_mode': 'โหมดคีออสก์ (Kiosk Mode)',
  'settings_kiosk_mode_desc': 'ซ่อนแถบระบบและล็อกหน้าจอขณะใช้งานหน้าขาย',

  'settings_keep_screen_on': 'ป้องกันหน้าจอดับ',
  'settings_keep_screen_on_desc': 'หน้าจอจะไม่ดับขณะเปิดใช้งานหน้าขาย',

  'settings_auto_reopen_pos': 'เปิดหน้าขายอัตโนมัติ',
  'settings_auto_reopen_pos_desc': 'เปิดหน้าขายขึ้นใหม่ทันทีหากถูกปิดโดยไม่ตั้งใจ (โหมดคีออสก์)',

  'settings_clear_data': 'ล้างข้อมูลแคชและคุกกี้',
  'settings_clear_data_desc': 'ลบข้อมูลที่เก็บไว้ของ WebView ทั้งหมด',
  'settings_clear_data_success': 'ล้างข้อมูลเรียบร้อยแล้ว',

  'settings_version': 'เวอร์ชันแอป',

  // Language
  'settings_language': 'ภาษา',
  'settings_language_desc': 'เลือกภาษาที่แสดงในแอป',
  'language_dialog_title': 'เลือกภาษา',
  'language_system_default': 'ค่าเริ่มต้นของระบบ',
  'language_thai': 'ไทย (Thai)',
  'language_english': 'English',

  // Actions
  'action_back': 'ย้อนกลับ',
  'action_home': 'หน้าหลัก',
  'action_reload': 'โหลดหน้าใหม่',
  'action_close': 'ปิด',
  'action_save': 'บันทึก',

  // Kiosk exit
  'kiosk_exit_title': 'ออกจากโหมดคีออสก์',
  'kiosk_exit_message': 'คุณต้องการออกจากแอปหรือไม่? การดำเนินการนี้จะปิดระบบหน้าขาย',
  'kiosk_exit_confirm': 'ออกจากแอป',
  'kiosk_exit_cancel': 'ยกเลิก',

  // Permissions
  'permission_camera_rationale': 'แอปต้องการสิทธิ์เข้าถึงกล้องเพื่อใช้งานฟังก์ชันสแกนและอัปโหลดรูปภาพในหน้าขาย',
  'permission_denied': 'สิทธิ์ถูกปฏิเสธ บางฟังก์ชันอาจไม่สามารถใช้งานได้',

  // Splash
  'splash_loading': 'กำลังเริ่มต้นระบบ…',

  // Diagnostics
  'diagnostics_title': 'ข้อมูลวินิจฉัย',
  'diagnostics_copy': 'คัดลอก',
  'diagnostics_copied': 'คัดลอกข้อมูลวินิจฉัยแล้ว',

  // Login
  'login_title': 'เข้าสู่ระบบ',
  'login_server_url': 'ที่อยู่เซิร์ฟเวอร์',
  'login_username': 'ชื่อผู้ใช้',
  'login_password': 'รหัสผ่าน',
  'login_button': 'เข้าสู่ระบบ',
  'login_support': 'ติดต่อฝ่ายสนับสนุนเพื่อขอสิทธิ์การใช้งาน',
};
