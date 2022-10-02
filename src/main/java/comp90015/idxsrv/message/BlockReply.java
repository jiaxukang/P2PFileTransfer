package comp90015.idxsrv.message;

@JsonSerializable
public class BlockReply extends Message{

    @JsonElement
    public String filename;
    @JsonElement
    public String fileMd5;
    @JsonElement
    public Integer blockIdx;
    @JsonElement
    public String bytes;

    public BlockReply(){

    }

    // constructor for blockRepley used between peers
    public BlockReply(String filename, String fileMd5, int blockId, String bytes) {
        this.filename = filename;
        this.fileMd5 = fileMd5;
        this.blockIdx = blockId;
        this.bytes = bytes;
    }
}
