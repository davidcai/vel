package samoyan.core;

public class Pair<K, V>
{
    private K key;
    private V value;
    private static final String OPEN_BRACE = "{";
    private static final String COMMA = ",";
    private static final String CLOSE_BRACE = "}";

    public Pair()
    {
    }

    /**
	* @param key key to be set
	* @param value value to be set
	*/
    public Pair(final K key, final V value)
    {
        this.key = key;
        this.value = value;
    }

    /**
	* @param key key to be set
	* @param value value to be set
	* @return this
	*/
    public Pair<K, V> setPair(final K key, final V value)
    {
        this.key = key;
        this.value = value;
        return this;
    }

    /**
	* @return key
	*/
    public K getKey()
    {
        return key;
    }

    /**
	* @param key key to be set
	* @return this
	*/
    public Pair<K, V> setKey(final K key)
    {
        this.key = key;
        return this;
    }

    /**
	* @return value
	*/
    public V getValue()
    {
        return value;
    }

    /**
	* @param value value to be set
	* @return this
	*/
    public Pair<K, V> setValue(final V value)
    {
        this.value = value;
        return this;
    }

    public String toString()
    {
        return OPEN_BRACE + key + COMMA + value + CLOSE_BRACE;
    }

    /**
	* Matches checked only on key
	*
	* @param o
	* @return
	*/
    @Override
    public boolean equals(Object o)
    {
        if (this==o) return true;
        if (o==null || getClass()!=o.getClass()) return false;

        Pair<K,V> pair = (Pair<K,V>) o;

        if (key!=null? !key.equals(pair.key) : pair.key!=null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        return key!=null? key.hashCode() : 0;
    }
}
