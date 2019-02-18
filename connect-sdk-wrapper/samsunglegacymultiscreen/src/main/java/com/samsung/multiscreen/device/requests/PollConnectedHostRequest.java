//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.channel.Channel;
import com.samsung.multiscreen.channel.connection.ConnectionFactory;
import com.samsung.multiscreen.channel.info.ChannelInfo;
import com.samsung.multiscreen.device.DeviceAsyncResult;
import com.samsung.multiscreen.device.DeviceError;
import com.samsung.multiscreen.device.requests.GetChannelInfoRequest;
import com.samsung.multiscreen.impl.SchedulerKey;
import com.samsung.multiscreen.impl.Service;
import com.samsung.multiscreen.impl.SchedulerKey.SchedulerKeyType;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PollConnectedHostRequest implements DeviceAsyncResult<ChannelInfo>, Runnable {
    private static final Logger LOG = Logger.getLogger(PollConnectedHostRequest.class.getName());
    private int delayMilliseconds = 2000;
    private URI restEndpoint;
    private String channelId;
    private DeviceAsyncResult<Channel> callback;
    private GetChannelInfoRequest channelInfoRequest;
    private int attempts;

    public PollConnectedHostRequest(URI endpoint, String channelId, int attempts, int delayMS, DeviceAsyncResult<Channel> callback) {
        this.restEndpoint = endpoint;
        this.channelId = channelId;
        this.attempts = attempts;
        this.delayMilliseconds = delayMS;
        this.callback = callback;
    }

    public void onResult(ChannelInfo result) {
        if(result != null && result.getHostConnected().equals(Boolean.TRUE)) {
            LOG.info("PollConnectedHostRequest[onResult] -- got a connected host, returning channel!");
            Channel key1 = new Channel(result, new ConnectionFactory());
            this.callback.onResult(key1);
        } else {
            --this.attempts;
            LOG.info("PollConnectedHostRequest[onResult] -- attempts remaining: " + this.attempts);
            if(this.attempts > 0) {
                LOG.info("PollConnectedHostRequest[onResult] -- scheduling poll in " + this.delayMilliseconds + " seconds");
                SchedulerKey key = new SchedulerKey(SchedulerKeyType.CONNECTED_CHANNEL_POLL, this.channelId);
                Service.getInstance().getRunnableScheduler().scheduleOnce(key, this.channelInfoRequest, (long)this.delayMilliseconds, TimeUnit.MILLISECONDS);
            } else {
                this.callback.onError(new DeviceError(-1L, "Timeout: channel not ready"));
            }
        }

    }

    public void onError(DeviceError error) {
        --this.attempts;
        LOG.info("PollConnectedHostRequest[onError] -- attempts remaining: " + this.attempts);
        if(this.attempts > 0) {
            LOG.info("PollConnectedHostRequest[onError] -- scheduling poll in " + this.delayMilliseconds + " ms");
            SchedulerKey key = new SchedulerKey(SchedulerKeyType.CONNECTED_CHANNEL_POLL, this.channelId);
            Service.getInstance().getRunnableScheduler().scheduleOnce(key, this.channelInfoRequest, (long)this.delayMilliseconds, TimeUnit.MILLISECONDS);
        } else {
            LOG.info("PollConnectedHostRequest[onError] -- last attempt failed, returning error: " + error);
            this.callback.onError(error);
        }

    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        this.channelInfoRequest = new GetChannelInfoRequest(this.restEndpoint, this.channelId, this);
        SchedulerKey key = new SchedulerKey(SchedulerKeyType.CONNECTED_CHANNEL_POLL, this.channelId);
        Service.getInstance().getRunnableScheduler().scheduleOnce(key, this.channelInfoRequest, 0L, TimeUnit.MILLISECONDS);
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
