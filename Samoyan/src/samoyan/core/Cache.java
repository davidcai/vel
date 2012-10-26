package samoyan.core;

public final class Cache
{
	private static LRUCache lru = null;
	
	public static void init(int capacity)
	{
		lru = new LRUCache(capacity);
	}
	
	public static Object get(String key)
	{
		return lru==null? null : lru.get(key);
	}
	
	public static Object insert(String key, Object obj)
	{
		return lru==null? null : lru.insert(key, obj);
	}
	
	public static Object invalidate(String key)
	{
		return lru==null? null : lru.invalidate(key);
	}
		
	public static float getSuccessRate()
	{
		return lru==null? 0F : lru.getSuccessRate();
	}

	public static int getCount()
	{
		return lru==null? 0 : lru.size();
	}
	
	public static void clearAll()
	{
		if (lru!=null) lru.clearAll();
	}	
}
