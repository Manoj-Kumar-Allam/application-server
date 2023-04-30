package appserver.server;

import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.comm.ConnectivityInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import utils.PropertyHandler;

/**
 * The class [Server] represents the Application Server
 * 
 * It listens for incoming client connections and spawns ServerThread objects
 * to handle each incoming request.
 * 
 * @author manoj
 */
public class Server {

    // Singleton objects - there is only one of them. For simplicity, this is not enforced though ...
    static SatelliteManager satelliteManager = null;
    static LoadManager loadManager = null;
    static ServerSocket serverSocket = null;

    /**
     * Constructor for the Server class.
     * Initializes the satellite manager and load manager, and creates a server socket.
     * 
     * @param serverPropertiesFile the path to the server properties file
     */
    public Server(String serverPropertiesFile) {

        Properties properties;

        // create satellite manager and load manager
        satelliteManager = new SatelliteManager();
        loadManager = new LoadManager();
        
        // read properties and create server socket
        try {
            properties = new PropertyHandler(serverPropertiesFile);
            
            // get host name and port
            String name = properties.getProperty("NAME");
            System.out.println("[Server.Server] NAME: " + name);
            int port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("[Server.Server] Port: " + port);
            
            // create a server socket
            serverSocket = new ServerSocket(port);
            System.out.println("[Server.Server] Waiting for connections on Port #" + port);
        } catch (IOException ex) {
            System.err.println("[Server.Server] Severe: " + ex.getMessage());
        }
    }
    
    /**
     * The run method listens for incoming client connections in a server loop.
     * 
     * When a request comes in, a ServerThread object is spawned to handle the request.
     */
    public void run() {
        // serve clients in server loop ...
        // when a request comes in, a ServerThread object is spawned
        while (true) {
            try {
                new ServerThread(serverSocket.accept()).start();
            } catch (IOException ex) {
                System.out.println("[Server.run] Severe: unable to create connection");
            }
            System.out.println("[Server.run] A connection to a client is established!");
        }
    }

    // objects of this helper class communicate with satellites or clients
    private class ServerThread extends Thread {

        Socket client = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;
        
        /**
         * Constructor for the ServerThread class.
         * 
         * Initializes the object streams and reads the incoming message.
         * @param client the client socket
         */
        private ServerThread(Socket client) {
            this.client = client;
        }
        
        /**
         * The run method processes the incoming message and responds appropriately.
         */
        @Override
        public void run() {
            // set up object streams and read message
            try {
                readFromNet = new ObjectInputStream(client.getInputStream());
                writeToNet = new ObjectOutputStream(client.getOutputStream());
                message = (Message) readFromNet.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                System.err.println("[ServerThread.run] Sever: Error in opening object streams");
            }

            
            // process message
            switch (message.getType()) {
                
                // handle register satellite request 
                case REGISTER_SATELLITE:
                    // read satellite info
                    ConnectivityInfo connectivityInfo = (ConnectivityInfo) message.getContent();
                    
                    // register satellite
                    synchronized (Server.satelliteManager) {
                        Server.satelliteManager.registerSatellite(connectivityInfo);
                    }

                    // add satellite to loadManager
                    synchronized (Server.loadManager) {
                        Server.loadManager.satelliteAdded(connectivityInfo.getName());
                    }

                    break;
                
                // handle job request    
                case JOB_REQUEST:
                    System.err.println("\n[ServerThread.run] Received job request");

                    String satelliteName = null;
                    synchronized (Server.loadManager) {
                        // get next satellite from load manager
                        try {
                            satelliteName = Server.loadManager.nextSatellite();
                            
                            System.out.println("\n [ServerThread.run] job request handled by " + satelliteName );
                        } catch(Exception ex) {
                            System.err.println("[ServerThread.run] " + ex.getMessage());
                        }
                        
                        // get connectivity info for next satellite from satellite manager
                        connectivityInfo = Server.satelliteManager.getSatelliteForName(satelliteName);
                    }

                    Socket satellite = null;
                    Integer result = null;
                    
                    try {
                        // connect to satellite
                        satellite = new Socket(connectivityInfo.getHost(), connectivityInfo.getPort());
                        
                        // open object streams
                        ObjectOutputStream writeToNet = new ObjectOutputStream(satellite.getOutputStream());
                        writeToNet.writeObject(message);
                        
                        ObjectInputStream readFromNet = new ObjectInputStream(satellite.getInputStream());
                        // get the result
                        result = (Integer) readFromNet.readObject();
                        
                    } catch (IOException | ClassNotFoundException ex) {
                        System.err.println("[ServerThread.run] Severe: " + ex.getMessage());
                    }
                    
                    // send back the result to client
                    try {
                        writeToNet.writeObject(result);
                    } catch (IOException ex) {
                        System.err.println("[ServerThread.run] Severe: " + ex.getMessage());
                    }
          
                    break;

                default:
                    System.err.println("[ServerThread.run] Warning: Message type not implemented");
            }
        }
    }

    // main()
    public static void main(String[] args) {
        // start the application server
        Server server = null;
        if(args.length == 1) {
            server = new Server(args[0]);
        } else {
            server = new Server("../../config/Server.properties");
        }
        server.run();
    }
}
