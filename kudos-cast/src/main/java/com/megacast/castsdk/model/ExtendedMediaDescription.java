package com.megacast.castsdk.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dmitry on 27.10.16.
 */

public class ExtendedMediaDescription extends MediaDescription {

    private Map<String, String> videoStreamsMap;
    private Map<String, String> audioStreamsMap;

    private int rotation = 0;
    private Integer width;
    private Integer height;
    private boolean a4K;
    private Long bitrate;

    public ExtendedMediaDescription(MediaDescription description) {
        super(description);
        videoStreamsMap = new HashMap<>();
        audioStreamsMap = new HashMap<>();
    }

    public ExtendedMediaDescription(ExtendedMediaDescription description) {
        super(description);
        videoStreamsMap = new HashMap<>();
        videoStreamsMap.putAll(description.getVideoStreamsMap());
        audioStreamsMap = new HashMap<>();
        audioStreamsMap.putAll(description.getAudioStreamsMap());

        a4K = description.is4K();
    }

    public void addVideoStream(String streamIndex, String stream) {
        videoStreamsMap.put(streamIndex, stream);
    }

    public void addAudioStream(String streamIndex, String stream) {
        audioStreamsMap.put(streamIndex, stream);
    }

    public List<String> getVideoStreams() {
        List<String> streams = new ArrayList<>();
        for (String stream : videoStreamsMap.values()) {
            streams.add(stream);
        }
        return streams;
    }

    public List<String> getAudioStreams() {
        List<String> streams = new ArrayList<>();
        for (String stream : audioStreamsMap.values()) {
            streams.add(stream);
        }
        return streams;
    }

    public String listVideoStreams() {
        StringBuffer buffer = new StringBuffer();
        for (String stream : videoStreamsMap.values()) {
            buffer.append(stream);
            buffer.append(", ");
        }
        return buffer.toString();
    }

    public String listAudioStreams() {
        StringBuffer buffer = new StringBuffer();
        for (String stream : audioStreamsMap.values()) {
            buffer.append(stream);
            buffer.append(", ");
        }
        return buffer.toString();
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public Map<String, String> getVideoStreamsMap() {
        return videoStreamsMap;
    }

    public Map<String, String> getAudioStreamsMap() {
        return audioStreamsMap;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public void set4K(boolean a4K) {
        this.a4K = a4K;
    }

    public boolean is4K() {
        return a4K;
    }

    public void setBitrate(Long bitrate) {
        this.bitrate = bitrate;
    }

    public Long getBitrate() {
        return bitrate;
    }
}
