package com.example.opencv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.LocaleList;
import android.util.ArrayMap;
import java.util.Locale;
import java.util.Map;

public class LanguageManager {
    private static final String SP_NAME = "app_language_config";
    private static final String KEY_SELECTED_LANG = "selected_language_code";
    private static final String DEFAULT_LANG = "zh"; // 默认中文

    // 仅支持中英文
    private static final Map<String, Integer> SUPPORT_LANGUAGES = new ArrayMap<>();
    static {
        SUPPORT_LANGUAGES.put("zh", R.string.language_zh);
        SUPPORT_LANGUAGES.put("en", R.string.language_en);
    }

    public static Context initLanguage(Context context) {
        Locale targetLocale = getSelectedLocale(context);
        return updateContextLocale(context, targetLocale);
    }

    public static void switchLanguage(Context context, String languageCode, Runnable callback) {
        getSP(context).edit().putString(KEY_SELECTED_LANG, languageCode).apply();
        updateContextLocale(context, getLocaleByCode(languageCode));
        if (callback != null) {
            callback.run();
        }
    }

    public static Locale getSelectedLocale(Context context) {
        String langCode = getSP(context).getString(KEY_SELECTED_LANG, DEFAULT_LANG);
        return getLocaleByCode(langCode);
    }

    public static String getSelectedLanguageCode(Context context) {
        return getSP(context).getString(KEY_SELECTED_LANG, DEFAULT_LANG);
    }

    private static Locale getLocaleByCode(String code) {
        switch (code) {
            case "zh":
                // 关键修改：强制返回zh_CN（MIUI/VIVO唯一稳定识别的中文编码）
                return new Locale("zh", "CN");
            case "en":
                return Locale.ENGLISH; // 英文逻辑不变
            default:
                return new Locale("zh", "CN"); // 兜底也强制zh_CN
        }
    }

    // 补充：增强updateContextLocale方法，强制清空布局缓存
    @SuppressWarnings("deprecation")
    private static Context updateContextLocale(Context context, Locale targetLocale) {
        Context newContext = context;
        android.content.res.Resources resources = newContext.getResources();
        android.content.res.Configuration config = new android.content.res.Configuration(resources.getConfiguration());

        // 新增：强制清空布局缓存（击穿MIUI/VIVO的磁盘缓存）
        resources.flushLayoutCache();
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // 原有Locale设置逻辑
        Locale.setDefault(targetLocale);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocale(targetLocale);
            config.setLocales(new android.os.LocaleList(targetLocale));
            newContext = newContext.createConfigurationContext(config);
        } else {
            config.locale = targetLocale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
        }
        return newContext;
    }

//    private static Locale getLocaleByCode(String code) {
//        switch (code) {
//            case "zh":
//                // 原有代码：return Locale.SIMPLIFIED_CHINESE;
//                return new Locale("zh", "CN"); // 仅改这1行，强制返回zh_CN（MIUI唯一稳定识别的中文编码）
//            case "en":
//                return Locale.ENGLISH; // 英文逻辑不动
//            default:
//                return new Locale("zh", "CN"); // 兜底也改为zh_CN
//        }
//    }
//
//    @SuppressWarnings("deprecation")
//    private static Context updateContextLocale(Context context, Locale targetLocale) {
//        Context newContext = context;
//        android.content.res.Resources resources = newContext.getResources();
//        android.content.res.Configuration config = resources.getConfiguration();
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            config.setLocale(targetLocale);
//            config.setLocales(new LocaleList(targetLocale));
//            newContext = newContext.createConfigurationContext(config);
//        } else {
//            config.locale = targetLocale;
//            resources.updateConfiguration(config, resources.getDisplayMetrics());
//        }
//        return newContext;
//    }

    private static SharedPreferences getSP(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    public static Map<String, Integer> getSupportLanguages() {
        return SUPPORT_LANGUAGES;
    }
}