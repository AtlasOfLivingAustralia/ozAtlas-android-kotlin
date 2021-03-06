package language;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by sad038 on 8/11/17.
 */

public class LanguageManager {
    public static JSONObject languageJSON;

    /**
     * get the value of given key from language
     *
     * @param context
     * @param key
     * @param defaultRes from resource XML
     * @return
     */
    public static String localizedString(Context context, String key, int defaultRes) {
        if (languageJSON != null) {
            String value = key;
            try {
                value = languageJSON.getString(key);
            } catch (JSONException e) {
                value = key;
            } catch (Exception n) {
                Log.e("LanguageManager", n.getMessage());
            }
            return value;
        }
        return context.getString(defaultRes);
    }

    /**
     * get the value of given key from language
     *
     * @param key
     * @return
     */
    public static String localizedString(String key) {
        if (languageJSON != null) {
            String value = key;
            try {
                value = languageJSON.getString(key);
            } catch (JSONException e) {
                value = key;
            } catch (Exception n) {
                Log.e("LanguageManager", n.getMessage());
            }
            return value;
        }
        return key;
    }

    public static String getEnglishValue(String value) {
        if (languageJSON != null) {
            Iterator<?> keys = languageJSON.keys();

            while (keys.hasNext()) {
                try {
                    String key = (String) keys.next();
                    if (languageJSON.getString(key).equals(value))
                        return key;
                } catch (JSONException e) {
                    Log.e("LanguageManager", e.getMessage());
                } catch (Exception n) {
                    Log.e("LanguageManager", n.getMessage());
                }
            }
        }
        return value;
    }
}
