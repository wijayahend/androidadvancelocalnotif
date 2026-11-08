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
    private static final String KEY_ANSWERED_QUESTION_TEXT = "answered_q_text";
    private static final String KEY_WIDGET_PENDING_INSTALL = "widget_pending_install";
    private static final String KEY_UI_LABELS = "ui_labels";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String extractQuestionText(JSONObject question) {
        if (question == null) return "";
        String[] keys = {"question", "sentence_with_blank", "text", "sentence", "prompt"};
        for (String k : keys) {
            String val = question.optString(k, "").trim();
            if (!val.isEmpty()) {
                return val;
            }
        }
        return "";
    }

    public static synchronized void resetAnsweredState(Context context) {
        if (context == null) return;
        getPrefs(context).edit()
                .putBoolean(KEY_TODAY_ANSWERED, false)
                .putInt(KEY_TODAY_SELECTED_OPT, -1)
                .putString(KEY_ANSWERED_QUESTION_TEXT, "")
                .apply();
    }

    public static synchronized void saveQuestionsPool(Context context, String jsonArrayString) {
        if (jsonArrayString == null || jsonArrayString.trim().isEmpty()) {
            return;
        }
        try {
            JSONArray arr = new JSONArray(jsonArrayString);
            if (arr.length() > 0) {
                SharedPreferences prefs = getPrefs(context);
                SharedPreferences.Editor editor = prefs.edit();
                JSONObject firstObj = arr.optJSONObject(0);
                if (firstObj != null && firstObj.has("ui_labels")) {
                    JSONObject labels = firstObj.optJSONObject("ui_labels");
                    if (labels != null) {
                        editor.putString(KEY_UI_LABELS, labels.toString());
                    }
                }
                if (firstObj != null && firstObj.optBoolean("_only_ui_labels", false)) {
                    editor.apply();
                    Log.d(TAG, "Updated widget UI labels only.");
                    return;
                }
                editor.putString(KEY_QUESTIONS_POOL, jsonArrayString);
                editor.putInt(KEY_TODAY_QUESTION_INDEX, 0);
                editor.putString(KEY_DAILY_DATE, getTodayDateString());
                editor.putBoolean(KEY_TODAY_ANSWERED, false);
                editor.putInt(KEY_TODAY_SELECTED_OPT, -1);
                editor.putString(KEY_ANSWERED_QUESTION_TEXT, "");
                editor.apply();
                Log.d(TAG, "Saved questions pool successfully (" + arr.length() + " items) and reset answered state.");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed parsing questions JSON: " + e.getMessage());
        }
    }

    public static synchronized JSONObject getUiLabels(Context context) {
        String raw = getPrefs(context).getString(KEY_UI_LABELS, null);
        if (raw != null && !raw.isEmpty()) {
            try {
                return new JSONObject(raw);
            } catch (JSONException ignored) {
            }
        }
        return new JSONObject();
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
                        .putString(KEY_ANSWERED_QUESTION_TEXT, "")
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
        SharedPreferences prefs = getPrefs(context);
        if (!prefs.getBoolean(KEY_TODAY_ANSWERED, false)) {
            return false;
        }
        JSONObject currentQ = getCurrentDailyQuestion(context);
        String currentQText = extractQuestionText(currentQ);
        String answeredQText = prefs.getString(KEY_ANSWERED_QUESTION_TEXT, "");
        if (!currentQText.isEmpty() && !currentQText.equals(answeredQText)) {
            prefs.edit()
                    .putBoolean(KEY_TODAY_ANSWERED, false)
                    .putInt(KEY_TODAY_SELECTED_OPT, -1)
                    .putString(KEY_ANSWERED_QUESTION_TEXT, "")
                    .apply();
            return false;
        }
        return true;
    }

    public static synchronized int getTodaySelectedOption(Context context) {
        return getPrefs(context).getInt(KEY_TODAY_SELECTED_OPT, -1);
    }

    public static synchronized void setTodayAnswered(Context context, int selectedIndex) {
        JSONObject currentQ = getCurrentDailyQuestion(context);
        String qText = extractQuestionText(currentQ);
        getPrefs(context).edit()
                .putBoolean(KEY_TODAY_ANSWERED, true)
                .putInt(KEY_TODAY_SELECTED_OPT, selectedIndex)
                .putString(KEY_ANSWERED_QUESTION_TEXT, qText)
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
