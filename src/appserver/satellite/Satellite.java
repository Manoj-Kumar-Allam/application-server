package appserver.satellite;

import appserver.job.Job;
import appserver.comm.ConnectivityInfo;
import appserver.job.UnknownToolException;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;

import appserver.job.Tool;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.PropertyHandler;

/**
 * The [Satellite] class acts like an computing node that execute jobs by
 * calling the callback method of tool a implementation, loading the tool's code 
 * dynamically over a network or locally from the cache, if a tool got executed before.
 *
 * @author Dr.-Ing. Wolf-Dieter Otte, manoj, sampath, Srinivasa
 */
public class Satellite extends Thread {

    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null;
    private Hashtable<String, Tool> toolsCache = null;

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) {

        Properties properties;
        String host;
        int port;
        String name;

        // read this satellite's properties and populate satelliteInfo object,
        // which later on will be sent to the server
        try {
            properties = new PropertyHandler(satellitePropertiesFile);
            
            System.out.println("=============================================");
            System.out.println("Satellite Configurations");
            System.out.println("=============================================");
            name = properties.getProperty("NAME");
            System.out.println("[Satellite.Satellite] NAME: " + name);
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("[Satellite.Satellite] Port: " + port);

            satelliteInfo.setName(name);
            satelliteInfo.setPort(port);
        } catch (IOException ex) {
            System.err.println("[Satellite.Satellite] Severe: " + ex.getMessage());
        }
        
        
        // read properties of the application server and populate serverInfo object
        // other than satellites, the as doesn't have a human-readable name, so leave it out
        try {
            properties = new PropertyHandler(serverPropertiesFile);
            
            System.out.println("=============================================");
            System.out.println("Application Server Configurations");
            System.out.println("=============================================");
            
            host = properties.getProperty("HOST");
            System.out.println("[Satellite.Satellite] HOST: " + host);
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("[Satellite.Satellite] Port: " + port);

            serverInfo.setHost(host);
            serverInfo.setPort(port);
        } catch (IOException ex) {
            System.err.println("[Satellite.Satellite] Severe: " + ex.getMessage());
        }
        
        // read properties of the code server and create class loader
        // -------------------
        try {
            properties = new PropertyHandler(classLoaderPropertiesFile);
            
            System.out.println("=============================================");
            System.out.println("Webserver Configurations");
            System.out.println("=============================================");
            
            host = properties.getProperty("HOST");
            System.out.println("[Satellite.Satellite] HOST: " + host);
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("[Satellite.Satellite] Port: " + port);

            classLoader = new HTTPClassLoader(host, port);
            if (classLoader == null) {
                System.err.println("Could not create HTTPClassLoader, exiting ...");
                System.exit(1);
            }
        } catch (IOException ex) {
            System.err.println("[Satellite.Satellite] Severe: " + ex.getMessage());
        }

        
        // create tools cache
        // -------------------
        toolsCache = new Hashtable();
        
    }

    @Override
    public void run() {

        // register this satellite with the SatelliteManager on the server
        // ---------------------------------------------------------------
        
        try { 
            // connect to application server
            Socket server = new Socket(serverInfo.getHost(), serverInfo.getPort());
            
            // send request to register satellite
            Message message = new Message(REGISTER_SATELLITE, satelliteInfo);
            
            // sending job out to the application server in a message
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);
            
        } catch (Exception ex) {
            System.err.println("[Satellite.run] Error occurred " + ex.getMessage());
        }
        
        
        // create server socket
        // ---------------------------------------------------------------
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(satelliteInfo.getPort());
        } catch (IOException ex) {
            System.err.println("[Satellite.run] Severe: Failed to create server socket");
        }
        
        
        // start taking job requests in a server loop
        // ---------------------------------------------------------------
        System.out.println("[Satellite.run] Waiting for connections on Port #" + satelliteInfo.getPort());
        while (true) {
            try {
                new SatelliteThread(serverSocket.accept()).start();

            } catch (IOException ex) {
                System.out.println("[Satellite.run] Severe: unable to create connection");
            }
            System.out.println("[Satellite.run] A connection to a client is established!");
        }
    }

    // inner helper class that is instanciated in above server loop and processes single job requests
    private class SatelliteThread extends Thread {

        Satellite satellite = null;
        Socket jobRequest = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        SatelliteThread(Socket jobRequest) {
            this.jobRequest = jobRequest;
        }

        @Override
        public void run() {
            // setting up object streams
            try {
                readFromNet = new ObjectInputStream(jobRequest.getInputStream());
                writeToNet = new ObjectOutputStream(jobRequest.getOutputStream());
            } catch (IOException ex) {
                System.err.println("[SatelliteThread.run] Severe: Error in opening object streams");
            }
            
            // reading message
            try {
                message = (Message) readFromNet.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                System.err.println("[SatelliteThread.run] Severe: Unable to read the message");
            }
            
            switch (message.getType()) {
                case JOB_REQUEST:
                    // processing job request
                    Job job = (Job) message.getContent();
                    try {
                        // get the Tool
                        Tool tool = getToolObject(job.getToolName());
                        // compute the operation
                        Object object = tool.go(job.getParameters());
                        // send back the result to the client
                        writeToNet.writeObject(((Integer) object));
                    } catch (UnknownToolException | ClassNotFoundException | InstantiationException | IllegalAccessException | IOException ex) {
                        System.err.println("[SatelliteThread.run] Severe: " + ex.getMessage());
                    }
                    break;

                default:
                    System.err.println("[SatelliteThread.run] Warning: Message type not implemented");
            }
        }
    }

    /**
     * Aux method to get a tool object, given the fully qualified class string
     * If the tool has been used before, it is returned immediately out of the cache,
     * otherwise it is loaded dynamically
     */
    public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        Tool toolObject = null;
        
        // check whether it is present in the cache or not
        if ((toolObject = (Tool) toolsCache.get(toolClassString)) == null)
        {

            System.out.println("\nOperation's Class: " + toolClassString);
            
            // get the class from webserver
            Class<?> operationClass = classLoader.loadClass(toolClassString);
            try {
                try {
                    // construct object
                    toolObject = (Tool) operationClass.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException | SecurityException ex) {
                    Logger.getLogger(Satellite.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (InvocationTargetException ex) {
                System.err.println("[Satellite] getOperation() - InvocationTargetException");
            }
            
            // store it in the cache
            toolsCache.put(toolClassString, toolObject);
        }
        else
        {
            System.out.println("Operation: \"" + toolClassString + "\" already in Cache");
        }
        
        // return the tool object
        return toolObject;
    }

    public static void main(String[] args) {
        // start the satellite
        Satellite satellite = new Satellite(args[0], args[1], args[2]);
        satellite.run();
    }
}
