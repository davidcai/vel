package samoyan.core;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * A locale specific comparator of strings. Not very efficient so do not use where high-performance is needed.
 */
public final class SortStringsAlphabetically implements Comparator<String>
{
	private Collator collator;

	public SortStringsAlphabetically(Locale locale)
	{
		this.collator = Collator.getInstance(locale);
	}

	public int compare(String str1, String str2)
	{
		CollationKey key1 = collator.getCollationKey(str1);
		CollationKey key2 = collator.getCollationKey(str2);

		return key1.compareTo(key2);
	}
}