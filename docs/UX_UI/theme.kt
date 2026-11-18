package com.example.healthapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.healthapp.R

// Настройка провайдера Google Fonts
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Шрифты для заголовков и крупных цифр (метрики здоровья)
val GoogleSansFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Google Sans"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Google Sans"), fontProvider = provider, weight = FontWeight.Bold)
)

// Шрифты для интерфейса, кнопок и основного текста
val GoogleSansTextFontFamily = FontFamily(
    Font(googleFont = GoogleFont("Google Sans Text"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Google Sans Text"), fontProvider = provider, weight = FontWeight.Medium)
)

// Полная конфигурация Typography по правилам Google Health (M3)
val HealthTypography = Typography(
    // Крупные цифры (например, количество шагов: "10 342")
    displayLarge = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    // Второстепенные метрики (например, пульс: "72 bpm")
    displaySmall = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    // Главные заголовки экранов ("Метрики здоровья", "Сегодня")
    headlineLarge = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    // Заголовки карточек разделов ("Пульс", "История активности")
    titleLarge = TextStyle(
        fontFamily = GoogleSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // Пункты меню и важные подзаголовки
    titleMedium = TextStyle(
        fontFamily = GoogleSansTextFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    // Основной читаемый текст и инсайты
    bodyLarge = TextStyle(
        fontFamily = GoogleSansTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Второстепенный текст ("Обновлено 2 минуты назад")
    bodyMedium = TextStyle(
        fontFamily = GoogleSansTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    // Текст на кнопках ("Начать тренировку")
    labelLarge = TextStyle(
        fontFamily = GoogleSansTextFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Мелкие подписи к графикам и таймстампы
    labelSmall = TextStyle(
        fontFamily = GoogleSansTextFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
