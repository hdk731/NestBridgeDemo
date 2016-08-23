package com.cstkit.demo.nest;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;

import org.opendof.core.ReconnectingStateListener;
import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOF.SecurityDesire;
import org.opendof.core.oal.DOFAddress;
import org.opendof.core.oal.DOFConnection;
import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFDomain;
import org.opendof.core.oal.DOFDomain.State;
import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterestLevel;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Query;
import org.opendof.core.oal.DOFProviderInfo;
import org.opendof.core.oal.DOFQuery;
import org.opendof.core.oal.DOFSubscription;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFUtil;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFBoolean;
import org.opendof.core.oal.value.DOFString;
import org.opendof.core.oal.value.DOFUInt8;
import org.opendof.core.transport.inet.InetTransport;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class Requestor {
    DOFSystem mySystem = null;
    DOFObject providerObject = null;
    DOFObjectID providerObjectID; 
    RequestorUI parent = null;

    DOFSubscription subscribeAmbientTemp = null;
    DOFSubscription subscribeTargetTemp = null;
    DOFSubscription subscribeTargetHigh = null;
    DOFSubscription subscribeTargetLow = null;
    DOFSubscription subscribeHumidity = null;
    DOFSubscription subscribeHvacMode = null;
    DOFSubscription subscribeAwayHigh = null;
    DOFSubscription subscribeAwayLow = null;
    DOFSubscription subscribeCanCool = null;
    DOFSubscription subscribeCanHeat = null;
    DOFSubscription subscribeHasLeaf = null;

    DOFOperation.Get activeGetAmbientOp = null;
    DOFOperation.Get activeGetTargetOp = null;
    DOFOperation.Get activeGetTargetHighOp = null;
    DOFOperation.Get activeGetTargetLowOp = null;
    DOFOperation.Get activeGetHumidityOp = null;
    DOFOperation.Get activeGetHvacOp = null;
    DOFOperation.Get activeGetNameOp = null;
    DOFOperation.Get activeGetDeviceIDOp = null;
    DOFOperation.Get activeGetTempScaleOp = null;
    DOFOperation.Get activeGetVersionOp = null;

    DOFOperation.Set activeSetTargetOp = null;
    DOFOperation.Set activeSetTargetHighOp = null;
    DOFOperation.Set activeSetTargetLowOp = null;
    DOFOperation.Set activeSetHvacOp = null;

    DOFQuery query = null;
    static int TIMEOUT = 1000;

    SubscribeListener subListener = new SubscribeListener();

    public static void main(String[] args) throws IOException{
        // Set up yaml config
        Yaml yaml = new Yaml(new Constructor(RequestorYAMLConfig.class));
        RequestorYAMLConfig yamlConfig = (RequestorYAMLConfig)yaml.load(new FileInputStream(args[0]));

        // Get values from config file
        final String addr = yamlConfig.addr;
        final int port = yamlConfig.port;
        final DOFObjectID.Domain domainOID = DOFObjectID.Domain.create(yamlConfig.domain_id);
        final DOFObjectID.Authentication authID = DOFObjectID.Authentication.create(yamlConfig.auth_id);
        final byte[] key = DOFUtil.hexStringToBytes(yamlConfig.key);
        final String providerID = yamlConfig.provider_id;

        // Gateway address, domain, and authentication credentials
        DOFObjectID.Domain domain = DOFObjectID.Domain.create(domainOID);
        DOFCredentials creds = DOFCredentials.Key.create(domain, authID, key);
        DOFAddress gatewayAddr = InetTransport.createAddress(addr, port);

        // Create DOF
        DOF.Config myDOFConfig = new DOF.Config.Builder().build();
        DOF reqDof = new DOF(myDOFConfig);

        // Start requestor (the user's client)
        startRequestor(reqDof, gatewayAddr, creds, providerID);
    }

    public Requestor(DOFSystem system, RequestorUI par, String providerOID){
        mySystem = system;
        parent = par;

        // Create the provider      
        providerObjectID = DOFObjectID.create(providerOID);
        providerObject = mySystem.createObject(providerObjectID);

        mySystem.beginInterest(providerObjectID, NestInterface.IID, DOFInterestLevel.WATCH, DOF.TIMEOUT_NEVER, null, null);

        // start query and interest operations
        query = new DOFQuery.Builder()
                .addFilter(providerObjectID, NestInterface.IID)
                .build();
        mySystem.beginQuery(query, DOF.TIMEOUT_NEVER, new QueryListener(), null);
    }

    private static void startRequestor(DOF reqDof, DOFAddress myAddress, DOFCredentials requestorCreds, String providerID) throws IOException{
        RequestorUI reqParent = new RequestorUI(providerID);

        DOFDomain.Config domainConfig = connectRouter(reqDof, myAddress, requestorCreds);

        SystemAvailableCallback callback = new SystemAvailableCallback()
        {
            @Override
            public void systemAvailable(String name, DOFSystem system, Exception ex)
            {
                if(system != null){
                    reqParent.setRequestor(new Requestor(system, reqParent, providerID));
                    return;
                }
                if(ex != null){
                    WindowEvent wev = new WindowEvent(RequestorUI.getWindows()[0], WindowEvent.WINDOW_CLOSING);
                    Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
                }
            }

        };
        DOFSystem.Config systemConfig = new DOFSystem.Config.Builder()
                .setCredentials(requestorCreds)
                .setName("requestor")
                .build();

        DOFDomain domain = reqDof.createDomain(domainConfig);

        domain.addStateListener(new CustomDomainListener(reqDof, systemConfig, callback));
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

    public void destroy(){
        if(subscribeAmbientTemp != null)
            subscribeAmbientTemp.destroy();
        if(subscribeTargetHigh != null)
            subscribeTargetHigh.destroy();
        if(subscribeTargetLow != null)
            subscribeTargetLow.destroy();
        if(subscribeHumidity != null)
            subscribeHumidity.destroy();
        if(subscribeHvacMode != null)
            subscribeHvacMode.destroy();
        if(subscribeAwayHigh != null)
            subscribeAwayHigh.destroy();
        if(subscribeAwayLow != null)
            subscribeAwayLow.destroy();
        if(subscribeCanCool != null)
            subscribeCanCool.destroy();
        if(subscribeCanHeat != null)
            subscribeCanHeat.destroy();
        if(subscribeHasLeaf != null)
            subscribeHasLeaf.destroy();

        if(providerObject != null)
            providerObject.destroy();
    }


    // Gets all provided information at once.
    public void sendBeginGetAllRequest()
    {
        GetListener getListener = new GetListener();
        activeGetAmbientOp = providerObject.beginGet(NestInterface.PROPERTY_AMBIENT_F, 1000, getListener, null);
        activeGetTargetOp = providerObject.beginGet(NestInterface.PROPERTY_TARGET_TEMP_F, 1000, getListener, null);
        activeGetTargetHighOp = providerObject.beginGet(NestInterface.PROPERTY_TARGET_HIGH_F, 1000, getListener, null);
        activeGetTargetLowOp = providerObject.beginGet(NestInterface.PROPERTY_TARGET_LOW_F, 1000, getListener, null);
        activeGetHumidityOp = providerObject.beginGet(NestInterface.PROPERTY_HUMIDITY, 1000, getListener, null);
        activeGetHvacOp = providerObject.beginGet(NestInterface.PROPERTY_HVAC_MODE, 1000, getListener, null);
        activeGetDeviceIDOp = providerObject.beginGet(NestInterface.PROPERTY_DEVICE_ID, 1000, getListener, null);
        activeGetNameOp = providerObject.beginGet(NestInterface.PROPERTY_NAME, 1000, getListener, null);
        activeGetTempScaleOp = providerObject.beginGet(NestInterface.PROPERTY_TEMP_SCALE, 1000, getListener, null);
        activeGetVersionOp = providerObject.beginGet(NestInterface.PROPERTY_SOFTWARE_VERSION, 1000, getListener, null);
    }

    // Asynch SET requests
    public void sendBeginSetTargetRequest(int target){
        DOFInterface.Property property = NestInterface.PROPERTY_TARGET_TEMP_F;
        DOFUInt8 setTargetValue = new DOFUInt8((short)target);
        activeSetTargetOp = providerObject.beginSet(property, setTargetValue, TIMEOUT, new SetListener(), null);
    }

    public void sendBeginSetTargetHighRequest(int target){
        DOFInterface.Property property = NestInterface.PROPERTY_TARGET_HIGH_F;
        DOFUInt8 setTargetHighValue = new DOFUInt8((short)target);
        activeSetTargetHighOp = providerObject.beginSet(property, setTargetHighValue, TIMEOUT, new SetListener(), null);
    }

    public void sendBeginSetTargetLowRequest(int target){
        DOFInterface.Property property = NestInterface.PROPERTY_TARGET_LOW_F;
        DOFUInt8 setTargetLowValue = new DOFUInt8((short)target);
        activeSetTargetLowOp = providerObject.beginSet(property, setTargetLowValue, TIMEOUT, new SetListener(), null);
    }

    public void sendBeginSetHvacRequest(String mode){
        DOFInterface.Property property = NestInterface.PROPERTY_HVAC_MODE;
        DOFString setHvacModeValue = new DOFString(mode);
        activeSetHvacOp = providerObject.beginSet(property, setHvacModeValue, TIMEOUT, new SetListener(), null);
    }

    // Subscription requests
    // Subscribe to just one property, the Ambient Temperature
    public void sendBeginSubscribeAmbientRequest(){
        try{
            subscribeAmbientTemp = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_AMBIENT_F, 200, subListener);
        }
        catch(DOFException e){
            System.err.println("Subscription to ambient temp failed: " + e.getMessage());
        }
    }

    // Subscribe to all properties than can change in real-time
    public void sendBeginSubscribeAllRequest(){
        try{
            subscribeAmbientTemp = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_AMBIENT_F, 200, subListener);
            subscribeTargetTemp = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_TARGET_TEMP_F, 200, subListener);
            subscribeTargetHigh = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_TARGET_HIGH_F, 200, subListener);
            subscribeTargetLow = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_TARGET_LOW_F, 200, subListener);
            subscribeHumidity = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_HUMIDITY, 200, subListener);
            subscribeHvacMode = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_HVAC_MODE, 200, subListener);
            subscribeAwayHigh = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_AWAY_HIGH_F, 200, subListener);
            subscribeAwayLow = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_AWAY_LOW_F, 200, subListener);
            subscribeCanCool = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_CAN_COOL, 200, subListener);
            subscribeCanHeat = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_CAN_HEAT, 200, subListener);
            subscribeHasLeaf = mySystem.createSubscription(providerObjectID, NestInterface.PROPERTY_HAS_LEAF, 200, subListener);
        }catch(DOFException e){
            System.err.println("Subscription to all failed: " + e.getMessage());
        }
    }


    // Provide asynchronous set operations
    private class SetListener implements DOFObject.SetOperationListener{
        @Override
        public void setResult(DOFOperation.Set op, DOFProviderInfo providerInfo, DOFException ex) {
            if(ex == null)
            {
                parent.displaySetResults("Successful");
            }
            else
            {		
                switch(ex.getMessage().toString()){
                case "EXCEPTION:26:":
                    parent.displaySetResults("Error: Target High must be greater than Target Low");
                    break;
                case "EXCEPTION:27:":
                    parent.displaySetResults("Error: Must be in HEAT-COOL mode to set Target High or Low");
                    break;
                case "EXCEPTION:28:":
                    parent.displaySetResults("Error: Must not be in HEAT-COOL mode to set Target Temperature");
                    break;
                }
            }
        }
        @Override
        public void complete(DOFOperation op, DOFException ex) {
            if(op.equals(activeSetHvacOp)){
                activeSetHvacOp.cancel();
                activeSetHvacOp = null;
            }
            else if(op.equals(activeSetTargetOp)){
                activeSetTargetOp.cancel();
                activeSetTargetOp = null;
            }
            else if(op.equals(activeSetTargetHighOp)){
                activeSetTargetHighOp.cancel();
                activeSetTargetHighOp = null;
            }
            else if(op.equals(activeSetTargetLowOp)){
                activeSetTargetLowOp.cancel();
                activeSetTargetLowOp = null;
            }
        }
    }

    // Provide asynchronous get operations
    private class GetListener implements DOFObject.GetOperationListener
    {
        @Override
        public void getResult(DOFOperation.Get op, DOFProviderInfo providerInfo, DOFValue result, DOFException ex)
        {
            if(ex == null)
            {
                DOFObjectID providerID = providerInfo.getProviderID();
                String providerIDString = providerID.toStandardString();

                if(op.getProperty()==NestInterface.PROPERTY_AMBIENT_F){
                    DOFUInt8 myDOFInt = (DOFUInt8)result;
                    int usableInt = myDOFInt.get();
                    parent.displayGetAmbientResults(providerIDString, usableInt);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_TARGET_TEMP_F){
                    DOFUInt8 myDOFInt = (DOFUInt8)result;
                    int usableInt = myDOFInt.get();
                    parent.displayGetTargetResults(providerIDString, usableInt);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_TARGET_HIGH_F){
                    DOFUInt8 myDOFInt = (DOFUInt8)result;
                    int usableInt = myDOFInt.get();
                    parent.displayGetTargetHighResults(providerIDString, usableInt);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_TARGET_LOW_F){
                    DOFUInt8 myDOFInt = (DOFUInt8)result;
                    int usableInt = myDOFInt.get();
                    parent.displayGetTargetLowResults(providerIDString, usableInt);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_SOFTWARE_VERSION){
                    DOFString dofString = (DOFString)result;
                    String usableString = dofString.get();
                    parent.displayGetVersionResults(providerIDString, usableString);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_HVAC_MODE){
                    DOFString dofString = (DOFString)result;
                    String usableString = dofString.get();
                    parent.displayGetHvacResults(providerIDString, usableString);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_HUMIDITY){
                    DOFUInt8 myDOFInt = (DOFUInt8)result;
                    int usableInt = myDOFInt.get();
                    parent.displayGetHumidityResults(providerIDString, usableInt);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_DEVICE_ID){
                    DOFString myDOFString = (DOFString)result;
                    String usableString = myDOFString.get();
                    parent.displayGetDeviceIDResults(providerIDString, usableString);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_NAME){
                    DOFString myDOFString = (DOFString)result;
                    String usableString = myDOFString.get();
                    parent.displayGetNameResults(providerIDString, usableString);
                }
                else if(op.getProperty()==NestInterface.PROPERTY_TEMP_SCALE){
                    DOFString myDOFString = (DOFString)result;
                    String usableString = myDOFString.get();
                    parent.displayGetTempScaleResults(providerIDString, usableString);
                }
                else
                    System.out.println(op);
            }
            else
            {
                System.err.println("Error: " + ex.getMessage());
            }
        }

        @Override
        public void complete(DOFOperation op, DOFException ex) {
            if(op.equals(activeGetAmbientOp)){
                activeGetAmbientOp.cancel();
                activeGetAmbientOp = null;
            }
            else if(op.equals(activeGetTargetOp)){
                activeGetTargetOp.cancel();
                activeGetTargetOp = null;
            }
            else if (op.equals(activeGetHvacOp)){
                activeGetHvacOp.cancel();
                activeGetHvacOp = null;
            }
            else if(op.equals(activeGetDeviceIDOp)){
                activeGetDeviceIDOp.cancel();
                activeGetDeviceIDOp = null;
            }
            else if(op.equals(activeGetHumidityOp)){
                activeGetHumidityOp.cancel();
                activeGetHumidityOp = null;
            }
            else if(op.equals(activeGetNameOp)){
                activeGetNameOp.cancel();
                activeGetNameOp = null;
            }
            else if(op.equals(activeGetTargetHighOp)){
                activeGetTargetHighOp.cancel();
                activeGetTargetHighOp = null;
            }
            else if(op.equals(activeGetTargetLowOp)){
                activeGetTargetLowOp.cancel();
                activeGetTargetLowOp = null;
            }
            else if(op.equals(activeGetTempScaleOp)){
                activeGetTempScaleOp.cancel();
                activeGetTempScaleOp = null;
            }
        }
    }

    // Query Listener
    private class QueryListener implements DOFSystem.QueryOperationListener
    {

        @Override
        public void interfaceAdded(Query operation, DOFObjectID oid, DOFInterfaceID iid){
            if(iid.equals(NestInterface.IID)){
                if(providerObject == null)
                {
                    providerObject = mySystem.createObject(oid);
                }
            }
        }

        @Override
        public void interfaceRemoved(Query operation, DOFObjectID oid, DOFInterfaceID iid){}

        @Override
        public void providerRemoved(Query operation, DOFObjectID oid){
            if(providerObject != null){
                providerObject.destroy();
            }
        }

        @Override
        public void complete(DOFOperation operation, DOFException exception){}
    }
    // Subscribe Listener
    private class SubscribeListener implements DOFSubscription.Listener{

        @Override
        public void propertyChanged(DOFSubscription subscription, DOFObjectID providerID, DOFValue value)
        {
            String providerIDString = providerID.toStandardString();

            if(subscription.getProperty().equals(NestInterface.PROPERTY_AMBIENT_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value; 
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeAmbientResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_TARGET_TEMP_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value; 
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeTargetResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_TARGET_HIGH_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value; 
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeTargetHighResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_TARGET_LOW_F)){
                DOFUInt8 myDOFInt = (DOFUInt8)value; 
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeTargetLowResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_HVAC_MODE)){
                DOFString myDOFString = (DOFString)value; 
                String unwrappedResult = myDOFString.get();
                parent.displaySubscribeHvacResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_HUMIDITY)){
                DOFUInt8 myDOFInt = (DOFUInt8)value; 
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeHumidityResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_AWAY_HIGH_F)){
                DOFUInt8 myDOFInt= (DOFUInt8)value;
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeAwayHigh(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_AWAY_LOW_F)){
                DOFUInt8 myDOFInt= (DOFUInt8)value;
                int unwrappedResult = myDOFInt.get();
                parent.displaySubscribeAwayLow(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_CAN_HEAT)){
                DOFBoolean myDOFBool= (DOFBoolean)value;
                boolean unwrappedResult = myDOFBool.get();
                parent.displaySubscribeCanHeatResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_CAN_COOL)){
                DOFBoolean myDOFBool= (DOFBoolean)value;
                boolean unwrappedResult = myDOFBool.get();
                parent.displaySubscribeCanCoolResults(providerIDString, unwrappedResult);
            }
            if(subscription.getProperty().equals(NestInterface.PROPERTY_HAS_LEAF)){
                DOFBoolean myDOFBool= (DOFBoolean)value;
                boolean unwrappedResult = myDOFBool.get();
                parent.displaySubscribeHasLeafResults(providerIDString, unwrappedResult);
            }

        }

        @Override
        public void stateChanged(DOFSubscription subscription, DOFSubscription.State state)
        {
        }

        @Override
        public void removed(DOFSubscription subscription, DOFException exception)
        {
        }
    }

    private static class CustomDomainListener implements DOFDomain.StateListener {
        public static final int SYSTEM_CREATION_TIMEOUT = 30000;
        private final DOFSystem.Config systemConfig;
        private final SystemAvailableCallback systemAvailableCallback;
        private final DOF dof;

        public CustomDomainListener(DOF dof, DOFSystem.Config systemConfig, SystemAvailableCallback callback) {
            this.systemConfig = systemConfig;
            this.systemAvailableCallback = callback;
            this.dof = dof;
        }

        @Override
        public void stateChanged(DOFDomain domain, State state) {
            String systemName = systemConfig.getName();
            //            }
            if(state.isConnected()){
                try{
                    DOFSystem system = dof.createSystem(systemConfig, SYSTEM_CREATION_TIMEOUT);
                    systemAvailableCallback.systemAvailable(systemName, system, null);
                } catch (DOFException e) {
                    System.err.println("Unable to create system. Getting error " + e.getMessage());
                    systemAvailableCallback.systemAvailable(systemName, null, e);
                }
            }
        }

        @Override
        public void removed(DOFDomain domain, DOFException exception) {
            // do nothing
        }
    }

    public interface SystemAvailableCallback{
        public void systemAvailable(String name, DOFSystem system, Exception exception);
    }

    private static class RequestorYAMLConfig{
        public int port = 3567;
        public String addr = "0.0.0.0";
        public String domain_id = null;
        public String auth_id = null;
        public String key = null;
        public String provider_id = null;
    }
}







