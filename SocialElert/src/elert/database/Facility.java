package elert.database;

import java.util.Locale;
import java.util.UUID;

import samoyan.database.DataBean;

public class Facility extends DataBean
{
	public static final int MAXSIZE_NAME = 64;
	public static final int MAXSIZE_CODE = 4;
	public static final int MAXSIZE_PHONE = 24;
	public static final int MAXSIZE_ADDRESS = 512;
	public static final int MAXSIZE_STATE = 2;
	public static final int MAXSIZE_CITY = 64;
	public static final int MAXSIZE_ZIP = 5;
	
	public String getCode()
	{
		return (String) get("Code");
	}
	
	public void setCode(String code)
	{
		if(code != null)
			code =  code.toUpperCase(Locale.US) ; 
		set("Code", code);
	}
	
	public String getName()
	{
		return (String) get("Name");
	}
	
	public void setName(String name)
	{
		set("Name", name);
	}
	
	public UUID getServiceAreaID()
	{
		return (UUID) get("ServiceAreaID");
	}
	
	public void setServiceAreaID(UUID serviceAreaID)
	{
		
		set("ServiceAreaID", serviceAreaID);
	}
	
	public String getAddress()
	{
		return (String) get("Address");
	}
	
	public void setAddress(String address)
	{
		set("Address",  address);
	}
	
	public String getCity()
	{
		return (String) get("City");
	}
	
	public void setCity(String city)
	{
		set("City",  city);
	}
	
	public String getState()
	{
		return (String) get("State");
	}
	
	public void setState(String state)
	{
		if(state != null)
			state =  state.toUpperCase(Locale.US) ; 
		set("State",  state);
	}
	
	public String getZip()
	{
		return (String) get("Zip");
	}
	
	public void setZip(String zip)
	{
		set("Zip",  zip);
	}
	
	public String getPhone()
	{
		return (String) get("Phone");
	}
	
	public void setPhone(String phone)
	{
		set("Phone",  phone);
	}	
}
