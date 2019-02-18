//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.samsung.multiscreen.channel.connection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import net.iharder.Base64;

public class PKI {
    private static final Logger LOG = Logger.getLogger(PKI.class.getName());
    private static final int CHUNK_SIZE = 117;
    private static final int PEM_LINE_LENGTH = 64;
    private static final String BEGIN = "-----BEGIN ";
    private static final String END = "-----END ";
    private static SecureRandom random;
    protected static final char[] hexArray;

    public PKI() {
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator e = KeyPairGenerator.getInstance("RSA");
            e.initialize(1024, random);
            return e.generateKeyPair();
        } catch (NoSuchAlgorithmException var1) {
            var1.printStackTrace();
            return null;
        }
    }

    public static String keyAsPEM(Key key, String type) {
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN ").append(type).append("-----\r\n");
        byte[] b64keyBytes = Base64.encodeBytesToBytes(key.getEncoded());
        char[] pemLine = new char[64];

        for(int i = 0; i < b64keyBytes.length; i += pemLine.length) {
            for(int index = 0; index != pemLine.length && i + index < b64keyBytes.length; ++index) {
                pemLine[index] = (char)b64keyBytes[i + index];
            }

            builder.append(pemLine);
            builder.append("\r\n");
        }

        builder.append("-----END ").append(type).append("-----");
        return builder.toString();
    }

    public static Key pemAsPublicKey(String pem) {
        try {
            ByteArrayInputStream e = new ByteArrayInputStream(pem.getBytes("UTF-8"));
            BufferedReader br = new BufferedReader(new InputStreamReader(e));

            String line;
            for(line = br.readLine(); line != null && !line.startsWith("-----BEGIN "); line = br.readLine()) {
                ;
            }

            StringBuilder builder = new StringBuilder();
            if(line != null) {
                while((line = br.readLine()) != null && !line.contains("-----END ")) {
                    builder.append(line.trim());
                }

                byte[] keyBytes = Base64.decode(builder.toString());
                X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                return kf.generatePublic(spec);
            }
        } catch (IOException var8) {
            var8.printStackTrace();
        } catch (NoSuchAlgorithmException var9) {
            var9.printStackTrace();
        } catch (InvalidKeySpecException var10) {
            var10.printStackTrace();
        }

        return null;
    }

    public static String encryptStringAsHex(String str, Key key) {
        byte[] cipherText = encryptString(str, key);
        return cipherText != null?bytesToHex(cipherText):null;
    }

    public static byte[] encryptString(String str, Key key) {
        if(key == null) {
            return null;
        } else {
            try {
                Cipher e = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                e.init(1, key, random);
                int chunkOutputSize = e.getOutputSize(117);
                LOG.info("encryptString string length: " + str.length());
                LOG.info("encryptString bytes length: " + str.getBytes("UTF-8").length);
                byte[] strBytes = str.getBytes("UTF-8");
                int numChunks = strBytes.length / 117;
                int remainderBytes = strBytes.length % 117;
                int bufferSize = numChunks * chunkOutputSize + e.getOutputSize(remainderBytes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);
                int offset = 0;

                byte[] output;
                for(output = new byte[chunkOutputSize]; offset + 117 <= strBytes.length; offset += 117) {
                    e.doFinal(strBytes, offset, 117, output);
                    baos.write(output);
                }

                if(remainderBytes > 0) {
                    output = new byte[e.getOutputSize(remainderBytes)];
                    e.doFinal(strBytes, offset, remainderBytes, output);
                    baos.write(output);
                }

                baos.close();
                return baos.toByteArray();
            } catch (IOException var11) {
                var11.printStackTrace();
            } catch (NoSuchAlgorithmException var12) {
                var12.printStackTrace();
            } catch (NoSuchPaddingException var13) {
                var13.printStackTrace();
            } catch (InvalidKeyException var14) {
                var14.printStackTrace();
            } catch (BadPaddingException var15) {
                var15.printStackTrace();
            } catch (ShortBufferException var16) {
                var16.printStackTrace();
            } catch (IllegalBlockSizeException var17) {
                var17.printStackTrace();
            }

            return null;
        }
    }

    public static String decryptHexString(String str, Key key) {
        byte[] plainText = decryptString(hexToBytes(str), key);

        try {
            return plainText != null?new String(plainText, "UTF-8"):null;
        } catch (UnsupportedEncodingException var4) {
            var4.printStackTrace();
            return null;
        }
    }

    public static byte[] decryptString(byte[] bytes, Key key) {
        if(key == null) {
            return null;
        } else {
            try {
                boolean e = true;
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(2, key);
                int remainderBytes = bytes.length % 128;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                int offset;
                byte[] output;
                for(offset = 0; offset + 128 <= bytes.length; offset += 128) {
                    output = cipher.doFinal(bytes, offset, 128);
                    baos.write(output);
                }

                if(remainderBytes > 0) {
                    output = cipher.doFinal(bytes, offset, remainderBytes);
                    baos.write(output);
                }

                baos.close();
                return baos.toByteArray();
            } catch (NoSuchAlgorithmException var8) {
                var8.printStackTrace();
            } catch (NoSuchPaddingException var9) {
                var9.printStackTrace();
            } catch (InvalidKeyException var10) {
                var10.printStackTrace();
            } catch (BadPaddingException var11) {
                var11.printStackTrace();
            } catch (IllegalBlockSizeException var12) {
                var12.printStackTrace();
            } catch (IOException var13) {
                var13.printStackTrace();
            }

            return null;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for(int j = 0; j < bytes.length; ++j) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 15];
        }

        return new String(hexChars);
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    static {
        LOG.setLevel(Level.OFF);
        random = new SecureRandom();
        hexArray = "0123456789ABCDEF".toCharArray();
    }
}
