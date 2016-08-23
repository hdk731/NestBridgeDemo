package com.cstkit.demo.nest;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.value.DOFBoolean;
import org.opendof.core.oal.value.DOFDateTime;
import org.opendof.core.oal.value.DOFFloat32;
import org.opendof.core.oal.value.DOFString;
import org.opendof.core.oal.value.DOFUInt8;

public class NestInterface {

	public static final DOFType FloatVal = DOFFloat32.TYPE;
	public static final DOFType UInt8Val = DOFUInt8.TYPE;
	public static final DOFType BoolVal = DOFBoolean.TYPE;
	public static final DOFType StringVal = new DOFString.Type(DOFString.US_ASCII, 40);
	public static final DOFType DateTimeVal = DOFDateTime.TYPE;

	public static final DOFInterface DEF;

	// The DOFInterface is encoded with class 63 and the word NEST in hexadecimal
	public static final DOFInterfaceID IID = DOFInterfaceID.create("[63:{4E455354}]");

	// *** ID Section *** 
	// Property IDs
	public static final int PROPERTY_DEVICE_ID_ID = 1;
	public static final int PROPERTY_NAME_ID = 2;
	public static final int PROPERTY_TEMP_SCALE_ID = 3;
	public static final int PROPERTY_TARGET_TEMP_F_ID = 4;
	public static final int PROPERTY_TARGET_HIGH_F_ID = 6;
	public static final int PROPERTY_TARGET_LOW_F_ID = 8;
	public static final int PROPERTY_AWAY_HIGH_F_ID = 10;
	public static final int PROPERTY_AWAY_LOW_F_ID = 12;
	public static final int PROPERTY_SOFTWARE_VERSION_ID = 13;
	public static final int PROPERTY_HVAC_MODE_ID = 14;
	public static final int PROPERTY_AMBIENT_F_ID = 15;
	public static final int PROPERTY_HAS_LEAF_ID = 16;
	public static final int PROPERTY_HUMIDITY_ID = 17;
	public static final int PROPERTY_CAN_COOL_ID = 18;
	public static final int PROPERTY_CAN_HEAT_ID = 19;
	public static final int PROPERTY_HAS_FAN_ID  = 20;
	public static final int PROPERTY_FAN_TIMER_ACTIVE_ID = 21;
	public static final int PROPERTY_FAN_TIMER_TIMEOUT_ID = 22;

	// Exception IDs
	public static final int EXCEPTION_INVALID_HIGHLOW_ID = 26;
	public static final int EXCEPTION_HEATCOOL_OFF_ID = 27;
	public static final int EXCEPTION_HEATCOOL_ON_ID = 28;
	// *** END ID Section ***
	
	
	// *** Declarations ***
	// Properties
	public static final DOFInterface.Property PROPERTY_DEVICE_ID;
	public static final DOFInterface.Property PROPERTY_NAME;
	public static final DOFInterface.Property PROPERTY_TEMP_SCALE; 
	// all temperatures stored in both F and C to reduce run-time computations
	public static final DOFInterface.Property PROPERTY_TARGET_TEMP_F;	
	public static final DOFInterface.Property PROPERTY_TARGET_HIGH_F;
	public static final DOFInterface.Property PROPERTY_TARGET_LOW_F;
	public static final DOFInterface.Property PROPERTY_AWAY_HIGH_F;
	public static final DOFInterface.Property PROPERTY_AWAY_LOW_F;
	public static final DOFInterface.Property PROPERTY_SOFTWARE_VERSION;
	public static final DOFInterface.Property PROPERTY_HVAC_MODE;
	public static final DOFInterface.Property PROPERTY_AMBIENT_F;
	public static final DOFInterface.Property PROPERTY_HAS_LEAF;
	public static final DOFInterface.Property PROPERTY_HUMIDITY;
	public static final DOFInterface.Property PROPERTY_CAN_COOL;
	public static final DOFInterface.Property PROPERTY_CAN_HEAT;
	public static final DOFInterface.Property PROPERTY_HAS_FAN;
	public static final DOFInterface.Property PROPERTY_FAN_TIMER_ACTIVE;
	public static final DOFInterface.Property PROPERTY_FAN_TIMER_TIMEOUT;

	// Exceptions
	// Setting a target or away high temperature lower than the low (or vice versa) indicates an exception
	public static final DOFInterface.Exception EXCEPTION_INVALID_HIGHLOW;
	public static final DOFInterface.Exception EXCEPTION_HEATCOOL_OFF;
	public static final DOFInterface.Exception EXCEPTION_HEATCOOL_ON;
	// Setting a name as an empty string or null is an exception
	// *** END Declarations

	// Build struct
	static{
		// build DEF
		DEF= new DOFInterface.Builder(IID) // addProperty(itemID, writable, readable, DOFType
		.addProperty(PROPERTY_DEVICE_ID_ID, false, true, StringVal)
		.addProperty(PROPERTY_NAME_ID, false, true, StringVal)		
		.addProperty(PROPERTY_TEMP_SCALE_ID, false, true, StringVal)
		.addProperty(PROPERTY_TARGET_TEMP_F_ID, true, true, UInt8Val)
		.addProperty(PROPERTY_TARGET_HIGH_F_ID, true, true, UInt8Val)
		.addProperty(PROPERTY_TARGET_LOW_F_ID, true, true, UInt8Val)
		.addProperty(PROPERTY_AWAY_HIGH_F_ID, false, true, UInt8Val)
		.addProperty(PROPERTY_AWAY_LOW_F_ID, false, true, UInt8Val)
		.addProperty(PROPERTY_SOFTWARE_VERSION_ID, false, true, StringVal)
		.addProperty(PROPERTY_HVAC_MODE_ID, true, true, StringVal)
		.addProperty(PROPERTY_AMBIENT_F_ID, false, true, UInt8Val)
		.addProperty(PROPERTY_HAS_LEAF_ID, false, true, BoolVal)
		.addProperty(PROPERTY_HUMIDITY_ID, false, true, UInt8Val)
		.addProperty(PROPERTY_CAN_COOL_ID, false, true, BoolVal)
		.addProperty(PROPERTY_CAN_HEAT_ID, false, true, BoolVal)
		.addProperty(PROPERTY_HAS_FAN_ID, false, true, BoolVal)
		.addProperty(PROPERTY_FAN_TIMER_ACTIVE_ID, true, true, BoolVal)
		.addProperty(PROPERTY_FAN_TIMER_TIMEOUT_ID, false, true, DateTimeVal)
		// add exceptions
		.addException(EXCEPTION_INVALID_HIGHLOW_ID, new DOFType[] {})
		.addException(EXCEPTION_HEATCOOL_OFF_ID, new DOFType[]{})
		.addException(EXCEPTION_HEATCOOL_ON_ID, new DOFType[]{})
		.build();

		// DEF.getProperty on all property IDs
		PROPERTY_DEVICE_ID = DEF.getProperty(PROPERTY_DEVICE_ID_ID);
		PROPERTY_NAME = DEF.getProperty(PROPERTY_NAME_ID);
		PROPERTY_TEMP_SCALE = DEF.getProperty(PROPERTY_TEMP_SCALE_ID);
		PROPERTY_TARGET_TEMP_F = DEF.getProperty(PROPERTY_TARGET_TEMP_F_ID);
		PROPERTY_TARGET_HIGH_F = DEF.getProperty(PROPERTY_TARGET_HIGH_F_ID);
		PROPERTY_TARGET_LOW_F = DEF.getProperty(PROPERTY_TARGET_LOW_F_ID);
		PROPERTY_AWAY_HIGH_F = DEF.getProperty(PROPERTY_AWAY_HIGH_F_ID);
		PROPERTY_AWAY_LOW_F = DEF.getProperty(PROPERTY_AWAY_LOW_F_ID);
		PROPERTY_SOFTWARE_VERSION = DEF.getProperty(PROPERTY_SOFTWARE_VERSION_ID);
		PROPERTY_HVAC_MODE = DEF.getProperty(PROPERTY_HVAC_MODE_ID);
		PROPERTY_AMBIENT_F = DEF.getProperty(PROPERTY_AMBIENT_F_ID);
		PROPERTY_HAS_LEAF = DEF.getProperty(PROPERTY_HAS_LEAF_ID);
		PROPERTY_HUMIDITY = DEF.getProperty(PROPERTY_HUMIDITY_ID);
		PROPERTY_CAN_COOL = DEF.getProperty(PROPERTY_CAN_COOL_ID);
		PROPERTY_CAN_HEAT = DEF.getProperty(PROPERTY_CAN_HEAT_ID);
		PROPERTY_HAS_FAN = DEF.getProperty(PROPERTY_HAS_FAN_ID);
		PROPERTY_FAN_TIMER_ACTIVE = DEF.getProperty(PROPERTY_FAN_TIMER_ACTIVE_ID);
		PROPERTY_FAN_TIMER_TIMEOUT = DEF.getProperty(PROPERTY_FAN_TIMER_TIMEOUT_ID);
		
		// DEF.getException on all exception IDs
		EXCEPTION_INVALID_HIGHLOW = DEF.getException(EXCEPTION_INVALID_HIGHLOW_ID);
		EXCEPTION_HEATCOOL_OFF = DEF.getException(EXCEPTION_HEATCOOL_OFF_ID);
		EXCEPTION_HEATCOOL_ON = DEF.getException(EXCEPTION_HEATCOOL_ON_ID);
	}
}
