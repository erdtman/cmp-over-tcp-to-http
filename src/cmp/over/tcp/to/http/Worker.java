package cmp.over.tcp.to.http;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Worker that will send and receive data form TCP to HTTP.
 * 
 * @author Samuel Erdtman
 * 
 */
public class Worker implements Runnable {
    /** Logger instance */
    private final static Logger log        = Logger.getLogger(Worker.class.getName());

    /** Incoming socket */
    private Socket              socket;
    /** HTTP Post URI */
    private URI                 uri;
    /** If to base 64 encode and decode data to and from HTTP */
    private boolean             base64;
    /** HTTP Client instance */
    private CloseableHttpClient httpclient = HttpClients.custom().build();

    /**
     * Worker constructor
     * 
     * @param socket
     *            socket to read and send data on
     * @param uri
     *            URI to Post data to
     * @param base64
     *            if to base 64 encode and decode data to and from HTTP endpoint
     */
    public Worker(Socket socket, URI uri, boolean base64) {
        this.socket = socket;
        this.uri = uri;
        this.base64 = base64;
    }

    @Override
    public String toString() {
        return Thread.currentThread().getId() + " (" + socket.getInetAddress() + ":" + socket.getPort() + ")";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        log.log(Level.INFO, "Starting work: {0}", this);
        try {
            byte[] buffer;
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            while (true) {
                log.log(Level.FINEST, "Loop step 1: {0}", this);
                buffer = readRequest(is);
                log.log(Level.FINEST, "Loop step 2: {0}", this);
                if (buffer == null) {
                    break; // done
                }
                log.log(Level.FINEST, "Loop step 3: {0}", this);
                buffer = postRequest(buffer);
                log.log(Level.FINEST, "Loop step 4: {0}", this);
                writeResponse(os, buffer);
            }
        } catch (EOFException e) {
            // done
        } catch (IOException e) {
            log.log(Level.WARNING, "Comunication problems with: " + this, e);
        } finally {
            try {
                this.socket.close();
            } catch (IOException e) {
                log.log(Level.WARNING, "Close down problems with: " + this, e);
            }
        }
        log.log(Level.INFO, "Ending work: {0}", this);
    }

    /**
     * Helper function to write a CMP responce to the socket
     * 
     * @param os
     *            output stream to write to
     * @param buffer
     *            the buffer to write
     * @throws IOException
     *             in case of comunication problems
     */
    private void writeResponse(OutputStream os, byte[] buffer) throws IOException {
        if (this.base64) {
            os.write(Base64.decodeBase64(buffer));
        } else {
            os.write(buffer);
        }
    }

    /**
     * Helper function to post a request to HTTP end point.
     * 
     * @param buffer
     *            the buffet to post
     * @return The raw return bytes that was returned for HTTP end point.
     * @throws IOException
     *             if there are communication problems with HTTP end point
     */
    private byte[] postRequest(byte[] buffer) throws IOException {
        HttpEntity entity = null;
        try {
            HttpPost httpPost = new HttpPost(this.uri);
            httpPost.setHeader("Content-Type", "application/pkixcmp");
            httpPost.setEntity(new ByteArrayEntity(buffer));
            CloseableHttpResponse response = httpclient.execute(httpPost);
            entity = response.getEntity();
            return EntityUtils.toByteArray(entity);
        } finally {
            if (entity != null) {
                EntityUtils.consume(entity);
            }
        }
    }

    /**
     * Helper function to read a request form socket.
     * 
     * @param is
     *            input socket to read CMP packet from
     * @return the CMP packet as raw bytes
     * @throws IOException
     *             in case of socket error or if data does not match lengths
     */
    private byte[] readRequest(InputStream is) throws IOException {
        byte[] buffer;
        DataInputStream dis = new DataInputStream(is);
        int msglen = dis.readInt();

        buffer = readBytes(is, 3);
        buffer = readBytes(is, msglen - 3);

        if (this.base64) {
            return Base64.encodeBase64(buffer);
        }
        return buffer;
    }

    /**
     * Helper function to read nr bytes form a stream and return a byte buffer
     * 
     * @param is
     *            input stream to read from
     * @param nr
     *            number of bytes to read
     * @return byte array with the read data
     * @throws IOException
     *             in case of problems
     */
    private byte[] readBytes(InputStream is, int nr) throws IOException {
        List<Byte> result = new LinkedList<>();
        for (int i = 0; i < nr; i++) {
            result.add((byte) is.read());
        }
        return ArrayUtils.toPrimitive(result.toArray(new Byte[] {}));
    }
}
