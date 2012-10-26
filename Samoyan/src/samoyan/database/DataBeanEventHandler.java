package samoyan.database;

import java.util.UUID;

public abstract class DataBeanEventHandler
{
	/**
	 * Called before a bean is saved to the database.
	 * @param bean
	 * @param insert If <code>true</code>, this is a new bean that is yet to be inserted.
	 * @throws Exception
	 */
	public void onBeforeSave(DataBean bean, boolean insert) throws Exception {}
	
	/**
	 * Called after a bean is saved to the database, but before its dirty flags are cleared.
	 * This allows event handlers to detect which columns changed.
	 * @param bean
	 * @param insert If <code>true</code>, this is a new bean that was just inserted.
	 * @throws Exception
	 */
	public void onAfterSave(DataBean bean, boolean insert) throws Exception {}

	/**
	 * Indicates if the bean can be removed.
	 * @return
	 * @throws Exception
	 */
	public boolean canRemove(UUID beanID) throws Exception {return true;}
	
	/**
	 * Called before a bean is removed from the database. The bean can still be loaded via a call to load.
	 * @param beanID
	 * @throws Exception
	 */
	public void onBeforeRemove(UUID beanID) throws Exception {}
	
	/**
	 * Called after the bean is removed from the database. The bean can no longer be loaded via a call to load.
	 * @param beanID
	 * @throws Exception
	 */
	public void onAfterRemove(UUID beanID) throws Exception {}
}
