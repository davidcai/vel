package samoyan.core;

import java.util.Iterator;
import java.util.List;

public class ReverseIterator<E> implements Iterator<E>
{
	public List<E> baseList;
	public int cursor;
	
	public ReverseIterator(List<E> list)
	{
		this.baseList = list;
		this.cursor = list.size();
	}
	
	@Override
	public boolean hasNext()
	{
		return this.cursor>0;
	}

	@Override
	public E next()
	{
		return baseList.get(--this.cursor);
	}

	@Override
	public void remove()
	{
		baseList.remove(this.cursor);
	}
}
