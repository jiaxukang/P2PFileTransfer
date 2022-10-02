package comp90015.idxsrv.peer;


import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
import comp90015.idxsrv.message.*;
import comp90015.idxsrv.server.IOThread;
import comp90015.idxsrv.server.IndexElement;
import comp90015.idxsrv.textgui.ISharerGUI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Skeleton Peer class to be completed for Project 1.
 *
 * @author aaron
 */
public class Peer implements IPeer {

    private final IOThread ioThread;

    private LinkedBlockingDeque<Socket> incomingConnections;

    private final ISharerGUI tgui;

    private final String basedir;

    private final int timeout;

    private final int port;

    private final ServerSocket serverSocket;

    private final int serverPort;

    private final PeerServer peerServer;

    public Peer(int port, String basedir,
                int socketTimeout,
                ISharerGUI tgui) throws IOException {
        this.tgui = tgui;
        this.port = port;
        this.timeout = socketTimeout;
        this.basedir = new File(basedir).getCanonicalPath();
        ioThread = new IOThread(port, incomingConnections, socketTimeout, tgui);
        ioThread.start();
        serverSocket = new ServerSocket(0);
        this.serverPort = serverSocket.getLocalPort();
        this.peerServer = new PeerServer(serverSocket, incomingConnections, socketTimeout);
        peerServer.start();
    }

    public void shutdown() throws InterruptedException, IOException {
        ioThread.shutdown();
        ioThread.interrupt();
        ioThread.join();
    }

    /*
     * Students are to implement the interface below.
     */

    @Override
    public void shareFileWithIdxServer(File file, InetAddress idxAddress, int idxPort, String idxSecret,
                                       String shareSecret) {
        try {
            //create a socket and check Authenticate
            Socket socket = new Socket(idxAddress, idxPort);
            IOSocket ioSocket = new IOSocket(socket);
            AuthenticateReply authenticateReply = ioSocket.checkAuthenticate(idxSecret, tgui);
            tgui.logDebug(authenticateReply.toString());

            //send share requests
            RWMsg rwMsg = new RWMsg();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            FileDescr fileDescr = new FileDescr(randomAccessFile);
            ShareRequest shareRequest = new ShareRequest(fileDescr, file.getName(), shareSecret, serverPort);
            rwMsg.writeMsg(ioSocket.bufferedWriter, shareRequest, tgui);
            ShareReply shareReply = (ShareReply) rwMsg.readMsg(ioSocket.bufferedReader, tgui);

            //add record to gui
            FileMgr fileMgr = new FileMgr(file.getName(), fileDescr);
            int numShares = shareReply.numSharers;
            String status = "STATUS";
            ShareRecord shareRecord = new ShareRecord(fileMgr, numShares, status, idxAddress,
                    idxPort, idxSecret, shareSecret);
            tgui.addShareRecord(file.getCanonicalPath(), shareRecord);
        } catch (FileNotFoundException e) {
            tgui.logError("File Not Found.");
            e.printStackTrace();
        } catch (JsonSerializationException e) {
            tgui.logError("Wrong Type!");
            e.printStackTrace();
        } catch (IOException e) {
            tgui.logError("IO Error!");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            tgui.logError("Algorithm Error!");
            e.printStackTrace();
        } finally {
            tgui.logInfo("File shared successful!");
        }
    }

    @Override
    public void searchIdxServer(String[] keywords,
                                int maxhits,
                                InetAddress idxAddress,
                                int idxPort,
                                String idxSecret) {
        tgui.clearSearchHits();
        try {
            //create a socket and check Authenticate
            Socket socket = new Socket(idxAddress, idxPort);
            IOSocket ioSocket = new IOSocket(socket);
            AuthenticateReply authenticateReply = ioSocket.checkAuthenticate(idxSecret, tgui);
            tgui.logDebug(authenticateReply.toString());

            //send request and get response
            SearchRequest searchRequest = new SearchRequest(maxhits, keywords);
            RWMsg rwMsg = new RWMsg();
            rwMsg.writeMsg(ioSocket.bufferedWriter, searchRequest, tgui);
            SearchReply searchReply = (SearchReply) rwMsg.readMsg(ioSocket.bufferedReader, tgui);
            for (int i = 0; i < searchReply.hits.length; i++) {
                IndexElement hit = searchReply.hits[i];
                FileDescr fileDescr = hit.fileDescr;
                String filename = hit.filename;
                Integer seedCount = searchReply.seedCounts[i];
                String secret = hit.secret;
                SearchRecord searchRecord = new SearchRecord(fileDescr, seedCount,
                        idxAddress, idxPort, idxSecret, secret);
                tgui.addSearchHit(filename, searchRecord);
            }
        } catch (FileNotFoundException e) {
            tgui.logError("File Not Found.");
            e.printStackTrace();
        } catch (JsonSerializationException e) {
            tgui.logError("Wrong Type!");
            e.printStackTrace();
        } catch (IOException e) {
            tgui.logError("IO Error!");
            e.printStackTrace();
        } finally {
            tgui.logInfo("File search successful!");
        }
    }

    @Override
    public boolean dropShareWithIdxServer(String relativePathname, ShareRecord shareRecord) {
        InetAddress idxAddress = shareRecord.idxSrvAddress;
        int idxPort = shareRecord.idxSrvPort;
        String idxSecret = shareRecord.idxSrvSecret;
        try {
            //create a socket and check Authenticate
            Socket socket = new Socket(idxAddress, idxPort);
            IOSocket ioSocket = new IOSocket(socket);
            AuthenticateReply authenticateReply = ioSocket.checkAuthenticate(idxSecret, tgui);
            tgui.logDebug(authenticateReply.toString());

            //deal with data
            String word = "";
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                word = "\\\\";
            } else {
                word = "/";
            }
            String[] lines = relativePathname.split(word);
            String fileName = lines[lines.length - 1];
            FileMgr fileMgr = shareRecord.fileMgr;
            FileDescr fileDescr = fileMgr.getFileDescr();
            String fileMd5 = fileDescr.getFileMd5();
            String sharerSecret = shareRecord.sharerSecret;
            DropShareRequest dropShareRequest = new DropShareRequest(fileName, fileMd5, sharerSecret, serverPort);

            //send request and get message
            RWMsg rwMsg = new RWMsg();
            rwMsg.writeMsg(ioSocket.bufferedWriter, dropShareRequest, tgui);
            DropShareReply dropShareReply = (DropShareReply) rwMsg.readMsg(ioSocket.bufferedReader, tgui);
            tgui.logDebug(dropShareReply.toString());
            return dropShareReply.success;
        } catch (FileNotFoundException e) {
            tgui.logError("File Not Found.");
            e.printStackTrace();
        } catch (JsonSerializationException e) {
            tgui.logError("Wrong Type!");
            e.printStackTrace();
        } catch (IOException e) {
            tgui.logError("IO Error!");
            e.printStackTrace();
        } finally {
            tgui.logInfo("File drop successful!");
        }
        return false;
    }

    @Override
    public void downloadFromPeers(String relativePathname, SearchRecord searchRecord) {
        InetAddress idxAddress = searchRecord.idxSrvAddress;
        int idxPort = searchRecord.idxSrvPort;
        String idxSecret = searchRecord.idxSrvSecret;
        try {
            //create a socket and check Authenticate
            Socket socket = new Socket(idxAddress, idxPort);
            IOSocket ioSocket = new IOSocket(socket);
            AuthenticateReply authenticateReply = ioSocket.checkAuthenticate(idxSecret);

            //deal with data
            String word = "";
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                word = "\\\\";
            } else {
                word = "/";
            }
            String[] lines = relativePathname.split(word);
            String fileName = lines[lines.length - 1];
            FileDescr fileDescr = searchRecord.fileDescr;
            String fileMd5 = fileDescr.getFileMd5();
            String sharerSecret = searchRecord.sharerSecret;

            //send and reply message
            LookupRequest lookupRequest = new LookupRequest(fileName, fileMd5);
            RWMsg rwMsg = new RWMsg();
            rwMsg.writeMsg(ioSocket.bufferedWriter, lookupRequest);
            LookupReply lookupReply = (LookupReply) rwMsg.readMsg(ioSocket.bufferedReader);
            if (lookupReply.hits.length == 0) {
                return;
            }

            concurrentDownd(lookupReply, relativePathname, rwMsg);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonSerializationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void concurrentDownd(LookupReply lookupReply, String relativePathname, RWMsg rwMsg) {
        int blockId = 0;
        try {
            //create a new file
            RandomAccessFile file = new RandomAccessFile(relativePathname, "rw");
            FileDescr fileDescrPeer = new FileDescr(file);
            FileMgr fileMgrp = new FileMgr(relativePathname, fileDescrPeer);
            int fileMaxBlock = lookupReply.hits[0].fileDescr.getNumBlocks();

            //get all available socket
            HashMap<IndexElement, Socket> avaliableSocket = new HashMap<>();
            for (IndexElement hit : lookupReply.hits) {
                Socket socketPeer;
                try {
                    socketPeer = new Socket(hit.ip, hit.port);
                    avaliableSocket.put(hit, socketPeer);
                } catch (IOException e) {
                    tgui.logError("One seek server is closed!");
                    e.printStackTrace();
                }
            }
            if (avaliableSocket.size() == 0) {
                tgui.logError("Not found available seek.");
                return;
            }

            int times = (int) Math.ceil((float) fileMaxBlock / avaliableSocket.size());

            //create concurrently thread to download
            for (IndexElement hit : avaliableSocket.keySet()) {
                ConcurrentDownload concurrentDownload = new ConcurrentDownload(avaliableSocket.get(hit), blockId,
                        hit, rwMsg, fileMgrp, tgui, times, fileMaxBlock);
                concurrentDownload.start();
                blockId = blockId + times;
            }
        } catch (FileNotFoundException e) {
            tgui.logError("File Not Found.");
            e.printStackTrace();
        } catch (IOException e) {
            tgui.logError("IO Error!");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
