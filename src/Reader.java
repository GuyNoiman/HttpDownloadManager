import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Reader implements Runnable{
    private long startIndex;
    private long endIndex;
    private URL url;


    public Reader (long start, long end, URL url){
        this.startIndex = start;
        this.endIndex = end;
        this.url = url;
    }


    public void run() {
        try {
            System.out.println("start running Reader");

            //in resume case : skipping on the first downloaded chunks
            int chunkSize = Manager.chunkSize;
            int indexOfUnwritten = (int)this.startIndex / Manager.chunkSize;
            while(indexOfUnwritten * Manager.chunkSize < this.endIndex) {
                if(Manager.metadata[indexOfUnwritten]) {
                    indexOfUnwritten++;
                } else {
                    break;
                }
            }

            long updateStartBlock = indexOfUnwritten * Manager.chunkSize;
            boolean isFinished = updateStartBlock > this.endIndex;
            if(!isFinished) {
                // open inputStream to the given URL
                HttpURLConnection connection = (HttpURLConnection) this.url.openConnection();
                connection.setRequestProperty("Range", "bytes=" + updateStartBlock + "-" + this.endIndex);
                connection.connect();
                connection.setReadTimeout(6000); // for network failure handling - 1 min waiting
                InputStream inputStream = connection.getInputStream();

                //add chunks to the blocksQueue
                long currentLocation = this.startIndex;
                while (currentLocation < this.endIndex) {
                    int currentIndex = (int) currentLocation / Manager.chunkSize;
                    // to deal with the last chunk different size
                    if (currentIndex == Manager.numOfChunks-1) chunkSize = (int) (this.endIndex - currentLocation + 1);

                    if (Manager.metadata[currentIndex]) {
                        currentLocation += chunkSize;
                        continue;
                    }
                    byte[] buffer = new byte[chunkSize];
                    inputStream.readNBytes(buffer, 0, chunkSize);
                    Chunk currentChunk = new Chunk(buffer, currentLocation, chunkSize);
                    Manager.blocksQueue.put(currentChunk);
                    currentLocation += chunkSize;
                }
                inputStream.close();
                connection.disconnect();
            }
        } catch (IOException error) {
            System.err.println("IOException: " + error.toString());
            System.exit(0);
        } catch (InterruptedException error) {
            System.err.println("InterruptedException: " + error.toString());
            System.exit(0);
        }
    }
}
