package com.megacast.castsdk.model;


import java.io.File;

public class SubtitleDescription {

    public static final int SUBTITLE_UNKNOWN = -1;
    public static final int SUBTITLE_SRT = 0;
    public static final int SUBTITLE_WEB_VTT = 1;
    public static final int SUBTITLE_SAMI = 2;

    private String castUrl;
    private String sourceUrl;
    private File file;

    private String mimeType;
    private String language;
    private String label;

    private Integer type;

    public SubtitleDescription(String sourceUrl, String mimeType, String language, String label, int type) {
        this.sourceUrl = sourceUrl;
        this.mimeType = mimeType;
        this.language = language;
        this.label = label;
        this.type = type;
    }

    public SubtitleDescription(File file, String mimeType, String language, String label, int type) {
        this.file = file;
        this.mimeType = mimeType;
        this.language = language;
        this.label = label;
        this.type = type;
    }

    public SubtitleDescription(SubtitleDescription description) {
        this.file = description.getFile();
        this.mimeType = description.getMimeType();
        this.language = description.getLanguage();
        this.label = description.getLabel();
        this.type = description.getType();
    }

    public int getType() {
        if (type != null) {
            return type;
        } else {
            switch (mimeType) {
                case "application/x-subrip":
                    return SUBTITLE_SRT;
                case "text/vtt":
                    return SUBTITLE_WEB_VTT;
            }
            return SUBTITLE_UNKNOWN;
        }
    }

    public File getFile() {
        return file;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCastUrl() {
        return castUrl;
    }

    public void setCastUrl(String castUrl) {
        this.castUrl = castUrl;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
