package comp90015.idxsrv.peer;

import comp90015.idxsrv.message.AuthenticateReply;
import comp90015.idxsrv.message.AuthenticateRequest;
import comp90015.idxsrv.message.JsonSerializationException;
import comp90015.idxsrv.message.RWMsg;
import comp90015.idxsrv.textgui.ISharerGUI;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class IOSocket {

    public BufferedReader bufferedReader;
    public BufferedWriter bufferedWriter;

    public IOSocket() {

    }

    public IOSocket(Socket socket) throws IOException {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    /*
    check the authentication of each reply
    if successfully return the authenticateReply, output an error message otherwise
    and catch any exceptions
     */

    public AuthenticateReply checkAuthenticate(String idxSecret, ISharerGUI tgui) {
        AuthenticateReply authenticateReply = null;
        try {
            RWMsg rwMsg = new RWMsg();
            rwMsg.readMsg(this.bufferedReader, tgui);
            AuthenticateRequest authenticateRequest = new AuthenticateRequest(idxSecret);
            rwMsg.writeMsg(this.bufferedWriter, authenticateRequest, tgui);
            authenticateReply = (AuthenticateReply) rwMsg.readMsg(this.bufferedReader, tgui);
            if (!authenticateReply.success) {
                tgui.logError("You do not have permission to share!");
            }
        } catch (JsonSerializationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return authenticateReply;
    }

    public AuthenticateReply checkAuthenticate(String idxSecret) {
        AuthenticateReply authenticateReply = null;
        try {
            RWMsg rwMsg = new RWMsg();
            rwMsg.readMsg(this.bufferedReader);
            AuthenticateRequest authenticateRequest = new AuthenticateRequest(idxSecret);
            rwMsg.writeMsg(this.bufferedWriter, authenticateRequest);
            authenticateReply = (AuthenticateReply) rwMsg.readMsg(this.bufferedReader);
            if (!authenticateReply.success) {
            }
        } catch (JsonSerializationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return authenticateReply;
    }
}
