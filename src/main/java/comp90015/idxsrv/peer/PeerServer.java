package comp90015.idxsrv.peer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;
import comp90015.idxsrv.textgui.ITerminalLogger;

public class PeerServer extends Thread {

    private ServerSocket serverSocket;
    private LinkedBlockingDeque<Socket> incomingConnections;
    private int socketTimeout;

    /*
    find(accept) all available connections and create a thread for each socket and start
     */
    public PeerServer(ServerSocket serverSocket,
                      LinkedBlockingDeque<Socket> incomingConnections,
                      int socketTimeout) {
        this.serverSocket = serverSocket;
        this.incomingConnections = incomingConnections;
        this.socketTimeout = socketTimeout;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(this.socketTimeout);
                PeerThreadIO peerThreadIO = new PeerThreadIO(socket);
                peerThreadIO.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public PeerServer(ServerSocket socket) {
        this.serverSocket = socket;
    }
}
