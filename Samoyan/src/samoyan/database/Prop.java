package samoyan.database;

import java.util.UUID;

final class Prop
{
	/** The ID of the property or image. */
	UUID id;
	
	/** The name of the property. */
	String name;
	
	/** The value of the property. */
	Object value;
		
	/** If true, indicates that this property is in fact an image. id will hold the image ID. */
	boolean img;
	
	Prop()
	{
		id = null;
		name = null;
		value = null;
		img = false;
	}	
}
