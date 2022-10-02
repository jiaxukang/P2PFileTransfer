package comp90015.idxsrv.peer;

import comp90015.idxsrv.filemgr.FileMgr;
import comp90015.idxsrv.message.*;
import comp90015.idxsrv.server.IndexElement;
import comp90015.idxsrv.textgui.ISharerGUI;

import java.io.IOException;
import java.net.Socket;
import java.util.Base64;

public class ConcurrentDownload extends Thread {
    private Socket socketPeer;
    private int blockId;
    private IndexElement hit;
    private RWMsg rwMsg;
    private FileMgr fileMgrp;
    private ISharerGUI tgui;
    private int times;
    private int fileMaxBlock;

    /*
    constructor for thread used between peers when downloading blocks in order to do the downloading
    concurrently(improve efficiency)
     */
    public ConcurrentDownload(Socket socketPeer,
                              int blockId,
                              IndexElement hit,
                              RWMsg rwMsg,
                              FileMgr fileMgr,
                              ISharerGUI tgui,
                              int times,
                              int fileMaxBlock) {
        this.blockId = blockId;
        this.socketPeer = socketPeer;
        this.hit = hit;
        this.rwMsg = rwMsg;
        this.fileMgrp = fileMgr;
        this.tgui = tgui;
        this.times = times;
        this.fileMaxBlock = fileMaxBlock;
    }

    /*
    each thread has its assigned blocks to download, it keeps read and write message in a for loop
    until a specific blockId is reached, then output goodbye message and catch any exceptions
    during the process
     */
    @Override
    public void run() {
        IOSocket ioSocketPeer = null;
        try {
            ioSocketPeer = new IOSocket(socketPeer);
            for (int i = blockId; i < blockId + times; i++) {
                BlockRequest blockRequest = new BlockRequest(hit.filename, hit.fileDescr.getFileMd5(), i);
                rwMsg.writeMsg(ioSocketPeer.bufferedWriter, blockRequest, tgui);
                BlockReply blockReply = (BlockReply) rwMsg.readMsg(ioSocketPeer.bufferedReader, tgui);
                byte[] bytes = Base64.getDecoder().decode(blockReply.bytes);
                fileMgrp.writeBlock(i, bytes);

                if (i >= fileMaxBlock - 1) {
                    Goodbye goodbye = new Goodbye();
                    rwMsg.writeMsg(ioSocketPeer.bufferedWriter, goodbye, tgui);
                    tgui.logInfo("DownLoad Successfully!");
                    return;
                }
            }
        } catch (JsonSerializationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return;
        }

    }
}
