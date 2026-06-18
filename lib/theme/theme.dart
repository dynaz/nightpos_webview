import 'package:flutter/material.dart';

class NightPOSColors {
  // Primary colors (matching Android theme)
  static const Color nightBlack = Color(0xFF0A0A0A);
  static const Color neonPurple = Color(0xFFB84CE8);
  static const Color errorRed = Color(0xFFFF4444);
  static const Color textSecondary = Color(0xFF999999);

  // Background and surface
  static const Color background = nightBlack;
  static const Color surface = Color(0xFF1A1A1A);
  static const Color surfaceVariant = Color(0xFF2A2A2A);

  // Text colors
  static const Color textPrimary = Colors.white;
  static const Color textTertiary = Color(0xFF666666);

  // Semantic colors
  static const Color success = Color(0xFF4CAF50);
  static const Color warning = Color(0xFFFFC107);
  static const Color info = Color(0xFF2196F3);
}

class NightPOSTheme {
  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      primaryColor: NightPOSColors.neonPurple,
      scaffoldBackgroundColor: NightPOSColors.nightBlack,
      colorScheme: ColorScheme.dark(
        primary: NightPOSColors.neonPurple,
        secondary: NightPOSColors.neonPurple,
        tertiary: NightPOSColors.neonPurple,
        error: NightPOSColors.errorRed,
        background: NightPOSColors.nightBlack,
        surface: NightPOSColors.surface,
        surfaceVariant: NightPOSColors.surfaceVariant,
        onBackground: NightPOSColors.textPrimary,
        onSurface: NightPOSColors.textPrimary,
        onError: Colors.white,
      ),
      // Text themes
      textTheme: const TextTheme(
        displayLarge: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 32,
          fontWeight: FontWeight.bold,
        ),
        displayMedium: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 28,
          fontWeight: FontWeight.bold,
        ),
        headlineSmall: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 24,
          fontWeight: FontWeight.bold,
        ),
        headlineMedium: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
        titleLarge: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.w600,
        ),
        titleMedium: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 16,
          fontWeight: FontWeight.w500,
        ),
        bodyLarge: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 16,
        ),
        bodyMedium: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 14,
        ),
        bodySmall: TextStyle(
          color: NightPOSColors.textSecondary,
          fontSize: 12,
        ),
        labelLarge: TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 12,
          fontWeight: FontWeight.w500,
        ),
      ),
      // AppBar theme
      appBarTheme: AppBarTheme(
        backgroundColor: NightPOSColors.nightBlack,
        foregroundColor: NightPOSColors.textPrimary,
        elevation: 0,
        centerTitle: false,
        titleTextStyle: const TextStyle(
          color: NightPOSColors.textPrimary,
          fontSize: 18,
          fontWeight: FontWeight.bold,
        ),
      ),
      // Button themes
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: NightPOSColors.neonPurple,
          foregroundColor: Colors.white,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: NightPOSColors.neonPurple,
        ),
      ),
      // Input decoration
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: NightPOSColors.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: NightPOSColors.surfaceVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: NightPOSColors.surfaceVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: NightPOSColors.neonPurple, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: NightPOSColors.errorRed),
        ),
        hintStyle: const TextStyle(color: NightPOSColors.textSecondary),
        labelStyle: const TextStyle(color: NightPOSColors.textPrimary),
      ),
    );
  }
}
