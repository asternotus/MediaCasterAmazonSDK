//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.device.requests;

import com.samsung.multiscreen.device.requests.impl.DeviceURIResult;
import com.samsung.multiscreen.net.AsyncResult;
import com.samsung.multiscreen.net.http.client.HttpSyncClient;
import com.samsung.multiscreen.net.http.client.Response;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GetDialDeviceDescriptorRequest implements Runnable {
    private static final Logger LOG = Logger.getLogger(GetDialDeviceDescriptorRequest.class.getName());
    private URI descriptorURI;
    private AsyncResult<DeviceURIResult> callback;
    private String targetVersion;

    public GetDialDeviceDescriptorRequest(URI descriptorURI, String targetVersion, AsyncResult<DeviceURIResult> callback) {
        this.descriptorURI = descriptorURI;
        this.targetVersion = targetVersion;
        this.callback = callback;
    }

    public void run() {
        this.performRequest();
    }

    protected void performRequest() {
        HttpSyncClient client = new HttpSyncClient();

        try {
            URL e = this.descriptorURI.toURL();
            Map headers = HttpSyncClient.initGetHeaders(e);
            client.setReadTimeout(2000);
            Response response = client.get(e, headers);
            if(response == null) {
                this.callback.onException(new Exception(client.getLastErrorMessage()));
                return;
            }

            if(response.status == 200) {
                this.handleResponse(response);
            } else {
                this.callback.onException(new Exception("Non-matching device"));
            }
        } catch (MalformedURLException var5) {
            this.callback.onException(var5);
        }

    }

    protected void handleResponse(Response response) {
        String appURL = "";
        List headerlist = (List)response.headers.get("Application-URL");
        if(headerlist != null && headerlist.size() > 0) {
            appURL = (String)headerlist.get(0);
        }

        try {
            String e = new String(response.body, "UTF-8");
            LOG.info("Got XML descriptor: " + e);
            LOG.info("Application-URL is " + appURL);
            String baseURL = this.descriptorURI.getScheme() + "://" + this.descriptorURI.getHost();
            String serviceURL = this.getServiceURL(baseURL, this.targetVersion, e);
            if(appURL == null || appURL.isEmpty() || serviceURL == null || serviceURL.isEmpty()) {
                this.callback.onException(new Exception("Non-matching device"));
                return;
            }

            URI serviceURI = URI.create(serviceURL);
            URI appURI = URI.create(appURL);
            if(serviceURI != null && appURI != null) {
                this.callback.onResult(new DeviceURIResult(serviceURI, appURI));
            } else {
                this.callback.onException(new Exception("Non-matching device"));
            }
        } catch (UnsupportedEncodingException var9) {
            this.callback.onException(var9);
        }

    }

    protected String getServiceURL(String baseURL, String version, String rawDescriptor) {
        try {
            DocumentBuilder e = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(rawDescriptor));
            Document doc = e.parse(inputSource);
            NodeList capsNodeList = doc.getElementsByTagName("sec:Capabilities");
            if(capsNodeList != null && capsNodeList.getLength() > 0) {
                NodeList capsChildren = capsNodeList.item(0).getChildNodes();
                if(capsChildren != null && capsChildren.getLength() > 0) {
                    for(int i = 0; i < capsChildren.getLength(); ++i) {
                        Node child = capsChildren.item(i);
                        if(child != null && child.getNodeName().equalsIgnoreCase("sec:Capability") && child.hasAttributes()) {
                            NamedNodeMap attribs = child.getAttributes();
                            if(attribs != null) {
                                Node nameNode = attribs.getNamedItem("name");
                                if(nameNode != null && nameNode.getNodeValue().equalsIgnoreCase(version)) {
                                    String location = "";
                                    String port = "";
                                    Node locationNode = attribs.getNamedItem("location");
                                    Node portNode = attribs.getNamedItem("port");
                                    if(locationNode != null) {
                                        location = locationNode.getNodeValue();
                                    }

                                    if(portNode != null) {
                                        port = portNode.getNodeValue();
                                    }

                                    StringBuilder builder = new StringBuilder();
                                    builder.append(baseURL);
                                    if(port != null && !port.isEmpty()) {
                                        builder.append(":").append(port);
                                    }

                                    if(location != null && !location.isEmpty()) {
                                        builder.append(location);
                                    }

                                    String builtURL = builder.toString();
                                    LOG.info("Built url: " + builtURL);
                                    return builtURL;
                                }
                            }
                        }
                    }
                }
            }

            return "";
        } catch (SAXException var19) {
            LOG.info("GetDialDeviceDescriptor -- Exception parsing descriptor: " + var19.getLocalizedMessage());
            return "";
        } catch (IOException var20) {
            LOG.info("GetDialDeviceDescriptor -- Exception reading descriptor: " + var20.getLocalizedMessage());
            return "";
        } catch (ParserConfigurationException var21) {
            LOG.info("GetDialDeviceDescriptor -- Exception constructing parser: " + var21.getLocalizedMessage());
            return "";
        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
