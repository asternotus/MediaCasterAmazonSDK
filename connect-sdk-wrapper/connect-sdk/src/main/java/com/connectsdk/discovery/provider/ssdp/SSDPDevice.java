/*
 * SSDPDevice
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 6 Jan 2015
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.discovery.provider.ssdp;

import com.mega.cast.utils.log.SmartLog;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class SSDPDevice {

    public static final String LOG_TAG = SSDPDevice.class.getSimpleName();

    /* Required. UPnP device type. */
    public String deviceType;
    /* Required. Short description for end user. */
    public String friendlyName;
    /* Required. Manufacturer's name. */
    public String manufacturer;
    //    /* Optional. Web site for manufacturer. */
//    public String manufacturerURL;
    /* Recommended. Long description for end user. */
    public String modelDescription;
    /* Required. Model name. */
    public String modelName;
    /* Recommended. Model number. */
    public String modelNumber;
    //    /* Optional. Web site for model. */
//    public String modelURL;
//    /* Recommended. Serial number. */
//    public String serialNumber;
    /* Required. Unique Device Name. */
    public String UDN;
    //    /* Optional. Universal Product Code. */
//    public String UPC;
    /* Required. */
//    List<Icon> iconList = new ArrayList<Icon>();
    public String locationXML;
    /* Optional. */
    public List<Service> serviceList = new ArrayList<Service>();

    public String ST;
    public String applicationURL;

    public String serviceURI;

    public String baseURL;
    public String ipAddress;
    public int port;
    public String UUID;

    public Map<String, List<String>> headers;
    public String model;


    public SSDPDevice() {
    }

    public SSDPDevice(String url, String ST) throws IOException, ParserConfigurationException, SAXException {
        this(new URL(url), ST);
    }

    public SSDPDevice(URL urlObject, String ST) throws IOException, ParserConfigurationException, SAXException {
        if (urlObject.getPort() == -1) {
            baseURL = String.format("%s://%s", urlObject.getProtocol(), urlObject.getHost());
        } else {
            baseURL = String.format("%s://%s:%d", urlObject.getProtocol(), urlObject.getHost(), urlObject.getPort());
        }
        ipAddress = urlObject.getHost();
        port = urlObject.getPort();
        UUID = null;

        serviceURI = String.format("%s://%s", urlObject.getProtocol(), urlObject.getHost());

        parse(urlObject);

        SmartLog.d(LOG_TAG, "friendly name " + friendlyName);
    }

    public static final SSDPDevice getDevice(URL urlObject, String ST) {

        SSDPDevice ssdpDevide = new SSDPDevice();

        if (urlObject.getPort() == -1) {
            ssdpDevide.baseURL = String.format("%s://%s", urlObject.getProtocol(), urlObject.getHost());
        } else {
            ssdpDevide.baseURL = String.format("%s://%s:%d", urlObject.getProtocol(), urlObject.getHost(), urlObject.getPort());
        }
        ssdpDevide.ipAddress = urlObject.getHost();
        ssdpDevide.port = urlObject.getPort();
        ssdpDevide.UUID = null;

        ssdpDevide.serviceURI = String.format("%s://%s", urlObject.getProtocol(), urlObject.getHost());

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;

        try {

            SSDPDeviceDescriptionParser parser = new SSDPDeviceDescriptionParser(ssdpDevide);

            URLConnection urlConnection = urlObject.openConnection();

            ssdpDevide.applicationURL = urlConnection.getHeaderField("Application-URL");

            SmartLog.d(LOG_TAG, String.format("app url for %s is %s", urlObject.toString(), ssdpDevide.applicationURL));

            if (ssdpDevide.applicationURL != null && !ssdpDevide.applicationURL.substring(ssdpDevide.applicationURL.length() - 1).equals("/")) {
                ssdpDevide.applicationURL = ssdpDevide.applicationURL.concat("/");
            }

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            Scanner s = null;
            try {
                s = new Scanner(in).useDelimiter("\\A");
                ssdpDevide.locationXML = s.hasNext() ? s.next() : "";

                saxParser = factory.newSAXParser();
                saxParser.parse(new ByteArrayInputStream(ssdpDevide.locationXML.getBytes()), parser);
                SmartLog.d(LOG_TAG, "friendly name " + ssdpDevide.friendlyName);
            } finally {
                in.close();
                if (s != null)
                    s.close();
            }

            ssdpDevide.headers = urlConnection.getHeaderFields();

        } catch (Exception ex) {
            ex.printStackTrace();
            SmartLog.e(LOG_TAG, "Exception while parsing! " + ex.getMessage());
        }

        SmartLog.d(LOG_TAG, "friendly name " + ssdpDevide.friendlyName);

        return ssdpDevide;
    }

    public void parse(URL url) throws IOException, ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;

        SSDPDeviceDescriptionParser parser = new SSDPDeviceDescriptionParser(this);

        URLConnection urlConnection = url.openConnection();

        applicationURL = urlConnection.getHeaderField("Application-URL");

        SmartLog.d(LOG_TAG, String.format("app url for %s is %s", url.toString(), applicationURL));

        if (applicationURL != null && !applicationURL.substring(applicationURL.length() - 1).equals("/")) {
            applicationURL = applicationURL.concat("/");
        }

        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        Scanner s = null;
        try {
            s = new Scanner(in).useDelimiter("\\A");
            locationXML = s.hasNext() ? s.next() : "";

            saxParser = factory.newSAXParser();
            saxParser.parse(new ByteArrayInputStream(locationXML.getBytes()), parser);
            SmartLog.d(LOG_TAG, "friendly name " + friendlyName);
        } finally {
            in.close();
            if (s != null)
                s.close();
        }

        headers = urlConnection.getHeaderFields();

        SmartLog.d(LOG_TAG, "friendly name " + friendlyName);
    }

    @Override
    public String toString() {
        return friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }
}
