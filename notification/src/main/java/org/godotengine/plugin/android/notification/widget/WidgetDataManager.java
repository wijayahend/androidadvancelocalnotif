package org.godotengine.plugin.android.notification.widget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class WidgetDataManager {
    private static final String TAG = "ShiroWidgetDataManager";
    private static final String PREF_NAME = "shiro_widget_prefs";
    private static final String KEY_QUESTIONS_POOL = "questions_pool";

    // Daily rotation keys
    private static final String KEY_DAILY_DATE = "daily_date";
    private static final String KEY_TODAY_QUESTION_INDEX = "today_q_idx";
    private static final String KEY_YESTERDAY_QUESTION_INDEX = "yesterday_q_idx";
    private static final String KEY_TODAY_ANSWERED = "today_answered";
    private static final String KEY_TODAY_SELECTED_OPT = "today_selected_opt";
    private static final String KEY_WIDGET_PENDING_INSTALL = "widget_pending_install";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized void saveQuestionsPool(Context context, String jsonArrayString) {
        if (jsonArrayString == null || jsonArrayString.trim().isEmpty()) {
            return;
        }
        try {
            JSONArray arr = new JSONArray(jsonArrayString);
            if (arr.length() > 0) {
                SharedPreferences prefs = getPrefs(context);
                boolean wasEmpty = prefs.getString(KEY_QUESTIONS_POOL, "").isEmpty();
                
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_QUESTIONS_POOL, jsonArrayString);

                int curIdx = prefs.getInt(KEY_TODAY_QUESTION_INDEX, 0);
                if (curIdx >= arr.length() || wasEmpty) {
                    editor.putInt(KEY_TODAY_QUESTION_INDEX, 0);
                }

                if (wasEmpty || prefs.getString(KEY_DAILY_DATE, "").isEmpty()) {
                    editor.putString(KEY_DAILY_DATE, getTodayDateString());
                    editor.putBoolean(KEY_TODAY_ANSWERED, false);
                    editor.putInt(KEY_TODAY_SELECTED_OPT, -1);
                }

                editor.apply();
                Log.d(TAG, "Saved questions pool successfully (" + arr.length() + " items).");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed parsing questions JSON: " + e.getMessage());
        }
    }

    public static synchronized JSONArray getQuestionsPool(Context context) {
        String json = getPrefs(context).getString(KEY_QUESTIONS_POOL, null);
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new JSONArray();
        }
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static synchronized void checkDailyRotation(Context context) {
        String today = getTodayDateString();
        String savedDate = getPrefs(context).getString(KEY_DAILY_DATE, "");

        if (!today.equals(savedDate)) {
            JSONArray pool = getQuestionsPool(context);
            if (pool.length() > 0) {
                int lastIdx = getPrefs(context).getInt(KEY_TODAY_QUESTION_INDEX, -1);
                int nextIdx = 0;
                if (pool.length() > 1) {
                    Random random = new Random();
                    do {
                        nextIdx = random.nextInt(pool.length());
                    } while (nextIdx == lastIdx);
                }

                getPrefs(context).edit()
                        .putString(KEY_DAILY_DATE, today)
                        .putInt(KEY_YESTERDAY_QUESTION_INDEX, lastIdx)
                        .putInt(KEY_TODAY_QUESTION_INDEX, nextIdx)
                        .putBoolean(KEY_TODAY_ANSWERED, false)
                        .putInt(KEY_TODAY_SELECTED_OPT, -1)
                        .apply();
                Log.d(TAG, "Daily rotation: New day " + today + ". Selected question index: " + nextIdx);
            }
        }
    }

    public static synchronized JSONObject getCurrentDailyQuestion(Context context) {
        checkDailyRotation(context);
        JSONArray pool = getQuestionsPool(context);
        if (pool.length() == 0) {
            return null;
        }
        int idx = getPrefs(context).getInt(KEY_TODAY_QUESTION_INDEX, 0);
        if (idx < 0 || idx >= pool.length()) {
            idx = 0;
        }
        try {
            return pool.getJSONObject(idx);
        } catch (JSONException e) {
            return null;
        }
    }

    public static synchronized boolean isTodayAnswered(Context context) {
        checkDailyRotation(context);
        return getPrefs(context).getBoolean(KEY_TODAY_ANSWERED, false);
    }

    public static synchronized int getTodaySelectedOption(Context context) {
        return getPrefs(context).getInt(KEY_TODAY_SELECTED_OPT, -1);
    }

    public static synchronized void setTodayAnswered(Context context, int selectedIndex) {
        getPrefs(context).edit()
                .putBoolean(KEY_TODAY_ANSWERED, true)
                .putInt(KEY_TODAY_SELECTED_OPT, selectedIndex)
                .apply();
    }

    private static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        return sdf.format(new Date());
    }

    public static void setPendingWidgetInstall(Context context, boolean pending) {
        if (context != null) {
            getPrefs(context).edit().putBoolean(KEY_WIDGET_PENDING_INSTALL, pending).apply();
        }
    }

    public static boolean hasPendingWidgetInstall(Context context) {
        if (context != null) {
            return getPrefs(context).getBoolean(KEY_WIDGET_PENDING_INSTALL, false);
        }
        return false;
    }

    public static void consumePendingWidgetInstall(Context context) {
        setPendingWidgetInstall(context, false);
    }
}
