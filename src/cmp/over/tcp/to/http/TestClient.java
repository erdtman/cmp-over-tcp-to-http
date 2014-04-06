package cmp.over.tcp.to.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class TestClient {

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        Socket socket = new Socket("localhost", 7777);
        socket.getOutputStream().write(new byte[]{0,0,0,4,3,3,3,65});
        InputStream is = socket.getInputStream();
        int data;
        while((data = is.read()) != -1){
            System.out.print(new Character((char) data));
        }
        socket.close();
    }

}
