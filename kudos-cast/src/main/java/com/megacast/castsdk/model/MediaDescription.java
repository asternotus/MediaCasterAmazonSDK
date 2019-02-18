package com.megacast.castsdk.model;

import android.graphics.Bitmap;

import com.megacast.castsdk.model.constants.MediaTypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by Dmitry on 14.09.16.
 */
public class MediaDescription {

    public AbstractFile abstractFile;
    public String sourceUrl;
    public File file;

    public String castUrl;

    public String iconURL;
    public File iconFile;

    public String title;
    public String description;
    public String mimeType;
    public long duration;

    public JSONObject extras;

    public boolean onlyEmbeddedSubtitlesAllowed = false;
    private Bitmap coverImage;
    private ServerConnection serverConnection;

    public MediaDescription(AbstractFile abstractFile, String title, String description, String mimeType) {
        this.abstractFile = abstractFile;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
    }

    public MediaDescription(String url, String title, String description, String mimeType) {
        this.sourceUrl = url;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
    }

    public MediaDescription(File file, String title, String description, String mimeType) {
        this.file = file;
        this.title = title;
        this.description = description;
        this.mimeType = mimeType;
    }

    public MediaDescription(MediaDescription description) {
        this.sourceUrl = description.getSourceUrl();
        this.file = description.getFile();

        this.iconURL = description.getIconURL();
        this.iconFile = description.getIconFile();

        this.title = description.getTitle();
        this.description = description.getDescription();
        this.mimeType = description.getMimeType();
        this.duration = description.getDuration();
        this.extras = description.getExtras();

        this.coverImage = description.getCoverImage();
    }

    public boolean isPlaylist() {
        return mimeType.equals(MediaTypes.MIME_TYPE_PLAYLIST_X_MPEGURL);
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getIconURL() {
        return iconURL;
    }

    public void setIconURL(String iconURL) {
        this.iconURL = iconURL;
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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isOnlyEmbeddedSubtitlesAllowed() {
        return onlyEmbeddedSubtitlesAllowed;
    }

    public void setOnlyEmbeddedSubtitlesAllowed(boolean onlyEmbeddedSubtitlesAllowed) {
        this.onlyEmbeddedSubtitlesAllowed = onlyEmbeddedSubtitlesAllowed;
    }

    public void putExtra(String name, Object value) {
        if (extras == null) {
            this.extras = new JSONObject();
        }
        try {
            extras.put(name, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getExtras() {
        return extras;
    }

    public File getIconFile() {
        return iconFile;
    }

    public String getCastUrl() {
        return castUrl;
    }

    public void setCastUrl(String castUrl) {
        this.castUrl = castUrl;
    }

    public void setIconFile(File iconFile) {
        this.iconFile = iconFile;
    }

    public AbstractFile getAbstractFile() {
        return abstractFile;
    }

    public void setAbstractFile(AbstractFile abstractFile) {
        this.abstractFile = abstractFile;
    }

    public void setCoverImage(Bitmap coverImage) {
        this.coverImage = coverImage;
    }

    public Bitmap getCoverImage() {
        return coverImage;
    }

    public void setServerConnection(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }
}
