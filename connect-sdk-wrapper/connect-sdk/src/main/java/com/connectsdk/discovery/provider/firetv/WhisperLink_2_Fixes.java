package com.connectsdk.discovery.provider.firetv;

import com.mega.cast.utils.log.SmartLog;

import com.amazon.whisperlink.core.platform.PlatformCoreInitializer;
import com.amazon.whisperlink.core.platform.PlatformCoreManager;
import com.amazon.whisperlink.platform.PlatformManager;

import java.lang.reflect.Field;

/**
 * Created by Dmitry on 18.10.16.
 */

public class WhisperLink_2_Fixes {

    private static final String LOG_TAG = WhisperLink_2_Fixes.class.getSimpleName();

    public static void resetWhisperLink() {
        SmartLog.d(LOG_TAG, "resetWhisperLink ");

        try {
            Field field = PlatformManager.class.getDeclaredField("mPlatformManager");
            field.setAccessible(true);
            PlatformManager manager = (PlatformManager) field.get(null);
            if (manager != null) {
                SmartLog.d(LOG_TAG, "acquired PlatformManager ");
                PlatformCoreManager platformCoreManager = (PlatformCoreManager) manager;

                Field configField = platformCoreManager.getClass().getDeclaredField("config"); //NoSuchFieldException
                configField.setAccessible(true);
                PlatformCoreInitializer platformCoreInitializer = (PlatformCoreInitializer) configField.get(platformCoreManager); //IllegalAccessException

                if (platformCoreInitializer != null) {
                    SmartLog.d(LOG_TAG, "acquired platformCoreInitializer! Reset the PlatformCoreManager!");
                    platformCoreManager.doInitialization(platformCoreInitializer);
                }

            } else {
                SmartLog.e(LOG_TAG, "could not acquire manager ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            SmartLog.e(LOG_TAG, "Exception while resetting PlatformManager! " + ex.getMessage());
        }
    }

}
