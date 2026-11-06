package org.godotengine.plugin.android.notification.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.godotengine.plugin.android.notification.R;
import org.json.JSONArray;
import org.json.JSONObject;

public class ShiroQuizWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "ShiroQuizWidget";

    public static final String ACTION_ANSWER = "org.godotengine.plugin.android.notification.widget.ACTION_ANSWER";
    public static final String ACTION_OPEN_APP = "org.godotengine.plugin.android.notification.widget.ACTION_OPEN_APP";
    public static final String EXTRA_OPTION_INDEX = "extra_option_index";

    private static final int[] OPTION_BUTTON_IDS = {
            R.id.btn_option_0,
            R.id.btn_option_1,
            R.id.btn_option_2,
            R.id.btn_option_3
    };

    private static final String[] OPTION_PREFIXES = {"A. ", "B. ", "C. ", "D. "};

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "Shiro Quiz Widget added to home screen!");
        WidgetDataManager.setPendingWidgetInstall(context, true);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.i(TAG, "All Shiro Quiz Widgets removed from home screen.");
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetDataManager.checkDailyRotation(context);
        for (int widgetId : appWidgetIds) {
            renderWidget(context, appWidgetManager, widgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        if (ACTION_ANSWER.equals(action)) {
            int optIdx = intent.getIntExtra(EXTRA_OPTION_INDEX, -1);
            if (optIdx >= 0) {
                WidgetDataManager.setTodayAnswered(context, optIdx);
                updateAllWidgets(context);
            }
        } else if (ACTION_OPEN_APP.equals(action)) {
            openApp(context);
        }
    }

    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName component = new ComponentName(context, ShiroQuizWidgetProvider.class);
            int[] widgetIds = manager.getAppWidgetIds(component);
            for (int id : widgetIds) {
                renderWidget(context, manager, id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating all widgets: " + e.getMessage());
        }
    }

    private static void renderWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        try {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.shiro_quiz_widget);

            JSONObject question = WidgetDataManager.getCurrentDailyQuestion(context);

            if (question == null) {
                views.setViewVisibility(R.id.layout_empty, View.VISIBLE);
                views.setViewVisibility(R.id.tv_question, View.GONE);
                views.setViewVisibility(R.id.layout_options, View.GONE);
                views.setViewVisibility(R.id.layout_feedback, View.GONE);

                PendingIntent pOpen = createOpenAppPendingIntent(context, widgetId * 100);
                views.setOnClickPendingIntent(R.id.btn_empty_open, pOpen);
                appWidgetManager.updateAppWidget(widgetId, views);
                return;
            }

            views.setViewVisibility(R.id.layout_empty, View.GONE);

            String topic = question.optString("topic", "Grammar");

            // Extract question text with multiple fallback keys
            String qText = question.optString("question", "");
            if (qText.isEmpty()) {
                qText = question.optString("sentence_with_blank", "");
            }
            if (qText.isEmpty()) {
                qText = question.optString("text", "");
            }
            if (qText.isEmpty()) {
                qText = question.optString("sentence", "");
            }
            if (qText.isEmpty()) {
                qText = question.optString("prompt", "");
            }
            if (qText.isEmpty()) {
                qText = "Latihan kuis hari ini:";
            }

            Log.i(TAG, "renderWidget: id=" + widgetId + " topic=" + topic + " qText=" + qText);

            String answer = question.optString("answer", "").trim();
            String rule = question.optString("rule", "").trim();
            JSONArray options = question.optJSONArray("options");
            if (options == null) {
                options = new JSONArray();
            }

            views.setTextViewText(R.id.tv_topic, topic);
            views.setTextViewText(R.id.tv_question, qText);
            views.setViewVisibility(R.id.tv_question, View.VISIBLE);

            boolean answered = WidgetDataManager.isTodayAnswered(context);
            int selectedOpt = WidgetDataManager.getTodaySelectedOption(context);

            if (!answered) {
                // Not answered yet: Show options, hide feedback
                views.setViewVisibility(R.id.layout_options, View.VISIBLE);
                views.setViewVisibility(R.id.layout_feedback, View.GONE);

                for (int i = 0; i < OPTION_BUTTON_IDS.length; i++) {
                    int btnId = OPTION_BUTTON_IDS[i];
                    if (i < options.length()) {
                        views.setViewVisibility(btnId, View.VISIBLE);
                        String optText = options.optString(i, "");
                        views.setTextViewText(btnId, OPTION_PREFIXES[i] + optText);
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_option_default);

                        Intent answerIntent = new Intent(context, ShiroQuizWidgetProvider.class);
                        answerIntent.setAction(ACTION_ANSWER);
                        answerIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
                        answerIntent.putExtra(EXTRA_OPTION_INDEX, i);
                        PendingIntent pAnswer = PendingIntent.getBroadcast(
                                context,
                                widgetId * 100 + i,
                                answerIntent,
                                getPendingIntentFlags()
                        );
                        views.setOnClickPendingIntent(btnId, pAnswer);
                    } else {
                        views.setViewVisibility(btnId, View.GONE);
                    }
                }
            } else {
                // Answered: Hide options to fit perfectly without overflow, show feedback + Open App button
                views.setViewVisibility(R.id.layout_options, View.GONE);
                views.setViewVisibility(R.id.layout_feedback, View.VISIBLE);

                String selectedText = (selectedOpt >= 0 && selectedOpt < options.length()) ? options.optString(selectedOpt, "").trim() : "";
                boolean isCorrect = !answer.isEmpty() && selectedText.equalsIgnoreCase(answer);

                if (isCorrect) {
                    views.setTextViewText(R.id.tv_feedback_title, "Jawaban Benar! (" + answer + ")");
                    views.setTextColor(R.id.tv_feedback_title, 0xFF4CAF50);
                } else {
                    views.setTextViewText(R.id.tv_feedback_title, "Jawabanmu: " + selectedText + " • Kunci: " + answer);
                    views.setTextColor(R.id.tv_feedback_title, 0xFFF44336);
                }

                String explanation = rule.isEmpty() ? "Buka Shiro untuk belajar lebih banyak!" : rule;
                views.setTextViewText(R.id.tv_rule, explanation);

                // Button: Open App
                PendingIntent pOpen = createOpenAppPendingIntent(context, widgetId * 100 + 1);
                views.setOnClickPendingIntent(R.id.btn_open_app, pOpen);
            }

            appWidgetManager.updateAppWidget(widgetId, views);
        } catch (Throwable t) {
            Log.e(TAG, "renderWidget failed for widgetId " + widgetId, t);
        }
    }

    private static PendingIntent createOpenAppPendingIntent(Context context, int requestCode) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) {
            intent = new Intent(Intent.ACTION_VIEW);
        }
        intent.setData(Uri.parse("shiro://widget/daily"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                getPendingIntentFlags()
        );
    }

    private static void openApp(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            intent.setData(Uri.parse("shiro://widget/daily"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        }
    }

    private static int getPendingIntentFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.FLAG_UPDATE_CURRENT;
    }
}
