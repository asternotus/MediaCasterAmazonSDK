package com.mega.cast.utils.log;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.util.Log.ASSERT;
import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;


/**
 * Simple and effective logging helper.
 * <p/>
 * {@link SmartLog} is a static facade for logger access.
 * Assign realization that you want to use to the {@link SmartLog#logger} field before calling
 * log methods. For example you can use {AndroidSmartLogger} in android environment.
 * <p/>
 * You can use anything as a logging tag. Strings, any class (class name will be used)
 * or even null (class name will be determined via stacktrace). Throwable can be passed too,
 * it will be used as tag and as throwable.
 *
 * @author Artyom Krivolapov <amal.samally@gmail.com>
 * @see android.util.Log
 * @since 27.09.2016
 */
@SuppressWarnings({"unused", "StaticVariableMayNotBeInitialized", "OverloadedMethodsWithSameNumberOfParameters"})
public final class SmartLog {
    private SmartLog() {
    }


    /**
     * Put logger realization that you want to use to this field before calling log methods
     */
    public static SmartLogger logger;

    private static final boolean HIDE_LOG = false;

    /**
     * Logs error in release builds, throws exception in test builds
     */
    public static void errorOrThrow(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.logErrorOrThrow(tag, ex, msg, args);
    }

    /**
     * Logs error in release builds, throws exception in test builds
     */
    public static void errorOrThrow(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.logErrorOrThrow(tag, ex, null);
    }

    /**
     * Logs error in release builds, throws exception in test builds
     */
    public static void errorOrThrow(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.logErrorOrThrow(null, ex, null);
    }

    /**
     * Logs error in release builds, throws exception in test builds
     */
    public static void errorOrThrow(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.logErrorOrThrow(null, ex, msg, args);
    }

    /**
     * Logs error in release builds, throws exception in test builds
     */
    public static void errorOrThrow(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.logErrorOrThrow(tag, null, msg, args);
    }

    /**
     * Logs error in release builds, throws exception in test builds
     */
    public static void errorOrThrow(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.logErrorOrThrow(null, null, msg);
    }


    public static void e(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, ERROR, msg, ex, args);
        } else {
            Log.e(tag.toString(), msg, ex);
        }
    }

    public static void e(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, ERROR, msg, ex, args);
        } else {
            Log.e("", msg, ex);
        }
    }

    public static void e(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, ERROR, null, ex);
        } else {
            Log.e(tag.toString(), "", ex);
        }
    }

    public static void e(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, ERROR, null, ex);
        } else {
            Log.e("", "", ex);
        }
    }

    public static void e(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, ERROR, msg, null, args);
        } else {
            Log.e(tag.toString(), msg);
        }
    }

    public static void e(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, ERROR, msg, null);
        } else {
            Log.e("", msg);
        }
    }

    //  <editor-fold desc="w">

    public static void w(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, WARN, msg, ex, args);
        } else {
            Log.w(tag.toString(), msg, ex);
        }
    }

    public static void w(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, WARN, msg, ex, args);
        } else {
            Log.w("", msg, ex);
        }
    }

    public static void w(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, WARN, null, ex);
        } else {
            Log.w(tag.toString(), "", ex);
        }
    }

    public static void w(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, WARN, null, ex);
        } else {
            Log.w("", "", ex);
        }
    }

    public static void w(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, WARN, msg, null, args);
        } else {
            Log.w(tag.toString(), msg);
        }
    }

    public static void w(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, WARN, msg, null);
        } else {
            Log.w("", msg);
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="i">

    public static void i(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, INFO, msg, ex, args);
        } else {
            Log.i(tag.toString(), msg, ex);
        }
    }

    public static void i(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, INFO, msg, ex, args);
        } else {
            Log.i("", msg, ex);
        }
    }

    public static void i(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, INFO, null, ex);
        } else {
            Log.i(tag.toString(), "", ex);
        }
    }

    public static void i(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, INFO, null, ex);
        } else {
            Log.i("", "", ex);
        }
    }

    public static void i(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, INFO, msg, null, args);
        } else {
            Log.i(tag.toString(), msg);
        }
    }

    public static void i(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, INFO, msg, null);
        } else {
            Log.i("", msg);
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="d">

    public static void d(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, DEBUG, msg, ex, args);
        } else {
            Log.d(tag.toString(), msg, ex);
        }
    }

    public static void d(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, DEBUG, msg, ex, args);
        } else {
            Log.d("", msg, ex);
        }
    }

    public static void d(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, DEBUG, null, ex);
        } else {
            Log.d(tag.toString(), "", ex);
        }
    }

    public static void d(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, DEBUG, null, ex);
        } else {
            Log.d("", "", ex);
        }
    }

    public static void d(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, DEBUG, msg, null, args);
        } else {
            Log.d(tag.toString(), msg);
        }
    }

    public static void d(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, DEBUG, msg, null);
        } else {
            Log.d("", msg);
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="v">

    public static void v(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, VERBOSE, msg, ex, args);
        } else {
            Log.v(tag.toString(), msg, ex);
        }
    }

    public static void v(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, VERBOSE, msg, ex, args);
        } else {
            Log.v("", msg, ex);
        }
    }

    public static void v(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, VERBOSE, null, ex);
        } else {
            Log.v(tag.toString(), "", ex);
        }
    }

    public static void v(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, VERBOSE, null, ex);
        } else {
            Log.v("", "", ex);
        }
    }

    public static void v(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, VERBOSE, msg, null, args);
        } else {
            Log.v(tag.toString(), msg);
        }
    }

    public static void v(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, VERBOSE, msg, null);
        } else {
            Log.v("", msg);
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="wtf">

    public static void wtf(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, ASSERT, msg, ex, args);
        } else {
            Log.wtf(tag.toString(), msg, ex);
        }
    }

    public static void wtf(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, ASSERT, msg, ex, args);
        } else {
            Log.wtf("", msg, ex);
        }
    }

    public static void wtf(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, ASSERT, null, ex);
        } else {
            Log.wtf(tag.toString(), "", ex);
        }
    }

    public static void wtf(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, ASSERT, null, ex);
        } else {
            Log.wtf("", "", ex);
        }
    }

    public static void wtf(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, ASSERT, msg, null, args);
        } else {
            Log.wtf(tag.toString(), msg);
        }
    }

    public static void wtf(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, ASSERT, msg, null);
        } else {
            Log.wtf("", msg);
        }
    }

    //  </editor-fold>

    //  <editor-fold desc="tracker">

    public static void t(@Nullable final Object tag, @Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, INFO, msg, ex, args);
        } else {
            Log.i(tag.toString(), msg, ex);
        }

        ApplicationTracker.getInstance().sendMessage(tag + ": " + msg + " " + (ex != null ? ex : ""));
    }

    public static void t(@Nullable final Throwable ex, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, INFO, msg, ex, args);
        } else {
            Log.i("", msg, ex);
        }
        ApplicationTracker.getInstance().sendMessage("" + msg + " " + (ex != null ? ex : ""));
    }

    public static void t(@Nullable final Object tag, @NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, INFO, null, ex);
        } else {
            Log.i(tag.toString(), "", ex);
        }
        ApplicationTracker.getInstance().sendMessage(tag + ": " + (ex != null ? ex : ""));
    }

    public static void t(@NonNull final Throwable ex) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, INFO, null, ex);
        } else {
            Log.i("", "", ex);
        }
        ApplicationTracker.getInstance().sendMessage("" + (ex != null ? ex : ""));
    }

    public static void t(@Nullable final Object tag, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(tag, INFO, msg, null, args);
        } else {
            Log.i(tag.toString(), msg);
        }
        ApplicationTracker.getInstance().sendMessage(tag + ": " + msg);
    }

    public static void t(@NonNull final String msg) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) {
            log.log(null, INFO, msg, null);
        } else {
            Log.i("", msg);
        }
        ApplicationTracker.getInstance().sendMessage("" + msg);
    }

    //  </editor-fold>

    public static void log(@Nullable final Object tag, final int priority, @Nullable final String msg, @Nullable final Object... args) {
        if (HIDE_LOG) return;
        final SmartLogger log = logger;
        if (log != null) log.log(tag, priority, msg, null, args);
    }

    public static String getStackTraceString(Exception var24) {
        return Log.getStackTraceString(var24);
    }
}
