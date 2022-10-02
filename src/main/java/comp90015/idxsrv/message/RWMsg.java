package comp90015.idxsrv.message;

import comp90015.idxsrv.textgui.ISharerGUI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class RWMsg {
    private BufferedWriter bufferedWriter;
    private ISharerGUI tgui;
    private BufferedReader bufferedReader;

    public RWMsg (){

    }

    //output the message information to console
    public void writeMsg(BufferedWriter bufferedWriter, Message msg,ISharerGUI tgui) throws IOException {
        tgui.logDebug("sending: " + msg.toString());
        bufferedWriter.write(msg.toString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }
    public void writeMsg(BufferedWriter bufferedWriter, Message msg) throws IOException {
        bufferedWriter.write(msg.toString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    //read message information from console to create a mseeage
    public Message readMsg(BufferedReader bufferedReader) throws IOException, JsonSerializationException {
        String jsonStr = bufferedReader.readLine();
        if (jsonStr != null) {
            Message msg = (Message) MessageFactory.deserialize(jsonStr);
            return msg;
        } else {
            throw new IOException();
        }
    }
    public Message readMsg(BufferedReader bufferedReader,ISharerGUI tgui) throws IOException, JsonSerializationException {
        String jsonStr = bufferedReader.readLine();
        if (jsonStr != null) {
            Message msg = (Message) MessageFactory.deserialize(jsonStr);
            tgui.logDebug("received: " + msg.toString());
            return msg;
        } else {
            throw new IOException();
        }
    }
}
