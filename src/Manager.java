import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Manager {
    private String inputAddress;
    private List<URL> listOfURL = new ArrayList<>();
    private int numOfConnections;
    public static int fileSize; //in bytes
    public static String fileName;
    public static int numOfChunks;
    public static int chunkSize = 1024;
    public static BlockingQueue<Chunk> blocksQueue;
    public static boolean[] metadata;
    public static String filePath;

    public Manager (String inputAddress, int numOfConnections) {
        this.numOfConnections = numOfConnections;
        this.inputAddress = inputAddress;
        this.blocksQueue = new LinkedBlockingQueue<>();
    }


    public void init() {
        try {
            // convert input into list of URL
            List<String> stringURLs = new ArrayList<>();
            boolean isSingleUrl = this.inputAddress.contains("http");
            stringURLs.add(inputAddress);
            stringURLs = isSingleUrl ? stringURLs : Files.readAllLines(Paths.get(inputAddress));
            for(int i = 0; i < stringURLs.size(); i++) {
                URL url = new URL(stringURLs.get(i));
                this.listOfURL.add(url);
            }

            // Check file size and count Chunks
            HttpURLConnection connection = (HttpURLConnection) listOfURL.get(0).openConnection();
            connection.connect();
            this.fileSize = connection.getContentLength();
            this.numOfChunks = this.fileSize / this.chunkSize;
            if (this.fileSize % this.chunkSize > 0) {
                this.numOfChunks++;
            }

            //init file name
            String urlStringAddress = stringURLs.get(0);
            this.fileName = urlStringAddress.substring(urlStringAddress.lastIndexOf('/') + 1);
            this.filePath = this.fileName.substring(0, this.fileName.lastIndexOf('.'));
            this.metadata = new boolean[numOfChunks];
            separateIntoThreads();
        }
        catch (IOException err) {
            System.err.println(err);
        }
    }


    public void separateIntoThreads() {
        int numberOfChunksWritten = 0;
        String metadataPath = this.filePath + ".tmp";
        // if file already exists -> load the file and resume the download
        if (new File(metadataPath).exists()) {
            System.out.println("in resume process");
            try {
                ObjectInputStream is = new ObjectInputStream(new FileInputStream(metadataPath));
                this.metadata = (boolean[]) is.readObject();
                is.close();
                for (int i = 0; i < this.metadata.length; i++) {
                    if (this.metadata[i]) numberOfChunksWritten++;
                }
            } catch (Exception error) {
                System.err.println("InterruptedException: " + error.toString());
                System.out.println("Download failed");
                System.exit(1);
            }
        }

        // start Reader threads
        int bytesPerThread = (this.numOfChunks / this.numOfConnections) * this.chunkSize;
        System.out.println("normal thread size =" + bytesPerThread);
        for (int i = 0; i < numOfConnections; i++) {
            long startIndex = bytesPerThread * i;
            long endIndex = (i == this.numOfConnections -1) ? this.fileSize-1 : startIndex + (bytesPerThread)-1;
            URL randomURL = this.listOfURL.get(new Random().nextInt(this.listOfURL.size()));
            Thread thread = new Thread(new Reader(startIndex, endIndex, randomURL));
            thread.start();
        }

        // start Writer thread
        Writer writer = new Writer(numberOfChunksWritten);
        Thread writerThread = new Thread(writer);
        writerThread.start();
    }
}
