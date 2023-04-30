package appserver.server;

import appserver.comm.ConnectivityInfo;
import java.util.Hashtable;

/**
 * The class [SatelliteManager] manages the connectivity information of all satellite servers. 
 * 
 * It provides methods to register a new satellite and to get the connectivity info for a given satellite name.
 * 
 * @author Srinivasa 
 */
public class SatelliteManager {

    // (the one) hash table that contains the connectivity information of all satellite servers
    static private Hashtable<String, ConnectivityInfo> satellites = null;

    /**
     * Constructor that initializes the hash table for the connectivity info of the satellites.
     */
    public SatelliteManager() {
        satellites = new Hashtable();
    }
    
    /**
     * Registers a new satellite by adding its connectivity info to the hash table.
     *
     * @param satelliteInfo The connectivity info of the satellite to be registered.
     */
    public void registerSatellite(ConnectivityInfo satelliteInfo) {
        System.out.println("[SatelliteManager.registerSatellite] " + satelliteInfo.getName() + " is added to the registry");
        satellites.put(satelliteInfo.getName(), satelliteInfo);
    }
    
    /**
     * Returns the connectivity info for the satellite with the given name.
     *
     * @param satelliteName The name of the satellite whose connectivity info is to be returned.
     * @return The connectivity info of the specified satellite.
     */
    public ConnectivityInfo getSatelliteForName(String satelliteName) {
        return satellites.get(satelliteName);
    }
}
