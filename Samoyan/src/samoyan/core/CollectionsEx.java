package samoyan.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CollectionsEx
{
	/**
	 * Takes in a collection of elements, and breaks them into groups according to the <code>groupComparator</code>.
	 * Each group is then sorted according to the <code>sortComparator</code>.
	 * @param groupComparator
	 * @return
	 */
	public final static <T> Collection<Collection<T>> group(final Collection<T> collection, final Comparator<T> groupComparator, final Comparator<T> sortComparator)
	{
		List<Collection<T>> result = new ArrayList<Collection<T>>();

		int n = collection.size(); 
		if (n==0)
		{
			return result;
		}
		
		// Make a copy of the collection and sort it
		List<T> localCopy = new ArrayList<T>(collection);
		Collections.sort(localCopy, groupComparator);
		
		// Compare adjacent entries and bucketize
		List<T> insertBucket = null;
		T prevElem = null;
		for (int i=0; i<n; i++)
		{
			T currentElem = localCopy.get(i);
			if (prevElem==null || groupComparator.compare(prevElem, currentElem)!=0)
			{
				insertBucket = new ArrayList<T>();
				result.add(insertBucket);
			}
			insertBucket.add(currentElem);
			
			prevElem = currentElem;
		}
		
		// Sort each bucket
		for (Collection<T> bucket : result)
		{
			Collections.sort((List<T>) bucket, sortComparator);
		}
		
		// Sort the buckets by comparing the first element in each
		Collections.sort(result, new Comparator<Collection<T>>()
		{
			@Override
			public int compare(Collection<T> bucket1, Collection<T> bucket2)
			{
				T obj1 = ((List<T>) bucket1).get(0);
				T obj2 = ((List<T>) bucket2).get(0);
				return groupComparator.compare(obj1, obj2);
			}
		});
		
		return result;
	}
}
