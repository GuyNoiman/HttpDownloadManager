public class Chunk {
    private byte[] data;
    private long startIndex;
    private int size;

    public Chunk (byte[] data, long currentIndex, int size) {
        this.data = data;
        this.startIndex = currentIndex;
        this.size = size;
    }

    public byte[] getData() {
        return this.data;
    }

    public long getStartChunk() {
        return this.startIndex;
    }

    public int getChunkSize() {
        return this.size;
    }
}
