package com.megacast.castsdk.model;

import java.io.File;

/**
 * Created by Dmitry on 14.09.16.
 */
public class ImageDescription {

    public AbstractFile abstractFile;
    public String castUrl;
    public String sourceUrl;
    public File file;

    public String iconUrl;
    public String title;
    public String description;
    public String mimeType;

    public int width = -1, height = -1;
    private ServerConnection serverConnection;

    public ImageDescription(AbstractFile abstractFile, String iconUrl, String title, String description, String mimeType) {
        this.abstractFile = abstractFile;
        this.iconUrl = iconUrl;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
    }

    public ImageDescription(String imageUrl, String iconUrl, String title, String description, String mimeType) {
        this.sourceUrl = imageUrl;
        this.iconUrl = iconUrl;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
    }

    public ImageDescription(File file, String iconUrl, String title, String description, String mimeType) {
        this.file = file;
        this.iconUrl = iconUrl;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCastUrl(String castUrl) {
        this.castUrl = castUrl;
    }

    public String getCastUrl() {
        return castUrl;
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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFile(File imageFile) {
        this.file = imageFile;
    }

    public AbstractFile getAbstractFile() {
        return abstractFile;
    }

    public void setServerConnection(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }
}
