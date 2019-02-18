package com.megacast.ffmpegtranscoding;

import android.content.Context;
import android.os.Environment;

import com.megacast.castsdk.providers.managers.transcoding.TranscodingManager;

import java.io.File;

public class FFmpegHelper {

    private Context context;

    private final String PRESET_ULTRA_FAST = "ultrafast";
    private final String PRESET_SUPER_FAST = "superfast";
    private final String PRESET_VERY_FAST = "veryfast";
    private final String PRESET_FASTER = "faster";
    private final String PRESET_FAST = "fast";
    private final String PRESET_MEDIUM = "medium";
    private final String PRESET_SLOW = "slow";
    private final String PRESET_SLOWER = "slower";
    private final String PRESET_VERY_SLOW = "veryslow";

    public FFmpegHelper(Context context) {
        this.context = context;
    }

    public static String addFdkAacConversion() {
        StringBuilder command = new StringBuilder();
        command.append("-c:a");
        command.append("\t");
        command.append("libfdk_aac");
        command.append("\t");
        command.append("-b:a");
        command.append("\t");
        command.append("48k");
        command.append("\t");
        command.append("-ac");
        command.append("\t");
        command.append("2");
        command.append("\t");
        return command.toString();
    }

    public File ffmpegDir() {
        File filesDir;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filesDir = context.getExternalFilesDir(null);
            if (filesDir == null) {
                filesDir = Environment.getExternalStorageDirectory();
            }
            if (filesDir == null) {
                filesDir = context.getFilesDir();
            }
        } else
            filesDir = context.getFilesDir();

        return filesDir;
    }

    public String getInformationCommand(File loadedFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(loadedFile.getAbsolutePath());
        command.append("\t");
        return command.toString();
    }

    public String getPresetByName(String inputFileName) {
        String preset = "Ultra Fast";
        preset = preset.replaceAll("\\s+", "");
        return preset.toLowerCase();
    }

    private String getPresetByCode(int videoPreset) {
        switch (videoPreset) {
            case TranscodingManager.PRESET_ULTRA_FAST:
                return PRESET_ULTRA_FAST;
            case TranscodingManager.PRESET_SUPER_FAST:
                return PRESET_SUPER_FAST;
            case TranscodingManager.PRESET_VERY_FAST:
                return PRESET_VERY_FAST;
            case TranscodingManager.PRESET_FASTER:
                return PRESET_FASTER;
            case TranscodingManager.PRESET_FAST:
                return PRESET_FAST;
            case TranscodingManager.PRESET_MEDIUM:
                return PRESET_MEDIUM;
            case TranscodingManager.PRESET_SLOW:
                return PRESET_SLOW;
            case TranscodingManager.PRESET_SLOWER:
                return PRESET_SLOWER;
            case TranscodingManager.PRESET_VERY_SLOW:
                return PRESET_VERY_SLOW;
            case TranscodingManager.PRESET_4K:
                return PRESET_ULTRA_FAST;
            default:
                return PRESET_ULTRA_FAST;
        }
    }

    public String getAudioExtractAndTranscodeCommand(String inputFileName, File outputFile) {
        StringBuilder command = new StringBuilder();
        command.append("-y");
        command.append("\t");
        command.append("-i");
        command.append("\t");
        command.append(inputFileName);
        command.append("\t");
        command.append("-vn");
        command.append("\t");
        command.append("-c:a");
        command.append("\t");
        command.append("libfdk_aac");
        command.append("\t");
        command.append("-b:a");
        command.append("\t");
        command.append("48k");
        command.append("\t");
        command.append("-ac");
        command.append("\t");
        command.append("2");
        command.append("\t");
        command.append(outputFile.getAbsoluteFile());
        command.append("\t");
        return command.toString();
    }

    public String getCreateHlsPlaylistCommand(String inputFileName, File outputFile,
                                              String path, String fileNameWithoutExtension,
                                              String subtitleFile,
                                              String[] streams, int videoPreset) {
        StringBuilder command = new StringBuilder();
        command.append("-y");
        command.append("\t");
        command.append("-i");
        command.append("\t");
        command.append(inputFileName);
        command.append("\t");

        if (subtitleFile != null) {
            command.append("-vf");
            command.append("\t");
            command.append("subtitles=" + subtitleFile);
            command.append("\t");
        }

        command.append("-c:v");
        command.append("\t");
        command.append("libx264");
        command.append("\t");

        command.append("-r");
        command.append("\t");
        command.append("24000/1001");
        command.append("\t");

        command.append("-g");
        command.append("\t");
        command.append("48");
        command.append("\t");
        command.append("-threads");
        command.append("\t");
        command.append("0");
        command.append("\t");
        command.append("-pix_fmt");
        command.append("\t");
        command.append("yuv420p");
        command.append("\t");
        command.append("-preset");
        command.append("\t");
        command.append(getPresetByCode(videoPreset));
        command.append("\t");

        if(videoPreset == TranscodingManager.PRESET_4K){
            command.append("-vf");
            command.append("\t");
            command.append("scale=1280:720");
            command.append("\t");
        }

        command.append(addFdkAacConversion());

        //  copy all streams
        if (streams == null || streams.length == 0) {
            command.append("-map");
            command.append("\t");
            command.append("0");
            command.append("\t");
        } else {
            for (int i = 0; i < streams.length; i++) {
                command.append("-map");
                command.append("\t");
                command.append(streams[i]);
                command.append("\t");
            }
        }

        command.append("-flags");
        command.append("\t");
        command.append("+global_header");
        command.append("\t");
        command.append("-f");
        command.append("\t");
        command.append("segment");
        command.append("\t");
        command.append("-segment_time");
        command.append("\t");
        command.append("6");
        command.append("\t");
        command.append("-segment_list_size");
        command.append("\t");
        command.append("0");
        command.append("\t");
        command.append("-segment_list");
        command.append("\t");
        command.append(outputFile.getAbsolutePath());
        command.append("\t");
        //-bsf:a aac_adtstoasc -b:a 48k -ac 2 out5.mp4
        command.append("-segment_format");
        command.append("\t");
        command.append("mpegts");
        command.append("\t");
        command.append(path + "/" + fileNameWithoutExtension + "_" + "%d.ts");
        command.append("\t");
        return command.toString();
    }

    public String getProperFileName(String fileName) {
        return fileName.replace('.', '_').replace(' ', '_')
                .replace("[", "").replace("]", "").replace("'", "")
                .replace("(", "_").replace(")", "_").replace("-", "_").replace("+", "_");
    }

    public String getVideoDurationCommand(String filePath) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(filePath);
        command.append("\t");
        command.append("2>&1");
        command.append("\t");
        command.append("|");
        command.append("\t");
        command.append("grep");
        command.append("\t");
        command.append("Duration");
        command.append("\t");
        command.append("|");
        command.append("\t");
        command.append("sed");
        command.append("\t");
        command.append("'s/Duration: \\(.*\\), start/\\1/g'");
        command.append("\t");

        return command.toString();
    }

    public String getVideoInfoCommand(String input) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(input);
        command.append("\t");

        return command.toString();
    }

    //ffmpeg -i input.wav -vn -ar 44100 -ac 2 -ab 192k -f mp3 output.mp3
    //TODO: Requires ffmpeg recompilation!
    public String getMp3ConvertCommand(String mediaFileName, File ffmpegOutputFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(mediaFileName);
        command.append("\t");
        command.append("-vn");
        command.append("\t");
        command.append("-ar");
        command.append("\t");
        command.append("44100");
        command.append("\t");
        command.append("-ac");
        command.append("\t");
        command.append("2");
        command.append("\t");
        command.append("-ab");
        command.append("\t");
        command.append("192k");
        command.append("\t");
        command.append("-f");
        command.append("\t");
        command.append("mp3");
        command.append("\t");
        command.append(ffmpegOutputFile);
        command.append("\t");
        return command.toString();
    }

    //ffmpeg -i input.flac -c:a libfdk_aac -b:a 128k output.m4a
    public String getM4AConvertCommand(String mediaFileName, File ffmpegOutputFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(mediaFileName);
        command.append("\t");
        command.append("-c:a");
        command.append("\t");
        command.append("libfdk_aac");
        command.append("\t");
        command.append("-b:a");
        command.append("\t");
        command.append("128k");
        command.append("\t");

        command.append("-map");
        command.append("\t");
        command.append("0:0");
        command.append("\t");

        command.append(ffmpegOutputFile);
        command.append("\t");
        return command.toString();
    }

    //ffmpeg -i input.flac -acodec libfaac –ab 128 -vcodec libx264 \ -vpre roku -crf 15 -threads 0 ~/outputFile.mp4
    public String getMP4ConvertCommand(String mediaFileName, File ffmpegOutputFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(mediaFileName);
        command.append("\t");
        command.append("-acodec");
        command.append("\t");
        command.append("libfdk_aac");
        command.append("\t");
        command.append("-b:a");
        command.append("\t");
        command.append("128k");
        ;
        command.append("\t");
        command.append("-vcodec");
        command.append("\t");
        command.append("libx264");
        command.append("\t");
        command.append("-vpre");
        command.append("\t");
        command.append("roku");
        command.append("\t");
        command.append("-crf");
        command.append("\t");
        command.append("15");
        command.append("\t");
        command.append("-threads");
        command.append("\t");
        command.append("0");
        command.append("\t");
        command.append(ffmpegOutputFile);
        command.append("\t");
        return command.toString();
    }

    /* ffmpeg -i input.flac -strict experimental -c:a aac -b:a 128k output.m4a */
    public String getM4AConvertCommandExperimental(String mediaFileName, File ffmpegOutputFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(mediaFileName);
        command.append("\t");
        command.append("-strict");
        command.append("\t");
        command.append("experimental");
        command.append("\t");
        command.append("-c:a");
        command.append("\t");
        command.append("aac");
        command.append("\t");
        command.append("-b:a");
        command.append("\t");
        command.append("128k");
        command.append("\t");
        command.append(ffmpegOutputFile);
        command.append("\t");
        return command.toString();
    }

    /* ffmpeg -i input.mp4 -c copy -metadata:s:v:0 rotate=90 output.mp4 */
    public String getRotationCommand(String inputFile, int rotation, String ffmpegOutputFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(inputFile);
        command.append("\t");
        command.append("-c");
        command.append("\t");
        command.append("copy");
        command.append("\t");
        command.append("-metadata:s:v:0");
        command.append("\t");
        command.append("rotate=" + rotation);
        command.append("\t");
        command.append(ffmpegOutputFile);
        command.append("\t");
        return command.toString();
    }

    /* ffmpeg -codecs */
    public String getCodecsCommand() {
        StringBuilder command = new StringBuilder();
        command.append("-codecs");
        command.append("\t");
        return command.toString();
    }

    /* ffmpeg -ss 0.5 -i inputfile.mp4 -t 1 -s 480x300 -f image2 imagefile.jpg */
    /* ffmpeg -i inputfile.mp4 -ss 00:00:10 -vframes 1 output.jpg */
    public String getThumbnailDecodingCommand(String inputFile, String outFile) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(inputFile);
        command.append("\t");
        command.append("-ss");
        command.append("\t");
        command.append("00:00:5");
        command.append("\t");
        command.append("-vframes");
        command.append("\t");
        command.append("1");
        command.append("\t");
        command.append(outFile);
        command.append("\t");
        return command.toString();
    }

    public String getThumbnailDecodingCommand(String inputFile, int segmentTimeSec, String outFileName) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(inputFile);
        command.append("\t");


        command.append("-c");
        command.append("\t");
        command.append("copy");
        command.append("\t");

        command.append("-map");
        command.append("\t");
        command.append("0");
        command.append("\t");

        command.append("-segment_time");
        command.append("\t");
        command.append(segmentTimeSec);
        command.append("\t");

        command.append("-f");
        command.append("\t");
        command.append("-segment");
        command.append("\t");

        command.append(outFileName + "%3d");
        command.append("\t");

        return command.toString();
    }

    public String getTranscodeAndSplitCommand(String inputFileName, String path, String fileNameWithoutExtension, int segmentSecondsMax, String subtitleFile) {
        StringBuilder command = new StringBuilder();
        command.append("-y");
        command.append("\t");
        command.append("-i");
        command.append("\t");
        command.append(inputFileName);
        command.append("\t");

        if (subtitleFile != null) {
            command.append("-vf");
            command.append("\t");
            command.append("subtitles=" + subtitleFile);
            command.append("\t");
        }

        command.append("-c:v");
        command.append("\t");
        command.append("libx264");
        command.append("\t");

        command.append("-r");
        command.append("\t");
        command.append("24000/1001");
        command.append("\t");

        command.append("-g");
        command.append("\t");
        command.append("48");
        command.append("\t");
        command.append("-threads");
        command.append("\t");
        command.append("0");
        command.append("\t");
        command.append("-pix_fmt");
        command.append("\t");
        command.append("yuv420p");
        command.append("\t");
        command.append("-preset");
        command.append("\t");
        command.append(getPresetByName(inputFileName));
        command.append("\t");
        command.append(addFdkAacConversion());

        command.append("-map");
        command.append("\t");
        command.append("0:0");
        command.append("\t");
        command.append("-map");
        command.append("\t");
        command.append("0:1");
        command.append("\t");
        command.append("-flags");
        command.append("\t");
        command.append("+global_header");
        command.append("\t");
        command.append("-f");
        command.append("\t");
        command.append("segment");
        command.append("\t");
        command.append("-segment_time");
        command.append("\t");
        command.append(segmentSecondsMax);
        command.append("\t");
        command.append("-segment_format");
        command.append("\t");
        command.append("mpegts");
        command.append("\t");
        command.append(path + "/" + fileNameWithoutExtension + "_" + "%d.ts");
        command.append("\t");
        return command.toString();
    }

    /* ffmpeg -i title00.mkv -vcodec mpeg2video -sameq -acodec copy -f vob -copyts video.mpg */
    public String getFullTranscodingCommandMPEG(String inputFileName, File outFile, String subtitleFile, int videoPreset) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(inputFileName);
        command.append("\t");

        command.append("-vcodec");
        command.append("\t");
        command.append("mpeg2video");
        command.append("\t");

        command.append("-qscale");
        command.append("\t");
        command.append("0");
        command.append("\t");

        command.append("-c:a");
        command.append("\t");
        command.append("libfdk_aac");
        command.append("\t");

        command.append("-f");
        command.append("\t");
        command.append("vob");
        command.append("\t");

        command.append("-copyts");
        command.append("\t");

//        command.append("-c:v");
//        command.append("\t");
//        command.append("libx264");
//        command.append("\t");

        command.append(outFile);
        command.append("\t");
        return command.toString();
    }

    /* ffmpeg -i input.avi -c:v libx264 -preset slow -c:a libfdk_aac -b:a 192k -ac 2 out.mp4 */
    /* ffmpeg -i input.avi -c:a libfdk_aac -b:a 128k -c:v libx264 -crf 23 output.mp4 */
    public String getFullTranscodingCommandMP4(String inputFileName, File outFile, String subtitleFile, int videoPreset) {
        StringBuilder command = new StringBuilder();
        command.append("-i");
        command.append("\t");
        command.append(inputFileName);
        command.append("\t");

        command.append("-c:a");
        command.append("\t");
        command.append("libfdk_aac");
        command.append("\t");

        command.append("-preset");
        command.append("\t");
        command.append(getPresetByCode(videoPreset));
        command.append("\t");

        command.append("-c:v");
        command.append("\t");
        command.append("libx264");
        command.append("\t");

        command.append(outFile);
        command.append("\t");
        return command.toString();
    }

    /*
    -y -i sample.mp4 -c:v libx264 -r 24000/1001 -g 48 -threads 0 -pix_fmt yuv420p -preset UltraFast -c:a libfdk_aac -b:a 48k -ac 2 -map 0:0 -map 0:1 -flags +global_header sample.ts
     */
    public String getFullTranscodingCommand(String inputFileName, File outFile, String subtitleFile, int videoPreset) {
        StringBuilder command = new StringBuilder();
        command.append("-y");
        command.append("\t");
        command.append("-i");
        command.append("\t");
        command.append(inputFileName);
        command.append("\t");

        if (subtitleFile != null) {
            command.append("-vf");
            command.append("\t");
            command.append("subtitles=" + subtitleFile);
            command.append("\t");
        }

        command.append("-c:v");
        command.append("\t");
        command.append("libx264");
        command.append("\t");

        command.append("-r");
        command.append("\t");
        command.append("24000/1001");
        command.append("\t");

        command.append("-g");
        command.append("\t");
        command.append("48");
        command.append("\t");
        command.append("-threads");
        command.append("\t");
        command.append("0");
        command.append("\t");
        command.append("-pix_fmt");
        command.append("\t");
        command.append("yuv420p");
        command.append("\t");
        command.append("-preset");
        command.append("\t");
        command.append(getPresetByCode(videoPreset));
        command.append("\t");

        if(videoPreset == TranscodingManager.PRESET_4K){
            command.append("-vf");
            command.append("\t");
            command.append("scale=1280:720");
            command.append("\t");
        }

        command.append(addFdkAacConversion());

        command.append("-map");
        command.append("\t");
        command.append("0:0");
        command.append("\t");
        command.append("-map");
        command.append("\t");
        command.append("0:1");
        command.append("\t");
        command.append("-flags");
        command.append("\t");
        command.append("+global_header");
        command.append("\t");
        command.append(outFile);
        command.append("\t");
        return command.toString();
    }

}
