package com.megacast.connectsdkwrapper.internal.managers;

import com.connectsdk.core.MediaInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.command.ServiceCommandError;
import com.megacast.connectsdkwrapper.internal.model.TvDevice;
import com.megacast.connectsdkwrapper.internal.model.MediaControllerImpl;
import com.megacast.castsdk.providers.managers.cast.ImageCastManager;
import com.megacast.castsdk.model.Device;
import com.megacast.castsdk.model.ImageDescription;
import com.megacast.castsdk.model.MediaController;



import rx.Observable;
import rx.Subscriber;

/**
 * Created by Dmitry on 13.09.2016.
 */
public class ImageCastManagerImpl implements ImageCastManager {

    private String[] requiredCapabilities = {
            MediaPlayer.Display_Image
    };

    //  <editor-fold desc="public interface">

    @Override
    public Observable<MediaController> beamImageFile( final Device device,  final ImageDescription imageDescription) {
        return Observable.create(
                new Observable.OnSubscribe<MediaController>() {
                    @Override
                    public void call(final Subscriber<? super MediaController> sub) {
                        beamImageFile(device, imageDescription.getCastUrl(),
                                imageDescription.getIconUrl(), imageDescription.getTitle(),
                                imageDescription.getDescription(), imageDescription.getMimeType(),
                                new ImageLaunchListener() {
                                    @Override
                                    public void onSuccess(MediaController controller) {
                                        controller.setImageDescription(imageDescription);
                                        sub.onNext(controller);
                                        sub.onCompleted();
                                    }

                                    @Override
                                    public void onError(Throwable cause) {
                                        sub.onError(cause);
                                    }
                                });
                    }
                }
        );
    }

    //  </editor-fold>


    //  <editor-fold desc="private">

    private void beamImageFile(Device pDevice, String imageUrl, String iconUrl,
                               String title, String description, String mimeType,
                               final ImageLaunchListener imageLaunchListener) throws IllegalArgumentException {

        if (!(pDevice instanceof TvDevice)) {
            throw new IllegalArgumentException("Wrong device type!");
        }

        final ConnectableDevice connectableDevice = ((TvDevice) pDevice).getConnectableDevice();

        if (connectableDevice.isConnectable() && !connectableDevice.isConnected()) {
            if (imageLaunchListener != null) {
                imageLaunchListener.onError(new Throwable("Device must be connected!"));
            }
            return;
        }

        MediaPlayer mediaPlayer = connectableDevice.getCapability(MediaPlayer.class);

        if (!connectableDevice.hasCapabilities(requiredCapabilities) || mediaPlayer == null) {
            if (imageLaunchListener != null) {
                imageLaunchListener.onError(new Throwable("Device hasn't Media Player capabilities!"));
            }
            return;
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(imageUrl, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconUrl)
                .build();

        MediaPlayer.LaunchListener listener = new MediaPlayer.LaunchListener() {
            @Override
            public void onSuccess(MediaPlayer.MediaLaunchObject object) {
                if (imageLaunchListener != null) {
                    MediaControllerImpl mediaController = new MediaControllerImpl(object.launchSession, object.mediaControl, object.playlistControl);
                    imageLaunchListener.onSuccess(mediaController);
                }
            }

            @Override
            public void onError(ServiceCommandError error) {
                if (imageLaunchListener != null) {
                    imageLaunchListener.onError(new Throwable(error.getMessage()));
                }
            }
        };

        mediaPlayer.displayImage(mediaInfo, listener);
    }

    //  </editor-fold>

    //  <editor-fold desc="ImageLaunchListener">

    private interface ImageLaunchListener {

        void onSuccess(MediaController controller);

        void onError(Throwable cause);
    }

    //  </editor-fold>

}
