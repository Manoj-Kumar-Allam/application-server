/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appserver.client;

import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import appserver.job.Job;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import utils.PropertyHandler;

/**
 * The class [FibonacciClient] requests the Fibonacci number for a given integer 
 * from the Application Server.
 * 
 * @author manoj
 */
public class FibonacciClient extends Thread {
    
    String host = null;
    int port;

    Properties properties;
    
    Integer number;

    public FibonacciClient(String serverPropertiesFile, Integer number) {
        try {
            properties = new PropertyHandler(serverPropertiesFile);
            host = properties.getProperty("HOST");
            port = Integer.parseInt(properties.getProperty("PORT"));
            
            this.number = number;
        } catch (IOException | NumberFormatException ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try { 
            // connect to application server
            Socket server = new Socket(host, port);
            
            // hard-coded string of class, aka tool name ... plus one argument
            String classString = "appserver.job.impl.Fibonacci";
            
            
            // create job and job request message
            Job job = new Job(classString, number);
            Message message = new Message(JOB_REQUEST, job);
            
            // sending job out to the application server in a message
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);
            
            // reading result back in from application server
            // for simplicity, the result is not encapsulated in a message
            ObjectInputStream readFromNet = new ObjectInputStream(server.getInputStream());
            Integer result = (Integer) readFromNet.readObject();
            System.out.println("Fibonacci Number of " + number +  " : " + result);
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("[FibonacciClient.run] Error occurred");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) { 
        
        // create 46 requests to client server
        for(int i=46; i>0; i--) {
            new FibonacciClient("../../config/Server.properties", i).start();
        }
    }
}
