package com.github.hiteshsondhi88.libffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.Map;

class ShellCommand {

    Process run(String[] commandString) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commandString, getEnv());
        } catch (IOException e) {
            Log.e("Exception while trying to run: " + commandString, e);
        }
        return process;
    }

    private static String[] getEnv() {
        Map<String, String> env = System.getenv();
        String[] envp = new String[env.size() + 2];
        int i = 0;
        for (Map.Entry<String, String> e : env.entrySet()) {
            Log.d("variable " + e.getKey() + "=" + e.getValue());
            envp[i++] = e.getKey() + "=" + e.getValue();
        }

        final String fontConfVar = "FONTCONFIG_FILE=" + FFmpegLoadLibraryAsyncTask.FILES_DIR + File.separator + "fonts.conf";
        final String dataDir = "FFMPEG_DATADIR=" + FFmpegLoadLibraryAsyncTask.FILES_DIR;
        envp[env.size()] = fontConfVar;
        envp[env.size()+1] = dataDir;
        Log.d("variable " + envp[env.size()]);

        return envp;
    }

    CommandResult runWaitFor(String[] s) {
        Process process = run(s);

        Integer exitValue = null;
        String output = null;
        try {
            if (process != null) {
                exitValue = process.waitFor();

                if (CommandResult.success(exitValue)) {
                    output = Util.convertInputStreamToString(process.getInputStream());
                } else {
                    output = Util.convertInputStreamToString(process.getErrorStream());
                }
            }
        } catch (InterruptedException e) {
            Log.e("Interrupt exception", e);
        } finally {
            Util.destroyProcess(process);
        }

        return new CommandResult(CommandResult.success(exitValue), output);
    }

}