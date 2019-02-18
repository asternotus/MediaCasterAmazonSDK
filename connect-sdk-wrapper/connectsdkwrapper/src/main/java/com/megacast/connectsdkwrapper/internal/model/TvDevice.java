package com.megacast.connectsdkwrapper.internal.model;

import android.graphics.Point;

import com.mega.cast.utils.log.SmartLog;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.WakeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.castsdk.exceptions.ReceiverAppNotInstalledException;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.DeviceListener;
import com.megacast.castsdk.model.ReceiverInfo;
import com.megacast.castsdk.model.VolumeController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Dmitry on 13.09.2016.
 */
public class TvDevice implements Device {

    private static final String LOG_TAG = TvDevice.class.getSimpleName();
    protected static final int DEFAULT_SUPPORTED_IMAGE_WIDTH = 1920;
    protected static final int DEFAULT_SUPPORTED_IMAGE_HEIGHT = 1080;

    private ConnectableDevice device;
    private CopyOnWriteArrayList<DeviceListener> listeners;
    private ReceiverInfo receiverInfo;

    protected String[] supportedVideoCodecs;
    protected String[] supportedAudioCodecs;

    protected List<SupportedFormat> mediaFormats;
    protected List<SupportedFormat> imageFormats;

    protected List<Integer> supportedSubtitles;

    private boolean allowAllMediaFormats = false;
    private boolean allowAllImageFormats = false;

    private int unsupportedVideoConversionType = UNSUPPORTED_VIDEO_CONVERSION_TYPE_HLS;

    private boolean transcodingForced = false;
    private boolean audioPlayListMode = false;

    private boolean liveSubtitleChangesSupported = false;

    public TvDevice(ConnectableDevice device) {
        this.device = device;
        this.listeners = new CopyOnWriteArrayList<>();

        this.mediaFormats = new ArrayList<>();
        this.imageFormats = new ArrayList<>();
        this.supportedSubtitles = new ArrayList<>();
    }

    //  <editor-fold desc="Device public interface">

    @Override
    public String getIpAdress() {
        return device.getIpAddress();
    }

    @Override
    public String getName() {
        return device.getFriendlyName();
    }

    @Override
    public String getID() {
        return device.getId();
    }

    @Override
    public VolumeController getVolumeController() {
        return new VolumeControllerImpl(device.getCapability(VolumeControl.class));
    }

    @Override
    public boolean isDeviceOnline() {
        final WakeControl wakeControl = device.getCapability(WakeControl.class);
        return wakeControl == null || wakeControl.isDeviceOnline();
    }

    @Override
    public void addDeviceListener(DeviceListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListeners() {
        listeners.clear();
    }

    @Override
    public void sendPairingData(String data) {
        device.sendPairingKey(data);
    }

    @Override
    public boolean isConnected() {
        return device.isConnected() || !device.isConnectable();
    }

    @Override
    public boolean isImageResolutionSupported(int width, int height) {
        return true;
    }

    @Override
    public Point getSupportedImageResolution() {
        return new Point(DEFAULT_SUPPORTED_IMAGE_WIDTH, DEFAULT_SUPPORTED_IMAGE_HEIGHT);
    }

    @Override
    public boolean hasSupportedDataType(String metaData, String extension) {
        if (allowAllMediaFormats) {
            return true;
        }

        for (int i = 0; i < mediaFormats.size(); i++) {
            if (mediaFormats.get(i).has(metaData, extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasSupportedImageType(String metaData, String extension) {
        if (allowAllImageFormats) {
            return true;
        }

        for (int i = 0; i < imageFormats.size(); i++) {
            if (imageFormats.get(i).has(metaData, extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasSupportedVideoStreams(List<String> videoStreams) {
        if (allowAllMediaFormats) {
            return true;
        }

        if (videoStreams.isEmpty()) {
            return true;
        }

        if (supportedVideoCodecs != null) {
            for (String supportedVideoCodec : supportedVideoCodecs) {
                for (String videoStream : videoStreams) {
                    if (videoStream.contains(supportedVideoCodec)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean hasSupportedAudioStreams(List<String> audioStreams) {
        if (allowAllMediaFormats) {
            return true;
        }

        if (audioStreams.isEmpty()) {
            return true;
        }

        if (supportedAudioCodecs != null) {
            for (String supportedAudioCodec : supportedAudioCodecs) {
                for (String audioStream : audioStreams) {
                    if (audioStream.contains(supportedAudioCodec)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isMediaCompatible() {
        for (DeviceService service : device.getServices()) {
            if (service instanceof MediaPlayer) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setReceiverInfo(ReceiverInfo receiverInfo) {
        this.receiverInfo = receiverInfo;
    }

    @Override
    public ReceiverInfo getReceiverInfo() {
        return receiverInfo;
    }

    @Override
    public boolean equals(Device device) {
        return device != null && getUniqueIdentifier().equals(device.getUniqueIdentifier());
    }

    @Override
    public String getUniqueIdentifier() {
        return getName() + getIpAdress();
    }

    @Override
    public void cleanMediaResources() {
        MediaPlayer mediaPlayer = device.getMediaPlayer();
        if (mediaPlayer != null)
            mediaPlayer
                    .closeMedia(null, new ResponseListener<Object>() {
                        @Override
                        public void onSuccess(Object object) {
                            SmartLog.d(LOG_TAG, "media closed ");
                        }

                        @Override
                        public void onError(ServiceCommandError error) {
                            SmartLog.e(LOG_TAG, "Error closing media! " + error.getMessage());
                        }
                    });
    }

    @Override
    public List<Integer> getSupportedSubtitles() {
        return supportedSubtitles;
    }

    @Override
    public boolean hasSupportedSubtitleFormat(int type) {
        return supportedSubtitles.contains(type);
    }

    @Override
    public int getUnsupportedVideoConversionType() {
        return unsupportedVideoConversionType;
    }

    @Override
    public boolean isTranscodingForced() {
        return transcodingForced;
    }

    @Override
    public boolean isAudioPlaylistMode() {
        return audioPlayListMode;
    }

    @Override
    public boolean isLiveSubtitleChangesSupported() {
        return liveSubtitleChangesSupported;
    }

    @Override
    public int hashCode() {
        return getUniqueIdentifier().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TvDevice && hashCode() == o.hashCode();
    }

    //  </editor-fold>

    //  <editor-fold desc="wrapper public interface">

    public void connect() {
        if (!device.isConnected() && device.isConnectable()) {
            device.addListener(connectableDeviceListener);
            device.connect();
        } else {
            connectableDeviceListener.onDeviceReady(device);
        }
    }

    public void disconnect() {
        if (device.isConnected()) {
            device.disconnect();
        }
    }

    public ConnectableDevice getConnectableDevice() {
        return device;
    }

    public void setDevice(ConnectableDevice device) {
        this.device = device;
    }

    //  </editor-fold>

    //  <editor-fold desc="private">

    private ConnectableDeviceListener connectableDeviceListener = new ConnectableDeviceListener() {
        @Override
        public void onDeviceReady(ConnectableDevice device) {
            for (DeviceListener deviceListener : listeners) {
                deviceListener.onDeviceReady();
            }
        }

        @Override
        public void onDeviceDisconnected(ConnectableDevice device) {
            for (DeviceListener deviceListener : listeners) {
                deviceListener.onDeviceDisconnected();
            }
        }

        @Override
        public void onPairingRequired(ConnectableDevice device, DeviceService service, DeviceService.PairingType pairingType) {
            for (DeviceListener deviceListener : listeners) {
                deviceListener.onPairingRequired(getPairingType(pairingType));
            }
        }

        @Override
        public void onCapabilityUpdated(ConnectableDevice device, List<String> added, List<String> removed) {

        }

        @Override
        public void onConnectionFailed(ConnectableDevice device, ServiceCommandError error) {
            Throwable failReason = error;
            if (error.getCode() == ServiceCommandError.RECEIVER_APP_NOT_INSTALLED) {
                failReason = new ReceiverAppNotInstalledException(error);
            }
            for (DeviceListener deviceListener : listeners) {
                deviceListener.onConnectionFailed(failReason);
            }
        }
    };

    private int getPairingType(DeviceService.PairingType pairingType) {
        switch (pairingType) {
            case NONE:
                return PAIRING_TYPE_NONE;
            case FIRST_SCREEN:
                return PAIRING_TYPE_TV_CONFIRMATION;
            case PIN_CODE:
                return PAIRING_PIN_CODE;
        }
        return PAIRING_TYPE_UNKNOWN;
    }

    protected void setAllowAllMediaFormats(boolean allowAllMediaFormats) {
        this.allowAllMediaFormats = allowAllMediaFormats;
    }

    protected void setAllowAllImageFormats(boolean allowAllImageFormats) {
        this.allowAllImageFormats = allowAllImageFormats;
    }

    protected void setUnsupportedVideoConversionType(int unsupportedVideoConversionType) {
        this.unsupportedVideoConversionType = unsupportedVideoConversionType;
    }

    protected void setTranscodingForced(boolean transcodingForced) {
        this.transcodingForced = transcodingForced;
    }

    protected void setAudioPlayListMode(boolean audioPlayListMode) {
        this.audioPlayListMode = audioPlayListMode;
    }

    protected void setLiveSubtitleChangesSupported(boolean liveSubtitleChangesSupported) {
        this.liveSubtitleChangesSupported = liveSubtitleChangesSupported;
    }

    //  </editor-fold>

    static class SupportedFormat {
        String[] extensions;
        String mimeType;

        public SupportedFormat(String mimeType, String... extensions) {
            this.mimeType = mimeType;
            this.extensions = extensions;
        }

        public boolean has(String mimeType, String extension) {
            if (this.mimeType == null || this.mimeType.equals(mimeType)) {
                for (int i = 0; i < extensions.length; i++) {
                    if (extension != null && extensions[i].equals(extension.toLowerCase())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

}
