package com.cstkit.demo.nest;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.opendof.core.ReconnectingStateListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAddress;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFDomain;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFProviderException;
import org.opendof.core.oal.DOFRequest;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFUtil;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFBoolean;
import org.opendof.core.oal.value.DOFString;
import org.opendof.core.oal.value.DOFUInt8;
import org.opendof.core.transport.inet.InetTransport;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

public class Provider 
{
    private final static int TIMEOUT = 20000;

    DOFObject myObject;

    // Nest property values. Cached locally to easily get information to requestor(s)
    int ambientTempF;
    int targetTempF;
    int targetHighF;
    int targetLowF;
    int humidity;
    String softwareVersion;
    String hvacMode;
    String tempScale;
    String name;
    String deviceID;
    int awayHigh;
    int awayLow;
    boolean canHeat;
    boolean canCool;
    boolean hasLeaf;

    // Firebase objects. Necessary for properties that can be adjusted
    Firebase fbAmbientTempF;
    Firebase fbTargetTempF;
    Firebase fbHvacMode;
    Firebase fbTargetHighF;
    Firebase fbTargetLowF;

    public static void main(String[] args) throws FileNotFoundException{
        // Set up yaml config
        Yaml yaml = new Yaml(new Constructor(ProviderYAMLConfig.class));
        ProviderYAMLConfig yamlConfig = (ProviderYAMLConfig)yaml.load(new FileInputStream(args[0]));

        // Get values from config file
        final String addr = yamlConfig.addr;
        final int port = yamlConfig.port;
        final DOFObjectID.Domain domainOID = DOFObjectID.Domain.create(yamlConfig.domain_id);
        final DOFObjectID.Authentication authID = DOFObjectID.Authentication.create(yamlConfig.auth_id);
        final byte[] key = DOFUtil.hexStringToBytes(yamlConfig.key);
        final String providerID = yamlConfig.provider_id;
        final String deviceID = yamlConfig.device_id;
        final String token = yamlConfig.access_token;

        // Gateway address, domain, and authentication credentials
        DOFObjectID.Domain domain = DOFObjectID.Domain.create(domainOID);
        DOFCredentials creds = DOFCredentials.Key.create(domain, authID, key);
        DOFAddress gatewayAddr = InetTransport.createAddress(addr, port);

        // Create DOF
        DOF.Config myDOFConfig = new DOF.Config.Builder().build();
        DOF provDof = new DOF(myDOFConfig);

        // Start provider (the protocol bridge client)
        startProvider(gatewayAddr, providerID, deviceID, token, creds, provDof);
    }

    public Provider(DOFSystem sys, String objID, String deviceID, String token){
        DOFSystem mySystem = sys;
        DOFObjectID myOID = DOFObjectID.create(objID);

        myObject = mySystem.createObject(myOID);
        myObject.beginProvide(NestInterface.DEF, DOF.TIMEOUT_NEVER, new ProviderListener(), null);

        try{
            firebaseConnect(deviceID, token);
        }
        catch(Exception e){
            System.err.println("Error connecting with Firebase " + e.getMessage());
        }
    }

    /*
     * Method used to establish a communication link between our provider and the NEST thermostat.
     * Data is stored and retreived as standard JSON.
     * The thermostat is uniquely identified by an ID. 
     * This provider is authorized to communicate with the Nest thermostat using a unique token String, 
     * which is authenticated through Firebase
     */
    public void firebaseConnect(String thermostat_id, String token) throws InterruptedException{
        final Object lock = new Object();

        // cycle states to ensure a reliable connection
        Firebase.goOffline();
        Firebase.goOnline();

        // Get Firebase objects for communicating
        Firebase f = new Firebase("https://developer-api.nest.com");

        // assign Firebase children (thermostat properties)
        fbAmbientTempF = f.child("/devices/thermostats/" + thermostat_id + "/ambient_temperature_f");
        fbTargetTempF = f.child("/devices/thermostats/" + thermostat_id + "/target_temperature_f");
        fbTargetHighF = f.child("/devices/thermostats/" + thermostat_id + "/target_temperature_high_f");
        fbTargetLowF = f.child("/devices/thermostats/" + thermostat_id + "/target_temperature_low_f");
        fbHvacMode = f.child("/devices/thermostats/" + thermostat_id + "/hvac_mode");
        // Get the thermostat device
        Firebase therm = f.child("/devices");

        // Authentication with Firebase using the provided token.
        // Uses an interface to listen for the result of the authentication attempt
        f.authWithCustomToken(token, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticationError(FirebaseError arg0) {
                System.err.println("Authentication Error: " + arg0.getMessage() + " / " + arg0.getCode());
                synchronized (lock){
                    lock.notify();
                }
            }

            @Override
            public void onAuthenticated(AuthData arg0) {
                System.out.println("NEST authentication successful.");
                synchronized(lock){
                    lock.notify();
                }
            }
        });
        System.out.println("Waiting for authentication");
        synchronized(lock){
            lock.wait();
        }


        // Add a value event listener to all properties that are expected to change (either from 
        // the Requestor or from the thermostat readings).
        therm.addValueEventListener(new ValueEventListener(){
            @Override
            public void onCancelled(FirebaseError arg0) {
                System.out.println("Cancelled: " + arg0.getMessage());
            }

            // Listen for any property's data to change. Upon any value changing, use 
            // the DataSnapshot, which contains all information, to get specific values
            @Override
            public void onDataChange(DataSnapshot arg0) {
                System.out.println("Snapshot: " + arg0.getValue());

                // Get a List of all values specific to this DataSnapshot. This reduces the amount of time
                //  the provider communicates through Firebase (too many requests can throttle either side of 
                //  the communication)
                Iterable<DataSnapshot> snapList = arg0.child("thermostats/" + thermostat_id).getChildren();				

                for(DataSnapshot ds : snapList){
                    switch(ds.getName()){
                    case "ambient_temperature_f":
                        setAmbientTempF(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "humidity":
                        setHumidity(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "hvac_mode":
                        setHvacMode(ds.getValue().toString());
                        break;
                    case "target_temperature_f":
                        setTargetTempF(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "target_temperature_high_f":
                        setTargetHighF(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "target_temperature_low_f":
                        setTargetLowF(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "name":
                        setName(ds.getValue().toString());
                        break;
                    case "away_temperature_high_f":
                        setAwayHigh(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "away_temperature_low_f":
                        setAwayLow(Integer.parseInt(ds.getValue().toString()));
                        break;
                    case "can_heat":
                        setCanHeat(Boolean.parseBoolean(ds.getValue().toString()));
                        break;
                    case "can_cool":
                        setCanCool(Boolean.parseBoolean(ds.getValue().toString()));
                        break;
                    case "has_leaf":
                        setHasLeaf(Boolean.parseBoolean(ds.getValue().toString()));
                        break;
                    }
                }
            }
        });

        // Add a value event listener to set initial values that should not change.
        // This method listens for an event only once.
        therm.addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onCancelled(FirebaseError arg0) {
                System.out.println("Cancelled: " + arg0.getMessage());
            }

            @Override
            public void onDataChange(DataSnapshot arg0) {

                Iterable<DataSnapshot> snapList = arg0.child("thermostats/" + thermostat_id).getChildren();

                for(DataSnapshot ds : snapList){
                    switch(ds.getName()){
                    case "temperature_scale":
                        setTempScale(ds.getValue().toString());
                        break;
                    case "device_id":
                        setDeviceID(ds.getValue().toString());
                        break;
                    case "software_version":
                        setVersion(ds.getValue().toString());
                    }
                }
            }
        });
    }

    // ** Interface property getters and setters **
    private void setAmbientTempF(int pos){
        ambientTempF = pos;
        myObject.changed(NestInterface.PROPERTY_AMBIENT_F);
    }

    public void setTempScale(String scale){ 
        tempScale = scale;
    }

    public void setDeviceID(String id){
        deviceID = id;
    }

    public void setVersion(String version){
        softwareVersion = version;
    }

    public void setName(String name){
        this.name = name;
        myObject.changed(NestInterface.PROPERTY_NAME);
    }

    public void setTargetTempF(int target){ 
        targetTempF = target;				
        myObject.changed(NestInterface.PROPERTY_TARGET_TEMP_F);
    }

    public void setTargetHighF(int target){
        targetHighF = target;
        myObject.changed(NestInterface.PROPERTY_TARGET_HIGH_F); 
    }

    public void setTargetLowF(int target){
        targetLowF = target;
        myObject.changed(NestInterface.PROPERTY_TARGET_LOW_F);
    }

    public void setHvacMode(String mode){
        // check that the mode is valid
        if(mode.compareTo("heat") == 0 || mode.compareTo("cool") == 0 || mode.compareTo("heat-cool") == 0 || mode.compareTo("off") == 0){
            hvacMode = mode;
            myObject.changed(NestInterface.PROPERTY_HVAC_MODE);
        }
        else
            System.err.println("Invalid hvac mode entered. Must be 'heat', 'cool', 'heat-cool', or 'off'");
    }

    public void setHumidity(int hum){
        humidity = hum;
        myObject.changed(NestInterface.PROPERTY_HUMIDITY);
    }

    public void setAwayHigh(int target){
        awayHigh = target;
        myObject.changed(NestInterface.PROPERTY_AWAY_HIGH_F);
    }

    public void setAwayLow(int target){
        awayLow = target;
        myObject.changed(NestInterface.PROPERTY_AWAY_LOW_F);
    }

    public void setCanHeat(boolean canHeat){
        this.canHeat = canHeat;
        myObject.changed(NestInterface.PROPERTY_CAN_HEAT);
    }

    public void setCanCool(boolean canCool){
        this.canCool = canCool;
        myObject.changed(NestInterface.PROPERTY_CAN_COOL);
    }

    public void setHasLeaf(boolean hasLeaf){
        this.hasLeaf = hasLeaf;
        myObject.changed(NestInterface.PROPERTY_HAS_LEAF);
    }


    // Default Provider
    public class ProviderListener extends DOFObject.DefaultProvider {
        @Override
        public void get(Provide operation, DOFRequest.Get request, Property property) {

            DOFUInt8 myDOFInt = null;
            DOFString myDOFString = null;
            DOFBoolean myDOFBoolean = null;

            if(property.equals(NestInterface.PROPERTY_AMBIENT_F)){
                myDOFInt = new DOFUInt8((short)ambientTempF);
                request.respond(myDOFInt);
            }else if(property.equals(NestInterface.PROPERTY_TARGET_TEMP_F)){
                myDOFInt = new DOFUInt8((short)targetTempF);
                request.respond(myDOFInt);
            }else if(property.equals(NestInterface.PROPERTY_TARGET_HIGH_F)){
                myDOFInt = new DOFUInt8((short)targetHighF);
                request.respond(myDOFInt);
            }else if(property.equals(NestInterface.PROPERTY_TARGET_LOW_F)){
                myDOFInt = new DOFUInt8((short)targetLowF);
                request.respond(myDOFInt);
            }else if(property.equals(NestInterface.PROPERTY_HUMIDITY)){
                myDOFInt = new DOFUInt8((short)humidity);
                request.respond(myDOFInt);
            }else if(property.equals(NestInterface.PROPERTY_SOFTWARE_VERSION)){
                myDOFString = new DOFString(softwareVersion);
                request.respond(myDOFString);
            }
            else if(property.equals(NestInterface.PROPERTY_HVAC_MODE)){
                myDOFString = new DOFString(hvacMode);
                request.respond(myDOFString);
            }else if(property.equals(NestInterface.PROPERTY_DEVICE_ID)){
                myDOFString = new DOFString(deviceID);
                request.respond(myDOFString);
            }
            else if(property.equals(NestInterface.PROPERTY_NAME)){
                myDOFString = new DOFString(name);
                request.respond(myDOFString);
            }
            else if(property.equals(NestInterface.PROPERTY_TEMP_SCALE)){
                myDOFString = new DOFString(tempScale);
                request.respond(myDOFString);
            }
            else if(property.equals(NestInterface.PROPERTY_AWAY_HIGH_F)){
                myDOFInt = new DOFUInt8((short)awayHigh);
                request.respond(myDOFInt);
            }
            else if(property.equals(NestInterface.PROPERTY_AWAY_LOW_F)){
                myDOFInt = new DOFUInt8((short)awayLow);
                request.respond(myDOFInt);
            }
            else if(property.equals(NestInterface.PROPERTY_CAN_HEAT)){
                myDOFBoolean = new DOFBoolean(canHeat);
                request.respond(myDOFBoolean);
            }
            else if(property.equals(NestInterface.PROPERTY_CAN_COOL)){
                myDOFBoolean = new DOFBoolean(canCool);
                request.respond(myDOFBoolean);
            }
            else if(property.equals(NestInterface.PROPERTY_HAS_LEAF)){
                myDOFBoolean = new DOFBoolean(hasLeaf);
                request.respond(myDOFBoolean);
            }
            else{
//                DOFErrorException ex = DOFErrorException.create(DOFErrorException.NOT_SUPPORTED, "This provider does not support the requested action.", null);
            }
        }

        @Override
        public void set(Provide operation, DOFRequest.Set request, Property property, DOFValue value) {

            // Set the target temperature
            if(property.equals(NestInterface.PROPERTY_TARGET_TEMP_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value;

                // Handle this exception by throwing a providerException (for demonstration purposes)
                if(hvacMode.equals("heat-cool")){
                    DOFProviderException providerEx = new DOFProviderException(NestInterface.EXCEPTION_HEATCOOL_ON);
                    request.respond(providerEx);
                    return;
                }
                // Handle this exception by automatically setting the Hvac Mode to the appropriate mode (for usability)
                else{
                    if(myDOFInt.get() > ambientTempF)
                        fbHvacMode.setValue("heat");
                    else
                        fbHvacMode.setValue("cool");

                    fbTargetTempF.setValue(myDOFInt.get());
                }
            }
            // Set the target high temperature
            else if(property.equals(NestInterface.PROPERTY_TARGET_HIGH_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value;

                // If the suggested target high is lower than the target low, throw an exception
                if(myDOFInt.get() < targetLowF){
                    DOFProviderException providerEx = new DOFProviderException(NestInterface.EXCEPTION_INVALID_HIGHLOW);
                    request.respond(providerEx);
                    return;
                }
                // If the Hvac mode is off, automatically set it to heat-cool
                else if(hvacMode.equals("off")){
                    fbHvacMode.setValue("heat-cool");
                    fbTargetHighF.setValue(myDOFInt.get());
                }
                // If the Hvac mode is not heat-cool, throw an exception.
                else if(!hvacMode.equals("heat-cool")){
                    DOFProviderException providerEx = new DOFProviderException(NestInterface.EXCEPTION_HEATCOOL_OFF);
                    request.respond(providerEx);
                    return;
                }
                else
                    fbTargetHighF.setValue(myDOFInt.get());
            }
            // Set the target low temperature
            else if(property.equals(NestInterface.PROPERTY_TARGET_LOW_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value;

                if(myDOFInt.get() > targetHighF){
                    DOFProviderException providerEx = new DOFProviderException(NestInterface.EXCEPTION_INVALID_HIGHLOW);
                    request.respond(providerEx);
                    return;
                }
                else if(hvacMode.equals("off")){
                    fbHvacMode.setValue("heat-cool");
                    fbTargetLowF.setValue(myDOFInt.get());
                }
                else if(!hvacMode.equals("heat-cool")){
                    DOFProviderException providerEx = new DOFProviderException(NestInterface.EXCEPTION_HEATCOOL_OFF);
                    request.respond(providerEx);
                    return;
                }
                else
                    fbTargetLowF.setValue(myDOFInt.get());
            }
            // Set the Hvac mode
            else if(property.equals(NestInterface.PROPERTY_HVAC_MODE)){
                DOFString myDOFString = (DOFString)value;
                fbHvacMode.setValue(myDOFString.get());
            }

            request.respond();
        }

        @Override
        public void subscribe(Provide operation, DOFRequest.Subscribe request, Property property, int minPeriod){
            request.respond();
        }
    }

    private static void startProvider(DOFAddress dofAddr, String providerID, String deviceID, String token, DOFCredentials creds, DOF provDof){
        connectRouter(provDof, dofAddr, creds);

        try{
            DOFSystem.Config sysConfig = new DOFSystem.Config.Builder()
                    .setCredentials(creds)
                    .setName("provider")
                    .build();
            DOFSystem system = provDof.createSystem(sysConfig, TIMEOUT);

            new Provider(system, providerID, deviceID, token);
        }
        catch(DOFException e){
            System.err.println("Unable to create system: " + e.getMessage());
        }
        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static DOFDomain.Config connectRouter(DOF dof, DOFAddress myAddress, DOFCredentials creds){
        DOFDomain.Config domainConfig = new DOFDomain.Config.Builder(creds)
                .setMaxSilence(20 * 60 * 1000) // 20 minute max silence
                .build();

        DOFConnection.Config myConnConfig = new DOFConnection.Config.Builder(DOFConnection.Type.STREAM, myAddress)
                .setCredentials(creds)
                .addDomain(domainConfig)
                .setSecurityDesire(SecurityDesire.SECURE)
                .build();

        DOFConnection myConn = dof.createConnection(myConnConfig);

        try{
            myConn.connect(TIMEOUT);
        }
        catch(DOFException ex){
            System.err.println("Unable to connect the connection. " + ex.getMessage());
        }
        catch(Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        myConn.addStateListener(new ReconnectingStateListener());

        return domainConfig;
    }

    private static class ProviderYAMLConfig{
        public int port = 3567;
        public String addr = "0.0.0.0";
        public String domain_id = null;
        public String auth_id = null;
        public String key = null;
        public String provider_id = null;
        public String device_id = null;
        public String access_token = null;
    }
}