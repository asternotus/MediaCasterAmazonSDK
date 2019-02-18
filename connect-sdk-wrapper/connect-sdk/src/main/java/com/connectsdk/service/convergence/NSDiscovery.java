package com.connectsdk.service.convergence;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.cybergarage.xml.Node;
import org.cybergarage.xml.ParserException;
import org.cybergarage.xml.parser.JaxpParser;

import com.mega.cast.utils.log.SmartLog;

/**
 * <p>
 * This class is helper to find N-Service capable devices ({@link NSDevice}) using SSDP Packet
 * listener from {@link com.cyabergarage.upnp.ssdp}. This class will broadcast UPnP SSDP "<i>M-SEARCH</i>"
 * packet throughout the networks and waiting the reply of "<i>NOTIFY</i>" packet from N-Service capable devices.
 * </p>
 *
 * @author I Made Krisna Widhiastra (im.krisna@gmail.com)
 * @version 1.0.0
 * @see {@link NSDevice}
 * @see {@link NSConnection}
 * @see {@link NSDiscoveryListener}
 */
public class NSDiscovery {

    private static final String LOG_TAG = "NSDiscovery";

    /**
     * <p><b><i>public static final String UPNP_NSCREEN_FILTER</i></b></p>
     * <p>
     * UPnP SSDP "<i>ST</i>" header value when doing "<i>M-SEARCH</i>" broadcast and filtering "<i>NT</i>" value when
     * receiving "<i>NOTIFY</i>" packets from UPnP Devices
     * </p>
     * <p>
     * Static Value: <i>urn:samsung.com:service:MultiScreenService:1</i>
     * </p>
     *
     * @since 1.0.0
     */
    public static final String UPNP_NSCREEN_FILTER = "urn:samsung.com:service:MultiScreenService:1";

    private NSConnection connection;
    private ControlPoint controlPoint;
    private NSDiscoveryListener listener;

    /**
     * <p><b><i>public NSDiscovery({@link NSConnection} connection)</i></b></p>
     * <p>
     * Constructor for NSDiscovery class for searching N-Service capable devices in the networks
     * </p>
     *
     * @param connection - {@link NSConnection} instance where the found {@link NSDevice} will be stored
     * @since 1.0.0
     */
    public NSDiscovery(NSConnection connection) {
        this.connection = connection;
        this.controlPoint = new ControlPoint();
    }

    /**
     * <p><b><i>public void search()</i></b></p>
     * <p>
     * Starting the N-Service capable devices discovery sequence, Sending "<i>M-SEARCH</i>" packet
     * and waiting for the matched "<i>NOTIFY</i>" packet. The response from N-Service capable
     * device will be handled by {@link NSDiscoveryListener} class.
     * </p>
     *
     * @since 1.0.0
     */
    public void search() {
        this.dispose();
        this.controlPoint = new ControlPoint();

        if (this.listener != null) {
            this.controlPoint.removeSearchResponseListener(this.listener);
        }

        if (this.connection.isWifiConnected()) {
            this.listener = new NSDiscoveryListener(this.connection);
            this.controlPoint.addSearchResponseListener(this.listener);
            try {
                this.controlPoint.start(NSDiscovery.UPNP_NSCREEN_FILTER);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * <p><b><i>public void dispose()</i></b></p>
     * <p>
     * Cleanup resources, connections, listeners to prevent Address/Port blocking when allocating
     * search request again.
     * </p>
     *
     * @since 1.0.0
     */
    public void dispose() {
        this.controlPoint.stop();
        this.controlPoint.unlock();
        this.controlPoint.unsubscribe();
        this.controlPoint.removeSearchResponseListener(this.listener);
        this.controlPoint.finalize();
    }


    /**
     * <p>
     * This class implementing {@link SearchResponseListener} is a response handler for incoming {@link SSDPPacket}
     * and parsing the information to decide whether the devices is N-Service capable device or not then storing
     * the capable devices to {@link NSConnection} instance.
     * </p>
     *
     * @author I Made Krisna Widhiastra (im.krisna@gmail.com)
     * @version 1.0.0
     * @see {@link NSDevice}
     * @see {@link NSConnection}
     * @see {@link NSDiscovery}
     */
    private class NSDiscoveryListener implements SearchResponseListener {

        private NSConnection connection;

        /**
         * <p><b><i>public NSDiscoveryListener({@link NSConnection} connection)</i></b></p>
         * <p>
         * Constructor for NSDiscoveryListener class for handling the {@link SSDPPacket} response
         * from UPnP Devices in the networks.
         * </p>
         *
         * @since 1.0.0
         */
        public NSDiscoveryListener(NSConnection connection) {
            this.connection = connection;
        }

        /**
         * <p><b><i>public {@link Node} getDeviceNode({@link HttpResponse} response)</i></b></p>
         * <p>
         * Parsing the response XML from SSDP "<i>Location</i>" URL and search for "<i>device</i>" node.
         * <p>
         *
         * @param response - {@link HttpResponse} from device "<i>Location</i>" header
         * @return Device {@link Node} object from Response XML
         * @since 1.0.0
         */
        public Node getDeviceNode(HttpResponse response) throws IllegalStateException, ParserException, IOException {
            return new JaxpParser().parse(response.getEntity().getContent()).getNode("device");
        }

        /**
         * <p><b><i>public void deviceSearchResponseReceived({@link SSDPPacket} ssdpPacket)</i></b></p>
         * <p>
         * Callback/Listener method to receive {@link SSDPPacket} bundle from networks. This method will parse and process
         * the bundle to decide whether the device is N-Service capable or not then storing
         * the capable devices to {@link NSConnection} instance.
         * </p>
         *
         * @param ssdpPacket - {@link SSDPPacket} bundle received from networks
         * @since 1.0.0
         */
        public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
            try {
                String location = ssdpPacket.getLocation();
                String ipAddress = ssdpPacket.getRemoteAddress();

                SmartLog.d(LOG_TAG, "UPnP Device Found");
                SmartLog.d(LOG_TAG, "-- Location: " + location);
                SmartLog.d(LOG_TAG, "-- Address: " + ipAddress);

                DefaultHttpClient client = NSConnection.createHttpClient();
                HttpGet request = new HttpGet(location);
                HttpResponse response = client.execute(request);

                SmartLog.d(LOG_TAG, "UPnP Device (" + ipAddress + ") Response: " + String.valueOf(response.getStatusLine().getStatusCode()));
                SmartLog.d(LOG_TAG, "-- " + response.getStatusLine().getReasonPhrase());

                if (response != null && response.getStatusLine().getStatusCode() == 200) {
                    Node devNode = this.getDeviceNode(response);
                    if (devNode != null) {
                        String deviceName = devNode.getNodeValue("friendlyName");
                        this.connection.addDevice(new NSDevice(ipAddress, deviceName));
                        SmartLog.d(LOG_TAG, "UPnP Device (" + ipAddress + ") Recognized as " + deviceName);
                    } else {
                        SmartLog.d(LOG_TAG, "UPnP Device (" + ipAddress + ") Not Recognized");
                    }

                    response.getEntity().consumeContent();
                } else {
                    SmartLog.w(LOG_TAG, "UPnP Device (" + ipAddress + ") Unreachable");
                }
            } catch (ClientProtocolException e) {
                SmartLog.e(LOG_TAG, "ClientProtocolException: " + e.getMessage());
            } catch (ParserException e) {
                SmartLog.e(LOG_TAG, "ParserException: " + e.getMessage());
            } catch (IOException e) {
                SmartLog.e(LOG_TAG, "IOException: " + e.getMessage());
            }
        }
    }
}
