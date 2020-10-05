import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Writer implements Runnable{
    private File downloadedFile;
    private File metadataFile;
    private File tempMetadataFile;
    private int numberOfChunksWritten;
    private int lastPercentagePrinted = 0;
    private int currentPercentagePrinted = 0;

    public Writer (int numberOfChunksWritten) {
        this.downloadedFile = new File(System.getProperty("user.dir") + '/' + Manager.fileName);
        this.numberOfChunksWritten = numberOfChunksWritten;
        this.metadataFile = new File(Manager.filePath + ".tmp");
        this.tempMetadataFile = new File(Manager.filePath + ".tmp_copy");
    }


    public void run() {
        try {
            System.out.println("start running writer");
            RandomAccessFile destFile = new RandomAccessFile(this.downloadedFile, "rw");
            ObjectOutputStream oos;

            while (this.numberOfChunksWritten != Manager.numOfChunks) {
                Chunk chunk = Manager.blocksQueue.take();
                // write the chunk data to the destination file
                destFile.seek(chunk.getStartChunk());
                destFile.write(chunk.getData(), 0, chunk.getChunkSize());
                // update the metadata file
                int chunkIndex = (int) (chunk.getStartChunk() / Manager.chunkSize);
                Manager.metadata[chunkIndex] = true;
                oos = new ObjectOutputStream(new FileOutputStream(this.tempMetadataFile));
                oos.writeObject(Manager.metadata);
                oos.flush();
                oos.close();

                // get metadata system paths
                Path oldMetadata = this.metadataFile.toPath();
                Path newMetadata = this.tempMetadataFile.toPath();
                // swap files in one atomic operation
                Files.move(newMetadata, oldMetadata, REPLACE_EXISTING);

                // finishing the chunk download
                numberOfChunksWritten++;

                // print current percentage if needed
                currentPercentagePrinted = (int) Math.ceil(((double) numberOfChunksWritten / Manager.numOfChunks) * 100);
                if (currentPercentagePrinted > lastPercentagePrinted) {
                    System.out.println("Downloaded " + currentPercentagePrinted + "%");
                    lastPercentagePrinted = currentPercentagePrinted;
                }
            }
            destFile.close();
            System.out.println("Download succeed");
            this.metadataFile.delete();
            this.tempMetadataFile.delete();
        } catch (InterruptedException error) {
            System.err.println("InterruptedException: " + error.toString());
            System.exit(0);
        } catch (FileNotFoundException error) {
            System.err.println("FileNotFoundException: " + error.toString());
            System.exit(0);
        } catch (IOException error) {
            System.err.println("IOException: " + error.toString());
            System.exit(0);
        }
    }
}
