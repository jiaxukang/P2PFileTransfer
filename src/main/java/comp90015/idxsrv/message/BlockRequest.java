package comp90015.idxsrv.message;

@JsonSerializable
public class BlockRequest extends Message {


    @JsonElement
    public String filename;
    @JsonElement
    public String fileMd5;
    @JsonElement
    public Integer blockIdx;

    public BlockRequest() {

    }

    // constructor for blockRequest used between peers
    public BlockRequest(String filename, String fileMd5, int blockId) {
        this.filename = filename;
        this.fileMd5 = fileMd5;
        this.blockIdx = blockId;
    }
}
