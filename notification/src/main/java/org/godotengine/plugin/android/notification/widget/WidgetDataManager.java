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
                getPrefs(context).edit().putString(KEY_QUESTIONS_POOL, jsonArrayString).apply();
                Log.d(TAG, "Saved questions pool successfully (" + arr.length() + " items).");

                // Ensure current index is within bounds of new pool
                int curIdx = getPrefs(context).getInt(KEY_TODAY_QUESTION_INDEX, 0);
                if (curIdx >= arr.length()) {
                    getPrefs(context).edit().putInt(KEY_TODAY_QUESTION_INDEX, 0).apply();
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed parsing questions JSON: " + e.getMessage());
        }
    }

    public static synchronized JSONArray getQuestionsPool(Context context) {
        String json = getPrefs(context).getString(KEY_QUESTIONS_POOL, null);
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return getDefaultPool();
        }
        try {
            JSONArray arr = new JSONArray(json);
            return arr.length() > 0 ? arr : getDefaultPool();
        } catch (JSONException e) {
            return getDefaultPool();
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

    private static JSONArray getDefaultPool() {
        JSONArray arr = new JSONArray();
        try {
            JSONObject q1 = new JSONObject();
            q1.put("topic", "Grammar");
            q1.put("question", "The first month of the year is ......");
            JSONArray o1 = new JSONArray();
            o1.put("August"); o1.put("January"); o1.put("March"); o1.put("May");
            q1.put("options", o1);
            q1.put("answer", "January");
            q1.put("rule", "Ordinal numbers: first -> January, second -> February");
            arr.put(q1);

            JSONObject q2 = new JSONObject();
            q2.put("topic", "Grammar");
            q2.put("question", "She ___ to school every day.");
            JSONArray o2 = new JSONArray();
            o2.put("go"); o2.put("goes"); o2.put("going"); o2.put("gone");
            q2.put("options", o2);
            q2.put("answer", "goes");
            q2.put("rule", "Simple Present: subjek he/she/it -> verb + s/es");
            arr.put(q2);

            JSONObject q3 = new JSONObject();
            q3.put("topic", "Grammar");
            q3.put("question", "___ you like coffee?");
            JSONArray o3 = new JSONArray();
            o3.put("Does"); o3.put("Do"); o3.put("Is"); o3.put("Are");
            q3.put("options", o3);
            q3.put("answer", "Do");
            q3.put("rule", "Yes/No Question: Do/Does + subject + verb (base form)");
            arr.put(q3);

            JSONObject q4 = new JSONObject();
            q4.put("topic", "Grammar");
            q4.put("question", "They ___ football in the park yesterday.");
            JSONArray o4 = new JSONArray();
            o4.put("plays"); o4.put("play"); o4.put("played"); o4.put("playing");
            q4.put("options", o4);
            q4.put("answer", "played");
            q4.put("rule", "Past Simple: verb-2 (played) untuk aksi di masa lalu.");
            arr.put(q4);

            JSONObject q5 = new JSONObject();
            q5.put("topic", "Grammar");
            q5.put("question", "I ___ never seen that movie before.");
            JSONArray o5 = new JSONArray();
            o5.put("has"); o5.put("have"); o5.put("had"); o5.put("having");
            q5.put("options", o5);
            q5.put("answer", "have");
            q5.put("rule", "Present Perfect: subjek I/you/they/we -> have + V3.");
            arr.put(q5);
        } catch (Exception ignored) {}
        return arr;
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
