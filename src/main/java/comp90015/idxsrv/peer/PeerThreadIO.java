package comp90015.idxsrv.peer;

import comp90015.idxsrv.filemgr.BlockUnavailableException;
import comp90015.idxsrv.filemgr.FileDescr;
import comp90015.idxsrv.filemgr.FileMgr;
import comp90015.idxsrv.message.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class PeerThreadIO extends Thread {

    public Socket socket;

    public PeerThreadIO(Socket socket) {
        this.socket = socket;
    }

    /*
    each block assigned to a specific thread will be downloaded from here
    if message is a blockRequest, bytes in the block will be allocated into a randomAccessFile
    and create a blockReply
    if message is a goodbye, ignore it
    catch any exceptions during the process
     */
    @Override
    public void run() {
        try {
            RWMsg rwMsg = new RWMsg();
            while (true) {
                IOSocket ioSocket = new IOSocket(socket);
                Message msg = rwMsg.readMsg(ioSocket.bufferedReader);
                String msgname = msg.getClass().getName();
                if (msgname == BlockRequest.class.getName()) {
                    BlockRequest blockRequest = (BlockRequest) msg;
                    RandomAccessFile randomAccessFile = new RandomAccessFile(blockRequest.filename, "r");
                    FileDescr fileDescrPeer = new FileDescr(randomAccessFile);
                    FileMgr fileMgr = new FileMgr(blockRequest.filename, fileDescrPeer);
                    byte[] bytes = fileMgr.readBlock(blockRequest.blockIdx);
                    String fileMd5 = fileDescrPeer.getBlockMd5(blockRequest.blockIdx);
                    String str = Base64.getEncoder().encodeToString(bytes);
                    BlockReply blockReply = new BlockReply(blockRequest.filename, fileMd5, blockRequest.blockIdx, str);
                    rwMsg.writeMsg(ioSocket.bufferedWriter, blockReply);
                } else if (msgname == Goodbye.class.getName()) {
                    return;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonSerializationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BlockUnavailableException e) {
            e.printStackTrace();
        }
    }
}
