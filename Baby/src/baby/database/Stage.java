package baby.database;

/**
 * This <code>Stage</code> represents the stage of the pregnancy of the mother, which can be "preconception", "pregnancy week", or "infancy month".
 * It includes several convenience methods to work with this data.
 * This class is not a <code>DataBean</code>, but rather a data type.
 * @author brian
 *
 */
public class Stage
{
	public static final int MAX_WEEKS = 40;
	public static final int MAX_MONTHS = 12;
	
	private int key = 0;
	
	protected Stage()
	{
	}
	
	public static Stage fromInteger(int stageKey)
	{
		Stage result = new Stage();
		result.key = stageKey;
		return result;
	}
	
	public int toInteger()
	{
		return this.key;
	}

	public static Stage preconception()
	{
		Stage result = new Stage();
		result.key = 1;
		return result;
	}
	
	/**
	 * 
	 * @param week The week of the pregnancy, between 1 and 40.
	 * @return
	 */
	public static Stage pregnancy(int week)
	{
		Stage result = new Stage();
		result.key = 100+week;
		return result;
	}
	
	public static Stage invalid()
	{
		return new Stage();
	}
	
	/**
	 * 
	 * @param week The age of the baby in months, between 1 and 12.
	 * @return
	 */
	public static Stage infancy(int month)
	{
		Stage result = new Stage();
		result.key = 200+month;
		return result;
	}

	public boolean isPreconception()
	{
		return key==1;
	}

	public boolean isPregnancy()
	{
		return key>=101 && key<=140;
	}
	public int getPregnancyWeek()
	{
		return isPregnancy()? key-100 : 0;
	}
	
	public boolean isInfancy()
	{
		return key>=201 && key<=212;
	}
	public int getInfancyMonth()
	{
		return isInfancy()? key-200 : 0;
	}
	
	public boolean isValid()
	{
		return isPregnancy() || isPreconception() || isInfancy();
	}
}
