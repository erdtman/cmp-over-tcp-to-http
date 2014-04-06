package cmp.over.tcp.to.http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * http://pic.dhe.ibm.com/infocenter/zos/v1r12/index.jsp?topic=%2Fcom.ibm.zos.
 * r12.ikya100%2Fcmp.htm
 * 
 * http://tools.ietf.org/html/draft-ietf-pkix-cmp-tcp-00
 * 
 * http://tools.ietf.org/html/draft-ietf-pkix-cmp-http-00
 * 
 * @author Samuel Erdtman
 * 
 */
public class TcpHttp {
    /** Logger instance */
    private final static Logger log = Logger.getLogger(TcpHttp.class.getName());

    /**
     * Main method for starting application
     * 
     * @param args
     *            program arguments
     * @throws IOException
     *             on close down errors
     * @throws InterruptedException
     *             on close down interruption
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        log.log(Level.INFO, "Starting application...");
        ExecutorService executor = Executors.newFixedThreadPool(30);
        ServerSocket serverSocket = null;
        try {
            URI uri = new URI("http://google.com");
            serverSocket = new ServerSocket(7777);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                log.log(Level.FINE, "New connection from: {0}:{1}", new Object[] { clientSocket.getInetAddress(), clientSocket.getLocalPort() });
                executor.execute(new Worker(clientSocket, uri, false));
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Socket comunication error", e);
        } catch (URISyntaxException e) {
            log.log(Level.SEVERE, "Configured HTTP endpoint error", e);
        } finally {
            if (serverSocket != null) {
                serverSocket.close();
            }
            log.log(Level.INFO, "Waiting for application to close down...");
            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
                log.log(Level.FINEST, "Shutdown loop");
            }

            log.log(Level.INFO, "Application terminated");
        }
    }
}
