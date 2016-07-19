package cl.monsoon.s1next.view.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import cl.monsoon.s1next.App;
import cl.monsoon.s1next.R;
import cl.monsoon.s1next.data.pref.DownloadPreferencesManager;

/**
 * An Activity includes download settings that allow users
 * to modify download features and behaviors such as cache
 * size and avatars/images download strategy.
 */
public final class DownloadPreferenceFragment extends BasePreferenceFragment {

    public static final String PREF_KEY_TOTAL_DOWNLOAD_CACHE_SIZE = "pref_key_download_total_cache_size";
    public static final String PREF_KEY_DOWNLOAD_AVATARS_STRATEGY = "pref_key_download_avatars_strategy";
    public static final String PREF_KEY_AVATAR_RESOLUTION_STRATEGY = "pref_key_avatar_resolution_strategy";
    public static final String PREF_KEY_AVATAR_CACHE_INVALIDATION_INTERVAL = "pref_key_avatar_cache_invalidation_interval";
    public static final String PREF_KEY_DOWNLOAD_IMAGES_STRATEGY = "pref_key_download_images_strategy";

    private DownloadPreferencesManager mDownloadPreferencesManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_download);
        mDownloadPreferencesManager = App.getAppComponent(getActivity())
                .getDownloadPreferencesManager();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PREF_KEY_DOWNLOAD_AVATARS_STRATEGY:
                mDownloadPreferencesManager.invalidateAvatarsDownloadStrategy();

                break;
            case PREF_KEY_AVATAR_RESOLUTION_STRATEGY:
                mDownloadPreferencesManager.invalidateAvatarsResolutionStrategy();

                break;
            case PREF_KEY_AVATAR_CACHE_INVALIDATION_INTERVAL:
                mDownloadPreferencesManager.invalidateAvatarsCacheInvalidationInterval();

                break;
            case PREF_KEY_DOWNLOAD_IMAGES_STRATEGY:
                mDownloadPreferencesManager.invalidateImagesDownloadStrategy();

                break;
            default:
                // fall through
        }
    }
}
