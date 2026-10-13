package io.github.zensu357.camswap;

import android.content.Context;
import android.os.Bundle;

import java.io.File;

import io.github.zensu357.camswap.utils.LogUtil;
import io.github.zensu357.camswap.utils.VideoManager;

public final class ControlActionHelper {
    private ControlActionHelper() {
    }

    public static boolean switchVideo(Context context, boolean next) {
        String method = next ? IpcContract.METHOD_NEXT : IpcContract.METHOD_PREV;
        try {
            Bundle result = context.getContentResolver().call(IpcContract.CONTENT_URI, method, null, null);
            if (result != null) {
                return result.getBoolean(IpcContract.EXTRA_CHANGED, false);
            }
        } catch (Throwable t) {
            LogUtil.log("【CS】ControlActionHelper provider switch failed: " + t);
        }
        return fallbackSwitchVideo(context, next);
    }

    public static int rotateVideo(Context context) {
        ConfigManager configManager = new ConfigManager();
        configManager.setContext(context);
        configManager.forceReload();
        int rotation = (configManager.getInt(ConfigManager.KEY_VIDEO_ROTATION_OFFSET, 0) + 90) % 360;
        configManager.setInt(ConfigManager.KEY_VIDEO_ROTATION_OFFSET, rotation);
        return rotation;
    }

    public static void setOverlayEnabled(Context context, boolean enabled) {
        ConfigManager configManager = new ConfigManager();
        configManager.setContext(context);
        configManager.setBoolean(ConfigManager.KEY_OVERLAY_CONTROL_ENABLED, enabled);
    }

    public static boolean toggleOriginalCamera(Context context) {
        ConfigManager config = new ConfigManager();
        config.setContext(context);
        config.forceReload();
        boolean original = !config.getBoolean(ConfigManager.KEY_USE_ORIGINAL_CAMERA, false);
        config.setBoolean(ConfigManager.KEY_USE_ORIGINAL_CAMERA, original);
        return original;
    }

    private static boolean fallbackSwitchVideo(Context context, boolean next) {
        ConfigManager configManager = new ConfigManager();
        configManager.setContext(context);
        File[] files = VideoManager.listVideoFiles(new File(ConfigManager.DEFAULT_CONFIG_DIR));
        if (files == null || files.length == 0) {
            return false;
        }

        String selectedVideo = configManager.getString(ConfigManager.KEY_SELECTED_VIDEO, null);
        int currentIndex = -1;
        if (selectedVideo != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().equals(selectedVideo)) {
                    currentIndex = i;
                    break;
                }
            }
        }

        int newIndex = currentIndex == -1 ? 0
                : (next ? (currentIndex + 1) % files.length : (currentIndex - 1 + files.length) % files.length);
        configManager.setString(ConfigManager.KEY_SELECTED_VIDEO, files[newIndex].getName());
        return true;
    }
}
