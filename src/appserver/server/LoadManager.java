package appserver.server;

import java.util.ArrayList;

/**
 * The class [LoadManager] manages the list of available satellites and
 * implements a round-robin technique to distribute the load among them.
 * 
 * @author sampath
 */
public class LoadManager {

    // A list of available satellites
    static ArrayList satellites = null;
    
    // The index of the last selected satellite
    static int lastSatelliteIndex = -1;
    
    /**
     * Constructor that initializes the list of satellites.
     */
    public LoadManager() {
        satellites = new ArrayList<String>();
    }
    
    /**
     * Adds a new satellite to the list.
     *
     * @param satelliteName The name of the satellite to be added.
     */
    public void satelliteAdded(String satelliteName) {
        // add satellite
        satellites.add(satelliteName);
    }

    /**
     * Returns the next available satellite using a round-robin technique.
     *
     * @return The name of the next satellite.
     * @throws Exception If there are no satellites available.
     */
    public String nextSatellite() throws Exception {
        int numberSatellites;

        synchronized (satellites) {
            numberSatellites = satellites.size();
            if (numberSatellites == 0) {
                throw new Exception("No satellites available.");
            }
            lastSatelliteIndex = (lastSatelliteIndex + 1) % numberSatellites;
        }

        return (String) satellites.get(lastSatelliteIndex);
    }
}
