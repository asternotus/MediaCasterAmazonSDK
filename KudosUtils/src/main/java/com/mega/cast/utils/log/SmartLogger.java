package com.mega.cast.utils.log;

import android.support.annotation.Nullable;


/**
 * Logger interface for the {@link SmartLog}
 *
 * @author Artyom Krivolapov <amal.samally@gmail.com>
 * @since 27.09.2016
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface SmartLogger
{
    void logErrorOrThrow(@Nullable Object tag, @Nullable Throwable t, @Nullable String msg, @Nullable Object... args);

    void log(@Nullable Object tag, int priority, @Nullable String msg, @Nullable Throwable t, @Nullable Object... args);
}
