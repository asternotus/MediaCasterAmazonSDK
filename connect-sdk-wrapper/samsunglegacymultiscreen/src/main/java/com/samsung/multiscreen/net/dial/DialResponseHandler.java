//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.net.dial;

import com.samsung.multiscreen.application.ApplicationAsyncResult;
import com.samsung.multiscreen.application.ApplicationError;
import com.samsung.multiscreen.net.dial.DialApplication;
import com.samsung.multiscreen.net.dial.DialException;
import com.samsung.multiscreen.net.http.client.Response;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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

public class DialResponseHandler {
    public static final Logger LOG = Logger.getLogger(DialResponseHandler.class.getName());
    private DocumentBuilder db;
    private InputSource is;

    public DialResponseHandler() {
    }

    public void handleLaunchResponse(Response response, ApplicationAsyncResult<Boolean> callback) {
        if(response.status >= 200 && response.status < 300) {
            try {
                LOG.info("launchApplication() response:\nHEADERS: " + response.headers);
                callback.onResult(Boolean.TRUE);
                LOG.info("launchApplication() result: true");
            } catch (Exception var6) {
                callback.onError(ApplicationError.createWithException(var6));
            }
        } else if(response.status == 503) {
            callback.onError(new ApplicationError((long)response.status, "Service unavailable"));
        } else if(response.status == 404) {
            callback.onError(new ApplicationError((long)response.status, "Not found"));
        } else if(response.status == 413) {
            callback.onError(new ApplicationError((long)response.status, "Request entity too large"));
        } else if(response.status == 411) {
            callback.onError(new ApplicationError((long)response.status, "Length required"));
        } else {
            String message;
            try {
                message = new String(response.body, "UTF-8");
            } catch (UnsupportedEncodingException var5) {
                message = "error";
            }

            callback.onError(new ApplicationError((long)response.status, message));
        }

    }

    public void handleStopResponse(Response response, ApplicationAsyncResult<Boolean> callback) {
        LOG.info("stopApplication() response: " + response.status);
        if(response.status == 200) {
            callback.onResult(Boolean.TRUE);
        } else if(response.status == 501) {
            callback.onError(new ApplicationError((long)response.status, "Not implemented"));
        } else if(response.status == 404) {
            callback.onError(new ApplicationError((long)response.status, "Not found"));
        } else {
            String message;
            try {
                message = new String(response.body, "UTF-8");
            } catch (UnsupportedEncodingException var5) {
                message = "";
            }

            callback.onError(new ApplicationError((long)response.status, message));
        }

    }

    public void handleGetApplicationResponse(Response response, ApplicationAsyncResult<DialApplication> callback) {
        String message;
        if(response.status == 200) {
            try {
                message = new String(response.body, "UTF-8");
                DialApplication e = this.createDialApplication(message);
                callback.onResult(e);
                LOG.info("getApplication() result:\n" + e);
            } catch (RuntimeException var6) {
                callback.onError(new ApplicationError(var6.getLocalizedMessage()));
            } catch (Exception var7) {
                callback.onError(new ApplicationError(var7.getLocalizedMessage()));
            }
        } else if(response.status == 503) {
            callback.onError(new ApplicationError((long)response.status, "Service unavailable"));
        } else if(response.status == 404) {
            callback.onError(new ApplicationError((long)response.status, "Not found"));
        } else if(response.status == 413) {
            callback.onError(new ApplicationError((long)response.status, "Request entity too large"));
        } else if(response.status == 411) {
            callback.onError(new ApplicationError((long)response.status, "Length required"));
        } else {
            try {
                message = new String(response.body, "UTF-8");
            } catch (UnsupportedEncodingException var5) {
                message = "";
            }

            callback.onError(new ApplicationError((long)response.status, message));
        }

    }

    private DialApplication createDialApplication(String result) throws SAXException, IOException, DialException {
        this.initParser();
        DialApplication info = new DialApplication();
        this.is.setCharacterStream(new StringReader(result));
        Document doc = this.db.parse(this.is);
        NodeList nameNodeList = doc.getElementsByTagName("name");
        info.setName(nameNodeList.item(0).getTextContent());
        NodeList stateNodeList = doc.getElementsByTagName("state");
        info.setState(stateNodeList.item(0).getTextContent());
        NodeList optionsNodeList = doc.getElementsByTagName("options");
        String optName;
        if(optionsNodeList != null && optionsNodeList.getLength() > 0 && optionsNodeList.item(0).hasAttributes()) {
            NamedNodeMap linkNodeList = optionsNodeList.item(0).getAttributes();

            for(int nodeMap = 0; nodeMap < linkNodeList.getLength(); ++nodeMap) {
                Node i = linkNodeList.item(nodeMap);
                String node = i.getNodeName();
                optName = i.getNodeValue();
                if(node.equalsIgnoreCase("allowstop")) {
                    info.setStopAllowed(optName.equalsIgnoreCase("true"));
                }

                info.setOption(node, optName);
            }
        }

        NodeList var13 = doc.getElementsByTagName("atom:link");
        if(var13 == null || var13.getLength() == 0) {
            var13 = doc.getElementsByTagName("link");
        }

        if(var13 != null && var13.getLength() > 0 && var13.item(0) != null && var13.item(0).hasAttributes()) {
            NamedNodeMap var14 = var13.item(0).getAttributes();

            for(int var15 = 0; var15 < var14.getLength(); ++var15) {
                Node var16 = var14.item(var15);
                optName = var16.getNodeName();
                String optVal = var16.getNodeValue();
                if(optName.equalsIgnoreCase("rel")) {
                    info.setRelLink(optVal);
                } else if(optName.equalsIgnoreCase("href")) {
                    info.setHrefLink(optVal);
                }

                info.setOption(optName, optVal);
            }
        }

        return info;
    }

    private void initParser() throws DialException {
        if(this.db == null || this.is == null) {
            try {
                this.db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                this.is = new InputSource();
            } catch (ParserConfigurationException var2) {
                throw new DialException("client initialization failed");
            }
        }
    }

    static {
        LOG.setLevel(Level.OFF);
    }
}
