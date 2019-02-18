package com.github.hiteshsondhi88.libffmpeg;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;

import static com.github.hiteshsondhi88.libffmpeg.FileUtils.getFilesDirectory;

class FFmpegLoadLibraryAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private static final String LOG_TAG = FFmpegLoadLibraryAsyncTask.class.getSimpleName();
    public static File FILES_DIR;
    private final String cpuArchNameFromAssets;
    private final FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler;
    private final Context context;

    FFmpegLoadLibraryAsyncTask(Context context, String cpuArchNameFromAssets, FFmpegLoadBinaryResponseHandler ffmpegLoadBinaryResponseHandler) {
        this.context = context;
        this.cpuArchNameFromAssets = cpuArchNameFromAssets;
        this.ffmpegLoadBinaryResponseHandler = ffmpegLoadBinaryResponseHandler;

        FILES_DIR = getFilesDirectory(context);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        File ffmpegFile = new File(FileUtils.getFFmpeg(context));
        File fontconfigDir = new File(getFilesDirectory(context), "fonts" + File.separator + "fonts" + File.separator + "truetype");

        if (fontconfigDir.exists() && ffmpegFile.exists() && isDeviceFFmpegVersionOld() && !ffmpegFile.delete()) {
            return false;
        }
        if (!ffmpegFile.exists()) {
            boolean isFileCopied = FileUtils.copyBinaryFromAssetsToData(context,
                    cpuArchNameFromAssets + File.separator + FileUtils.ffmpegFileName,
                    FileUtils.ffmpegFileName);

            if (!fontconfigDir.exists()) {
                fontconfigDir.mkdirs();
            }

            boolean isFontsCopied = FileUtils.copyBinaryFromAssetsToData(context,
                    "fonts.conf", "fonts.conf");

            Log.d("isLibCopied " + isFontsCopied);

            boolean isRokuPresetCopied = FileUtils.copyBinaryFromAssetsToData(context,
                    "libx264-roku.ffpreset", "libx264-roku.ffpreset");

            Log.d("isRokuPresetCopied " + isRokuPresetCopied);

            // make file executable
            if (isFileCopied) {
                if (!ffmpegFile.canExecute()) {
                    Log.d("FFmpeg is not executable, trying to make it executable ...");
                    if (ffmpegFile.setExecutable(true)) {
                        return true;
                    }
                } else {
                    Log.d("FFmpeg is executable");
                    return true;
                }
            }
        }
        return ffmpegFile.exists() && ffmpegFile.canExecute();
    }

    @Override
    protected void onPostExecute(Boolean isSuccess) {
        super.onPostExecute(isSuccess);
        if (ffmpegLoadBinaryResponseHandler != null) {
            if (isSuccess) {
                ffmpegLoadBinaryResponseHandler.onSuccess();
            } else {
                ffmpegLoadBinaryResponseHandler.onFailure();
            }
            ffmpegLoadBinaryResponseHandler.onFinish();
        }
    }

    private boolean isDeviceFFmpegVersionOld() {
        return CpuArch.fromString(FileUtils.SHA1(FileUtils.getFFmpeg(context))).equals(CpuArch.NONE);
    }
}
