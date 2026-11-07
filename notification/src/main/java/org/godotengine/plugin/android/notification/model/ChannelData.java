//
// © 2024-present https://github.com/cengiz-pz
//

package org.godotengine.plugin.android.notification.model;

import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.godotengine.godot.Dictionary;

public class ChannelData {

	private static final String DATA_KEY_ID = "channel_id";
	private static final String DATA_KEY_ID_LEGACY = "id";
	private static final String DATA_KEY_NAME = "channel_name";
	private static final String DATA_KEY_NAME_LEGACY = "name";
	private static final String DATA_KEY_DESCRIPTION = "channel_description";
	private static final String DATA_KEY_DESCRIPTION_LEGACY = "description";
	private static final String DATA_KEY_IMPORTANCE = "channel_importance";
	private static final String DATA_KEY_IMPORTANCE_LEGACY = "importance";
	private static final String DATA_KEY_BADGE_ENABLED = "badge_enabled";

	private Dictionary data;

	public ChannelData(Dictionary data) {
		this.data = data;
	}

	public String getId() {
		if (data.containsKey(DATA_KEY_ID)) {
			return String.valueOf(data.get(DATA_KEY_ID));
		}
		return data.containsKey(DATA_KEY_ID_LEGACY) ? String.valueOf(data.get(DATA_KEY_ID_LEGACY)) : "";
	}

	public String getName() {
		if (data.containsKey(DATA_KEY_NAME)) {
			return String.valueOf(data.get(DATA_KEY_NAME));
		}
		return data.containsKey(DATA_KEY_NAME_LEGACY) ? String.valueOf(data.get(DATA_KEY_NAME_LEGACY)) : "";
	}

	public String getDescription() {
		if (data.containsKey(DATA_KEY_DESCRIPTION)) {
			return String.valueOf(data.get(DATA_KEY_DESCRIPTION));
		}
		return data.containsKey(DATA_KEY_DESCRIPTION_LEGACY) ? String.valueOf(data.get(DATA_KEY_DESCRIPTION_LEGACY)) : "";
	}

	public int getImportance() {
		Object val = data.containsKey(DATA_KEY_IMPORTANCE) ? data.get(DATA_KEY_IMPORTANCE) : data.get(DATA_KEY_IMPORTANCE_LEGACY);
		if (val instanceof Number) {
			return ((Number) val).intValue();
		}
		return NotificationManager.IMPORTANCE_DEFAULT;
	}

	public boolean getBadgeEnabled() {
		Object val = data.get(DATA_KEY_BADGE_ENABLED);
		if (val instanceof Boolean) {
			return (Boolean) val;
		}
		return true;
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public boolean validate() {
		if (!data.containsKey(DATA_KEY_IMPORTANCE) && !data.containsKey(DATA_KEY_IMPORTANCE_LEGACY)) {
			data.put(DATA_KEY_IMPORTANCE, NotificationManager.IMPORTANCE_DEFAULT);
		}

		if (!data.containsKey(DATA_KEY_BADGE_ENABLED)) {
			data.put(DATA_KEY_BADGE_ENABLED, true);
		}

		boolean hasId = data.containsKey(DATA_KEY_ID) || data.containsKey(DATA_KEY_ID_LEGACY);
		boolean hasName = data.containsKey(DATA_KEY_NAME) || data.containsKey(DATA_KEY_NAME_LEGACY);
		boolean hasDesc = data.containsKey(DATA_KEY_DESCRIPTION) || data.containsKey(DATA_KEY_DESCRIPTION_LEGACY);

		return hasId && hasName && hasDesc;
	}

}
