public class Main {

    public static void main(String [] args) {
        if(args.length > 2 || args.length < 1){
            System.err.println("unexpected input");
            System.exit(1);
        }
        String url = args[0];
        int numOfConnections = 1;
        if (args.length > 1) numOfConnections = Integer.parseInt(args[1]);

        Manager letsStart = new Manager(url, numOfConnections);
        letsStart.init();
    }
}
