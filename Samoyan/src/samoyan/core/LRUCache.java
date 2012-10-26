package samoyan.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LRUCache
{
	private class CleanerThread extends Thread
	{
		private ConcurrentHashMap<String, Slot> map = null;
		private int count;

		public CleanerThread(ConcurrentHashMap<String, Slot> map, int count)
		{
			this.map = map;
			this.count = count;
		}
		
		@Override
		public void run()
		{
			List<Slot> slots = new ArrayList<Slot>(this.map.size()+32);
			slots.addAll(this.map.values());
			Collections.sort(slots); // Sort in ascending order of last used date
			for (int i=0; i<slots.size() && i<this.count; i++)
			{
				this.map.remove(slots.get(i).key);
			}
		}
	}
	
	private class Slot implements Comparable<Slot>
	{
		String key = null;
		Object obj = null;
		long lastUsed = 0;
		
		public int compareTo(Slot that)
		{
			return (int) (this.lastUsed - that.lastUsed);
		}
	}
	
	private ConcurrentHashMap<String, Slot> map = null;
	private AtomicLong hits = new AtomicLong(0L);
	private AtomicLong misses = new AtomicLong(0L);
	private int capacity;
	
	/**
	 * Creates a new LRUCache.
	 * @param capacity The maximum number of items to be cached at any given time (older items invalidated).
	 */
	public LRUCache(int capacity)
	{
		this.capacity = capacity;
		this.map = new ConcurrentHashMap<String, Slot>(this.capacity+32);
	}
	
	/**
	 * Get the Object object by its key.
	 * @param key The key the object was inserted with.
	 * @return The object or <code>null</code> if not found.
	 */
	public Object get(String key)
	{
		long now = System.currentTimeMillis();
		
		Slot slot = this.map.get(key);
		if (slot==null)
		{
			this.misses.incrementAndGet();
			return null;
		}
		this.hits.incrementAndGet();
		
		slot.lastUsed = now;
		return slot.obj;
	}
	
	/**
	 * Clear the cache of all objects.
	 */
	public void clearAll()
	{
		this.map.clear();
	}
	
	public void resetSuccessRate()
	{
		this.hits.set(0);
		this.misses.set(0);
	}
	
	public float getSuccessRate()
	{
		float h = this.hits.get();
		float m = this.misses.get();
		
		if ((h + m)==0) return 0;
		return h / (h + m);
	}

	/**
	 * Insert an object to the cache.
	 * @param key The key to cache the object with.
	 * @param obj The object to insert.
	 * @return The previous cached object for this key, or <code>null</code> if there was none.
	 */
	public Object insert(String key, Object obj)
	{
		long now = System.currentTimeMillis();
		
		Object result = null;
			
		Slot slot = (Slot) map.get(key);
		if (slot==null)
		{
			// Create a new slot and insert as first in list
			slot = new Slot();
			slot.obj = obj;
			slot.key = key;
			slot.lastUsed = now;
			
			this.map.put(key, slot);
			
			if (this.map.size()>this.capacity)
			{
				invalidateOldest(this.capacity/4);
			}
		}
		else
		{
			result = slot.obj; // The previous object in the cache
			
			slot.obj = obj;
			slot.lastUsed = now;
		}
		
		return result;
	}
	
	/**
	 * @param key
	 */
	public Object invalidate(String key)
	{
		Slot slot = this.map.remove(key);
		if (slot!=null)
		{
			return slot.obj;
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Invalidates the oldest <code>count</code> items from the cache.
	 * Runs in a separate thread, asynchronously.
	 */
	public void invalidateOldest(int count)
	{
		CleanerThread t = new CleanerThread(this.map, count);
		t.start();
	}
	
	public List<String> getPrefixKeys(String prefix)
	{
		List<String> result = new ArrayList<String>();
		
		Iterator<String> iter = this.map.keySet().iterator();
		while (iter.hasNext())
		{
			String key = iter.next();
			if (key.startsWith(prefix)==true)
			{
				result.add(key);
			}
		}

		return result;
	}
	
	public List<Object> getPrefix(String prefix)
	{
		List<Object> result = new ArrayList<Object>();
		
		Iterator<Slot> iter = map.values().iterator();
		while (iter.hasNext())
		{
			Slot slot = iter.next();
			if (slot.key.startsWith(prefix)==true)
			{
				result.add(slot.obj);
			}
		}
		
		return result;
	}

	public List<Object> invalidatePrefix(String prefix)
	{
		List<Object> result = new ArrayList<Object>();
		
		Iterator<Slot> iter = map.values().iterator();
		while (iter.hasNext())
		{
			Slot slot = iter.next();
			if (slot.key.startsWith(prefix)==true)
			{
				result.add(slot.obj);
				iter.remove();
			}
		}
		
		return result;
	}

	public int size()
	{
		return this.map.size();
	}
}
