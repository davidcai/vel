package samoyan.core;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServlet;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringEscapeUtils;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public final class Util
{
	private static AtomicInteger uniqueNum = new AtomicInteger(0);
	private static String localHostName = null;
	
	private Util()
	{
	}
	
	public static int nextRoundRobin()
	{
		int result = uniqueNum.incrementAndGet();
		if (result>Integer.MAX_VALUE/2)
		{
			uniqueNum.compareAndSet(result, 0);
		}
		return result;
	}
	
	public static boolean isNaN(float f)
	{
		return !(f<0) && !(f>=0);
	}
	
	/**
	 * Checks two objects for equality, supporting <code>null</code> values.
	 * The strings will be deemed equal if both are <code>null</code>.
	 * Same as doing <code>(x!=null && y!=null)? x.equals(y) : (x==null && y==null)</code>.
	 * @param x
	 * @param y
	 * @return
	 */
	public static boolean objectsEqual(Object x, Object y)
	{
		return (x!=null && y!=null)? x.equals(y) : (x==null && y==null);
	}
	
	public static String strReplace(String source, String match, String replacement)
	{
		StringBuffer result = null;
		int p;
		int q = 0;
		while (true)
		{
			p = source.indexOf(match, q);
			if (p<0)
			{
				if (q==0) return source; // Match not found at all, return orig string
				
				if (result==null) result = new StringBuffer(source.length()); // Just in time creation
				result.append(source.substring(q));
				break;
			}
			else
			{
				if (result==null) result = new StringBuffer(source.length()); // Just in time creation
				result.append(source.substring(q, p));
				result.append(replacement);
				q = p + match.length();
			}
		}
		return result.toString();
	}

	/**
	 * Encode a string to ASCII URL form. All special characters are converted to %XX where
	 * XX stands for the binary value.
	 * This method supports UTF-8 encoding.
	 */
	public static String urlEncode(String raw)
	{
		try
		{
			return urlEncode(raw, false, null);
		}
		catch (UnsupportedEncodingException e)
		{
			// Will not happen when passing null for charset
		}
		return null;
	}
	public static String urlEncode(String raw, String charset) throws UnsupportedEncodingException
	{
		return urlEncode(raw, false, charset);
	}
	public static String urlEncode(String raw, boolean dbl, String charset) throws UnsupportedEncodingException
	{
		int n = raw.length();
		StringBuffer result = new StringBuffer(n);
		for (int i=0; i<n; i++)
		{
			char ch = raw.charAt(i);
			if (ch==' ')
			{
				// Space
				if (dbl==false)
				{
					result.append("+");
				}
				else
				{
					result.append("%2b");
				}
			}
			else if ((ch>='a' && ch<='z') || (ch>='A' && ch<='Z') || (ch>='0' && ch<='9') || ch=='.' || ch=='_' || ch=='-' || ch=='*' || ch=='{' || ch=='}')
			{
				// Standard chars
				result.append(ch);
			}
			else
			{
				// Unicode multibyte chars
				int[] array = null;
				if (charset==null || charset.equalsIgnoreCase("UTF-8"))
				{
					array = utf8Encode(ch);
				}
				else
				{
					byte[] bytes = ("" + ch).getBytes(charset);
					array = new int[bytes.length];
					for (int j=0; j<bytes.length; j++)
					{
						array[j] = (int) bytes[j];
						if (array[j]<0) array[j] += 256;
					}
				}
				
				for (int j=0; j<array.length; j++)
				{
					int hi = array[j] / 16;
					int lo = array[j] % 16;

					result.append("%");
					if (dbl==true) result.append("25");
					
					if (hi<10)
						result.append(hi);
					else
						result.append((char) ('a' + hi - 10));
					if (lo<10)
						result.append(lo);
					else
						result.append((char) ('a' + lo - 10));
				}
			}
		}
		return result.toString();
	}

	/**
	 * Decodes an encoded URL that uses UTF-8 to encode high order characters.
	 * @param encoded The encoded URL.
	 * @return
	 */
	public static String urlDecode(String encoded)
	{
		int n = encoded.length();
		StringBuffer result = new StringBuffer(n);
		for (int i=0; i<n; i++)
		{
			char ch = encoded.charAt(i);
			if (ch=='+')
			{
				// Space
				result.append(" ");
			}
			else if (ch=='%')
			{
				int[] utf8 = null;
				
				int z = getEscapedByte(encoded.substring(i+1, i+3));
				if (z<192)
				{
					// Optimize, no need to decode
					result.append((char) z);
					i += 2;
					continue;
				}
				else if (z>=192 && z<=223)
				{
					utf8 = new int[2];
				}
				else if (z>=224 && z<=239)
				{
					utf8 = new int[3];
				}
				else if (z>=240 && z<=247)
				{
					utf8 = new int[4];
				}
				else if (z>=248 && z<=251)
				{
					utf8 = new int[5];
				}
				else if (z==252 || z==253)
				{
					utf8 = new int[6];
				}
				else
				{
					// Invalid UTF-8 encoding in URL
					continue;
				}
				
				utf8[0] = z;
				i += 3;

				boolean err = false;
				for (int j=1; j<utf8.length; j++)
				{
					char percent = encoded.charAt(i);
					if (percent!='%')
					{
						// Invalid UTF-8 encoding in URL
						err = true;
						break;
					}
					utf8[j] = getEscapedByte(encoded.substring(i+1, i+3));
					i += 3;
				}
				
				// i will be incremented in the loop
				i--;
				if (err) continue;

				try
				{
					int ud = utf8Decode(utf8);
					result.append((char) ud);
				}
				catch (Exception exc)
				{
					// Invalid UTF-8 encoding in URL
				}
			}
//			else if ((ch>='a' && ch<='z') || (ch>='A' && ch<='Z') || (ch>='0' && ch<='9') || ch=='.' || ch=='_' || ch=='-')
			else  if (ch>=0x20 && ch<0x80)
			{
				// Standard chars
				result.append(ch);
			}
		}
		return result.toString();
	}

	private static int getEscapedByte(String s)
	{
		int result = 0;
		
		char hi = s.charAt(0);
		char lo = s.charAt(1);
		
		if (hi<='9' && hi>='0')
		{
			result = (hi-'0') * 16;
		}
		else if (hi<='f' && hi>='a')
		{
			result = (hi-'a'+10) * 16;	
		}
		else if (hi<='F' && hi>='A')
		{
			result = (hi-'A'+10) * 16;	
		}

		if (lo<='9' && lo>='0')
		{
			result += (lo-'0');
		}
		else if (lo<='f' && lo>='a')
		{
			result += (lo-'a'+10);	
		}
		else if (lo<='F' && lo>='A')
		{
			result += (lo-'A'+10);	
		}
		
		return result;
	}

	/**
	 * Encode special characters in the <code>raw</code> string to use ampersand encoding. For example,
	 * "&lt;" is converted to "&amp;#60;".
	 */
	public final static String htmlEncode(String raw)
	{
//		return StringEscapeUtils.escapeHtml(raw);

		int n = raw.length();
		StringBuffer result = null;
		for (int i=0; i<n; i++)
		{
			char ch = raw.charAt(i);
			if (ch=='\n' || ch=='\r' || ch=='\t')
			{
				if (result!=null) result.append(ch);
			}
			else if (ch=='\"' || ch=='\'' || ch=='<' || ch=='>' || ch=='&' || ch>=128 || ch<32)
			{
				if (result==null)
				{
					result = new StringBuffer(n*5/4);
					result.append(raw.substring(0, i));
				}
				
				result.append("&#");
				result.append((int) ch);
				result.append(";");
			}
			else
			{
				if (result!=null) result.append(ch);
			}
		}
		
		if (result!=null)
			return result.toString();
		else
			return raw;
	}	

	/**
	 * Decode ampersand codes in the <code>encoded</code> string to characters. For example,
	 * "&amp;lt;" is converted to "&lt;".
	 */
	public final static String htmlDecode(String encoded)
	{
		return StringEscapeUtils.unescapeHtml4(encoded);
	}
	
	public static String charsetDecode(String encoded, String charsetName) throws UnsupportedEncodingException
	{
		byte[] bytes = new byte[encoded.length()];
		for (int i=0; i<bytes.length; i++)
		{
			bytes[i] = (byte) encoded.charAt(i);
		}
		return new String(bytes, charsetName);
	}

	/**
	 * Decodes a string whose array characters represents an array of bytes in UTF-8 encoding.
	 * @param encoded The encoded string.
	 * @return The decoded string.
	 */
	public static String utf8Decode(String encoded)
	{
		int n = encoded.length();
		StringBuffer result = new StringBuffer(n);
		for (int i=0; i<n; i++)
		{
			int[] utf8 = null;
			int z = (int) encoded.charAt(i);
			if (z<192)
			{
				// Optimize, no need to decode
				result.append((char) z);
				continue;
			}
			else if (z>=192 && z<=223)
			{
				utf8 = new int[2];
			}
			else if (z>=224 && z<=239)
			{
				utf8 = new int[3];
			}
			else if (z>=240 && z<=247)
			{
				utf8 = new int[4];
			}
			else if (z>=248 && z<=251)
			{
				utf8 = new int[5];
			}
			else if (z==252 || z==253)
			{
				utf8 = new int[6];
			}
			else
			{
				// Invalid UTF-8 encoding in URL
				continue;
			}
				
			utf8[0] = z;
			i ++;

			boolean err = false;
			for (int j=1; j<utf8.length; j++)
			{
				if (i>=encoded.length())
				{
					err = true;
					break;
				}
				utf8[j] = (int) encoded.charAt(i);
				if (utf8[j]>255)
				{
					err = true;
					break;
				}
				i ++;
			}
				
			// i will be incremented in the loop
			i--;
			if (err)
			{
//				continue;
				return encoded;
			}

			try
			{
				int ud = utf8Decode(utf8);
				result.append((char) ud);
			}
			catch (Exception exc)
			{
				// Invalid UTF-8 encoding in URL
			}
		}
		return result.toString();
	}

	/**
	 * Returns the multi-byte represenation of the character per the UTF-8 encoding scheme.
	 */
	private static int[] utf8Encode(int ud)
	{
		int[] result;
		
		if (ud<128)
		{
			result = new int[1];
			result[0] = ud;
		}
		else if (ud>=128 && ud<=2047)
		{
			result = new int[2];
			result[0] = 192 + (ud / 64);
			result[1] = 128 + (ud % 64);
		}
		else if (ud>=2048 && ud<=65535)
		{
			result = new int[3];
			result[0] = 224 + (ud / 4096);
			result[1] = 128 + ((ud / 64) % 64);
			result[2] = 128 + (ud % 64);
		}
		else if (ud>=65536 && ud<=2097151)
		{
			result = new int[4];
			result[0] = 240 + (ud / 262144);
			result[1] = 128 + ((ud / 4096) % 64);
			result[2] = 128 + ((ud / 64) % 64);
			result[3] = 128 + (ud % 64);
		}
		else if (ud>=2097152 && ud<=67108863)
		{
			result = new int[5];
			result[0] = 248 + (ud / 16777216);
			result[1] = 128 + ((ud / 262144) % 64);
			result[2] = 128 + ((ud / 4096) % 64);
			result[3] = 128 + ((ud / 64) % 64);
			result[4] = 128 + (ud % 64);
		}
		else if (ud>=67108864 && ud<=2147483647)
		{
			result = new int[6];
			result[0] = 252 + (ud / 1073741824);
			result[1] = 128 + ((ud / 16777216) % 64);
			result[2] = 128 + ((ud / 262144) % 64);
			result[3] = 128 + ((ud / 4096) % 64);
			result[4] = 128 + ((ud / 64) % 64);
			result[5] = 128 + (ud % 64);
		}
		else
		{
			result = null;
		}
		
		return result;
	}

	/**
	 * Returns the character represented by the UTF-8 encoding.
	 */
	private static int utf8Decode(int[] utf)
	{
		int z = utf[0];
		if (z<192)
		{
			return z;
		}
		else if (z>=192 && z<=223)
		{
			int y = utf[1];
			return (z-192)*64 + (y-128);
		}
		else if (z>=224 && z<=239)
		{
			int y = utf[1];
			int x = utf[2];
			return (z-224)*4096 + (y-128)*64 + (x-128);
		}
		else if (z>=240 && z<=247)
		{
			int y = utf[1];
			int x = utf[2];
			int w = utf[3];
			return (z-240)*262144 + (y-128)*4096 + (x-128)*64 + (w-128);
		}
		else if (z>=248 && z<=251)
		{
			int y = utf[1];
			int x = utf[2];
			int w = utf[3];
			int v = utf[4];
			return (z-248)*16777216 + (y-128)*262144 + (x-128)*4096 + (w-128)*64 + (v-128);
		}
		else if (z==252 || z==253)
		{
			int y = utf[1];
			int x = utf[2];
			int w = utf[3];
			int v = utf[4];
			int u = utf[5];
			return (z-252)*1073741824 + (y-128)*16777216 + (x-128)*262144 + (w-128)*4096 + (v-128)*64 + (u-128);
		}
		else
		{
			// something wrong!
			throw new NumberFormatException("Invalid UTF-8 encoding");
		}
	}

	/**
	 * Returns <code>true</code> if the given <code>String</code> is <code>null</code>
	 * or contains only whitespaces.
	 * @param str The <code>String</code> to check.
	 * @return <code>true</code> if empty.
	 */
	static public boolean isEmpty(String str)
	{
		if (str==null) return true;
//		if (str.trim().length()==0) return true;

		int n = str.length();
		for (int i=0; i<n; i++)
		{
			char ch = str.charAt(i);
			if (Character.isWhitespace(ch)==false)
			{
				return false;
			}
		}
		return true;
	}

	public static String exceptionDesc(Throwable t)
	{
		StringWriter strWrt = new StringWriter();
		PrintWriter prtWrt = new PrintWriter(strWrt);
		t.printStackTrace(prtWrt);
		String desc = strWrt.getBuffer().toString();
		
		int p = desc.indexOf(HttpServlet.class.getName());
		if (p<0) return desc;
		p = desc.lastIndexOf("\n", p);
		if (p<0) return desc;
		return desc.substring(0, p+1).trim();
	}

	public static byte[] uuidToBytes(UUID uuid)
	{
		byte[] result = new byte[16];
		ByteBuffer byteBuf = ByteBuffer.wrap(result);
		byteBuf.putLong(uuid.getMostSignificantBits());
		byteBuf.putLong(uuid.getLeastSignificantBits());
		return result;
	}

	public static UUID bytesToUUID(byte[] bytes)
	{
		ByteBuffer byteBuf = ByteBuffer.wrap(bytes);
		long most = byteBuf.getLong();
		long least = byteBuf.getLong();
		return new UUID(most, least);
	}
	
	public static List<String> tokenize(String source, String delim)
	{
		if (source==null)
		{
			return new ArrayList<String>(0);
		}
		
		List<String> result = new ArrayList<String>();
		int p;
		int q = 0;
		while (true)
		{
			p = source.indexOf(delim, q);
			if (p<0)
			{
				result.add(source.substring(q));
				break;
			}
			else
			{
				result.add(source.substring(q, p));
				q = p + delim.length();
			}
		}
		
		return result;
	}
	
	/**
	 * Extracts the domain name from a host name.
	 * For example, "www.example.org" will return "example.org",
	 * "localhost" will return "localhost" and
	 * "example.org" will return "example.org".
	 * "abc.example.org/subfolder" will return "example.org".
	 * @param hostName
	 * @return
	 */
	public final static String domainPart(String hostName)
	{
		if (hostName==null) return null;
		
		int p = hostName.indexOf("://");
		if (p>=0) hostName = hostName.substring(p+3);
		
		p = hostName.indexOf("/");
		if (p>=0) hostName = hostName.substring(0, p);

		// Find first dot character
		int firstDot = hostName.indexOf(".");
		if (firstDot<0) return hostName;			// No dot, e.g. "localhost"
		
		// Find last dot charater
		int lastDot = hostName.lastIndexOf(".");
		if (lastDot==firstDot) return hostName;		// Only one dot, e.g. "example.org", "www.tv"
		
		// Classic host name case
		if (hostName.startsWith("www")) // will also take care of "www2.example.org"
		{
			return hostName.substring(firstDot+1);
		}
		
		// We know there's at least 2 dots and the server name is not "www"
		// e.g. "info.example.org", "alpha.info.example.org", "example.co.il", "info.example.co.il"
		
		// Get suffix
		String suffix = hostName.substring(lastDot+1);
		
		// IP number?
		try
		{
			int fld = Integer.parseInt(suffix);
			if (fld>=0 && fld<=255)
			{
				return hostName;
			}
		}
		catch (NumberFormatException nfe)
		{
			// Not a number
		}
		
		// Check TLD length
		int beforeLastDot = hostName.lastIndexOf(".", lastDot-1);
		if (suffix.length()>=3)
		{
			// .com, .org, .net, .biz, .gov, .info, .name, etc.
			return hostName.substring(beforeLastDot+1);
		}
		
		// We now know it's a ccTLD: .uk, .il, .ru, etc.

		// At least 4 chars in before last domain name part, e.g. "info.example.tv"
		if (lastDot-beforeLastDot>4)
		{
			return hostName.substring(beforeLastDot+1);
		}
		
		// More than 3 parts, e.g. "info.example.co.il"
		if (beforeLastDot!=firstDot)
		{
			int beforeBeforeLastDot = hostName.lastIndexOf(".", beforeLastDot-1);
			return hostName.substring(beforeBeforeLastDot+1);
		}
		
		// We know the host name has exactly 3 parts but we can't distinguish between the cases
		// e.g. "ibm.co.il", "ibm.org.il" or "alpha.ibm.it"
		return hostName;
	}

	/**
	 * Checks if the given string is a UUID.
	 * @param uuidStr
	 * @return
	 */
	public static boolean isUUID(String uuidStr)
	{
		if (uuidStr==null || uuidStr.length()!=36)
		{
			return false;
		}
		
		for (int i=0; i<36; i++)
		{
			char ch = uuidStr.charAt(i);
			if (i==8 || i==13 || i==18 || i==23)
			{
				if (ch != '-') return false;
			}
			else
			{
				if ((ch<'0' || ch>'9') && (ch<'a' || ch>'f') && (ch<'A' || ch>'F')) return false;
			}
		}
		return true;
	}

//	public static boolean isValidPhoneNumber(String phoneNumber)
//	{
//		return phoneNumber!=null && phoneNumber.trim().matches("^[0-9]+$");
//	}
	
	/**
	 * Performs a regex check to see if an email address is valid.
	 * @param email The address to check.
	 * @return <code>true</code> if the address seems valid, <code>false</code> otherwise.
	 * A return of <code>true</code> does not guarantee that the address is valid.
	 */
	public static boolean isValidEmailAddress(String email)
	{
		return email!=null && email.trim().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
	}

//	/**
//	 * Performs very basic tests to see if an email address is valid.
//	 * @param email The address to check.
//	 * @return <code>true</code> if the address seems valid, <code>false</code> otherwise.
//	 * A return of <code>true</code> does not necessarily mean the address is valid.
//	 */
//	public static boolean isValidEmailAddress(String email)
//	{
//		if (email==null) return false;					// null
//		int at = email.indexOf("@");
//		if (at<0) return false;							// No @
//		if (at>65) return false;						// local part limited to 64 chars
//		if (at==email.length()-1) return false;			// @ was last char
//		if (at==0) return false;						// @ was first char
//		
//		int p = email.indexOf("@", at+1);
//		if (p>0) return false;							// Second @ found
//		
//		int dot = email.indexOf(".", at+1);
//		if (dot<0) return false;						// No . after @
//		
//		// Check before @
////		String invalidChars = "!\"#$%&(),:;<>[]\\`|";
//		for (int i=0; i<at; i++)
//		{
//			char ch = email.charAt(i);
//			if (Character.isWhitespace(ch)==true) return false;	// Whitespace found
//			if ((int) ch >= 127) return false;					// Hi order char
////			if (invalidChars.indexOf(ch)>0) return false;		// Illegal char
//			if ((ch<'a' || ch>'z') && (ch<'A' || ch>'Z') &&
//				(ch<'0' || ch>'9') && ch!='-' && ch!='.' &&
//				ch!='+' && ch!='_') return false;				// Only a-z, 0-9, dot, plus, underscore and hyphen in local part
//		}
//		
//		// Check after @
//		for (int i=at+1; i<email.length(); i++)
//		{
//			char ch = email.charAt(i);
//			if (Character.isWhitespace(ch)==true) return false;	// Whitespace found
//			if ((int) ch >= 127) return false;					// Hi order char
//			if ((ch<'a' || ch>'z') && (ch<'A' || ch>'Z') &&
//				(ch<'0' || ch>'9') && ch!='-' && ch!='.') return false;		// Only a-z, 0-9, dot and hyphen in domain names
//		}
//		
//		if (email.indexOf("..", at+1)>0) return false;			// Two consecutive periods
//		
//		return true;									// Looks OK but not certain
//	}
	
	public static long getFreeSpace(String path) throws Exception
	{
		if (System.getProperty("os.name").startsWith("Windows"))
		{
			return getFreeSpaceOnWindows(path);
		}
		else
		{
			return 0;
		}
	}
	
	private static long getFreeSpaceOnWindows(String path) throws Exception
	{
		long bytesFree = -1;

		// Create the .bat file
		File script = new File(System.getProperty("java.io.tmpdir"), "freebytes" + nextRoundRobin() + ".bat");
		PrintWriter writer = new PrintWriter(new FileWriter(script, false));
		writer.println("dir \"" + path + "\"");
		writer.flush();
		writer.close();
		
		// Get the output from running the .bat file
		Process p = Runtime.getRuntime().exec(script.getAbsolutePath());
		InputStream reader = new BufferedInputStream(p.getInputStream());
		StringBuffer buffer = new StringBuffer();
		for (;;)
		{
			int c = reader.read();
			if (c<0) break;
			buffer.append((char) c);
		}
		String outputText = buffer.toString();
		reader.close();

		// Parse the output text for the bytes free info
		StringTokenizer tokenizer = new StringTokenizer(outputText, "\n");
		while (tokenizer.hasMoreTokens())
		{
			String line = tokenizer.nextToken().trim();
			// See if line contains the bytes free information
			if (line.endsWith("bytes free"))
			{
				tokenizer = new StringTokenizer(line, " ");
				tokenizer.nextToken();
				tokenizer.nextToken();
				bytesFree = Long.parseLong(tokenizer.nextToken().replaceAll(",", ""));
			}
		}
		
		// Delete the batch file
		script.delete();
		
		return bytesFree;
	}

	/**
	 * Encodes all special characters and convert newlines to &lt;br&gt; or &lt;p&gt; tags
	 * Also detect titles automatically - short lines with no period at end 
	 * with two newlines before and after - and encase with &lt;b&gt; tags.
	 * @param text The original text string.
	 * @return The converted HTML string.
	 * @throws IOException
	 */
	static public String textToHtml(String text)
	{
		// First encode all special chars to prevent tags or ampersands
		text = Util.htmlEncode(text.trim());

		// Allocate result buffer
		StringBuffer result = new StringBuffer(text.length());

		// Read line by line and add tags
		String prevLine = "";
		String line;
		boolean titleLine = false;
		boolean inBulletedList = false;
		BufferedReader reader = new BufferedReader(new StringReader(text));
		while (true)
		{
			try
			{
				line = reader.readLine();
			}
			catch (IOException exc)
			{
				// Should never happen
				return null;
			}
			
			if (line==null || line.trim().length()==0) // Last line or spacer line
			{
				boolean bullet = prevLine.length()>2 && prevLine.startsWith("* ");
				if (bullet)
				{
					if (inBulletedList==false)
					{
						result.append("<ul>");
						inBulletedList = true;
					}
					
					result.append("<li>");
					prevLine = prevLine.substring(2);
				}
				else
				{
					if (inBulletedList==true)
					{
						result.append("</ul>");
						inBulletedList = false;
					}
				}

				if (prevLine.trim().length()>0)
				{
					if (titleLine && line!=null) result.append("<b>"); // Last line cannot be a title
										
					result.append(addLinks(prevLine));
					
					if (titleLine && line!=null) result.append("</b>"); // Last line cannot be a title
					if (line!=null) // Don't add BR and P after last line
					{
//						result.append("<p>");
						result.append("<br><br>");
					}
				}
				
				if (inBulletedList==true)
				{
					result.append("</ul>");
					inBulletedList = false;
				}

				if (line==null) break;
			}
			else if (prevLine.trim().length()>0) // Normal line
			{
				boolean bullet = prevLine.length()>2 && prevLine.startsWith("* ");
				if (bullet)
				{
					if (inBulletedList==false)
					{
						result.append("<ul>");
						inBulletedList = true;
					}
					
					result.append("<li>");
					prevLine = prevLine.substring(2);
				}
				else
				{
					if (inBulletedList==true)
					{
						result.append("</ul>");
						inBulletedList = false;
					}
				}
				
				result.append(addLinks(prevLine));
				
				if (inBulletedList==false && line.startsWith("* ")==false)
				{
					result.append("<br>");
				}
			}
			
			titleLine = (prevLine.length()==0 &&
						line.trim().length()>0 &&
						line.length()<=120 &&
						Character.isLetterOrDigit(line.charAt(line.length()-1))
						&& line.startsWith("* ")==false);
			
			prevLine = line;
			line = null;
		}
		
		return result.toString();
	}
	
	private static String addLinks(String text)
	{
		// The call order is important here to prevent recursiveness
		text = addLinks(text, "http:");
		text = addLinks(text, "https:");
		text = addLinks(text, "www.");
		return text;
	}
	
	/**
	 * Detect links in text and add A tags.
	 * @param protocol <code>"http:"</code>, <code>"https:"</code>, <code>"mailto:"</code>
	 * or <code>"www."</code>.  
	 * @param text
	 * @return
	 */
	private static String addLinks(String text, String protocol)
	{
		StringBuffer result = new StringBuffer();
		
		int q = 0;
		int p;
		while (true)
		{
			p = text.indexOf(protocol, q);
			if (p<0)
			{
				if (q==0) return text; // Optimize
				result.append(text.substring(q));
				break;
			}

			int a = text.indexOf("<a", p);
			int b = text.indexOf("</a>", p);
			if (b>=0 && (a<0 || a>b))
			{
				// We are in the midst of a link tag
				result.append(text.substring(q, b+4));
				q = b+4;
				continue;
			}
			
			result.append(text.substring(q, p));
			result.append("<a href=\"");
			if (protocol.indexOf(":")<0)
			{
				result.append("http://");
			}
			
			// Look for the next whitespace
			for (q=p; q<text.length(); q++)
			{
				char ch = text.charAt(q);
				if (ch==' ' || Character.isWhitespace(ch)==true)
				{
					break;
				}
			}
			char pch = text.charAt(q-1);
			if (pch=='.' || pch==',' || pch==')' || pch==';' ||
				pch=='?' || pch=='\'' || pch=='\"')
			{
				q--;
			}
			
			result.append(text.substring(p, q));
			result.append("\">");
//			result.append("\">");
			result.append(text.substring(p, q));
			result.append("</a>");
		}
		
		return result.toString();
	}
	
	/**
	 * Removes all tags from the HTML code and returns the textual content only.
	 * Spaces may be added in lieu of tags to prevent concatenation of non-adjacent text.
	 * @param html The HTML code.
	 * @return The textual content of the input HTML code.
	 */
	public static String htmlToText(String html)
	{
		if (html==null) return null;
		
		int n = html.length();
		StringBuffer result = new StringBuffer(n/2);
		
		boolean inTag = false;
		boolean inAmp = false;
		boolean ws = true;
		boolean parag = true;
		int ampStart = 0;
		for (int i=0; i<n; i++)
		{
			char ch = html.charAt(i);
			if (inTag==false && ch=='<')
			{
				inTag = true;
				inAmp = false;
				
				if (parag==false && (html.indexOf("<p ", i)==i || html.indexOf("<p>", i)==i))
				{
					result.append("\r\n\r\n");
					ws = true;
					parag = true;
				}
				else if (html.indexOf("<br ", i)==i || html.indexOf("<br>", i)==i)
				{
					result.append("\r\n");
					ws = true;
					parag = false;
				}
				else
				{
					parag = false;
				}
			}
			else if (inTag==true && ch=='>')
			{
				char peek = 0;
				if (i<n-1)
				{
					peek = html.charAt(i+1);
				}

				inTag = false;
				if (ws==false && Character.isWhitespace(peek)==false && peek!='<')
				{
					result.append(" ");
					ws = true;
				}
			}
			else if (inTag==false && inAmp==false && ch=='&')
			{
				inAmp = true;
				ampStart = i;
			}
			else if (inAmp==true && ch==';')
			{
				inAmp = false;
				String ampCode = html.substring(ampStart+1, i);
				if (ampCode.equalsIgnoreCase("nbsp"))
				{
					result.append(" ");
				}
				else if (ampCode.equalsIgnoreCase("lt"))
				{
					result.append("<");
				}
				else if (ampCode.equalsIgnoreCase("gt"))
				{
					result.append(">");
				}
				else if (ampCode.equalsIgnoreCase("amp"))
				{
					result.append("&");
				}
				else if (ampCode.equalsIgnoreCase("quot") ||
						ampCode.equalsIgnoreCase("ldquo") ||
						ampCode.equalsIgnoreCase("rdquo") ||
						ampCode.equalsIgnoreCase("sdquo"))
				{
					result.append("\"");
				}
				else if (ampCode.equalsIgnoreCase("apos") ||
						ampCode.equalsIgnoreCase("lsquo") ||
						ampCode.equalsIgnoreCase("rsquo") ||
						ampCode.equalsIgnoreCase("sbquo"))
				{
					result.append("'");
				}
				else if (ampCode.startsWith("#"))
				{
					try
					{
						int code = Integer.parseInt(ampCode.substring(1));
						result.append((char) code);
					}
					catch (Exception e)
					{
						
					}
				}
			}
			else if (inTag==false && inAmp==false)
			{
				boolean prevWs = ws;
				ws = Character.isWhitespace(ch);
				if (ws)
				{
					if (prevWs==false) result.append(" ");
				}
				else
				{
					result.append(ch);
				}
			}
		}
		
		return result.toString();
	}
	
	/**
	 * Remove unsafe characters from the string so that it can be used
	 * as part of a URL. Hyphens will be added instead of removed characters.
	 * @param raw The string to check and convert
	 * @return The converted string will have only A-Z, a-z, 0-9 characters or hyphens.
	 */
	public static String urlSafe(String raw)
	{
		if (raw==null) return null;
		
		boolean dash = true;
		int n = raw.length();
		StringBuffer result = new StringBuffer(n);
		for (int i=0; i<n; i++)
		{
			char ch = raw.charAt(i);
			if ((ch>='a' && ch<='z') || (ch>='A' && ch<='Z') || (ch>='0' && ch<='9'))
			{
				// Standard chars
				result.append(ch);
				dash = false;
			}
			else if ((ch=='.' || ch=='-' || Character.isWhitespace(ch)) && dash==false)
			{
				result.append("-");
				dash = true;
			}
			else
			{
				Map<String, String> map = getUrlSafeMap();
				String mapped = map.get(String.valueOf(ch));
				if (mapped!=null)
				{
					result.append(mapped);
					dash = false;
				}
				else if (dash==false)
				{
					result.append("-");
					dash = true;
				}
			}
		}
		
		String str = result.toString();
		if (str.length()>0 && str.charAt(0)=='-')
		{
			str = str.substring(1);
		}
		if (str.length()>0 && str.charAt(str.length()-1)=='-')
		{
			str = str.substring(0, str.length()-1);
		}
		return str;
	}
	
	private static Map<String, String> urlSafeMap = null;
	private static Map<String, String> getUrlSafeMap()
	{
		if (urlSafeMap==null)
		{
			Map<String, String> localMap = new HashMap<String, String>();
			
			mapRange(localMap, 0xc0, 0xc5, "A");
			mapRange(localMap, 0xc6, 0xc6, "AE");
			mapRange(localMap, 0xc7, 0xc7, "C");
			mapRange(localMap, 0xc8, 0xcb, "E");
			mapRange(localMap, 0xcc, 0xcf, "I");
			mapRange(localMap, 0xd0, 0xd0, "D");
			mapRange(localMap, 0xd1, 0xd1, "N");
			mapRange(localMap, 0xd2, 0xd6, "O");
			mapRange(localMap, 0xd8, 0xd8, "O");
			mapRange(localMap, 0xd9, 0xdc, "U");
			mapRange(localMap, 0xdd, 0xdd, "Y");
			mapRange(localMap, 0xde, 0xde, "P");
			mapRange(localMap, 0xdf, 0xdf, "ss");
			mapRange(localMap, 0xe0, 0xe5, "a");
			mapRange(localMap, 0xe6, 0xe6, "ae");
			mapRange(localMap, 0xe7, 0xe7, "c");
			mapRange(localMap, 0xe8, 0xeb, "e");
			mapRange(localMap, 0xec, 0xef, "i");
			mapRange(localMap, 0xf0, 0xf0, "o");
			mapRange(localMap, 0xf1, 0xf1, "n");
			mapRange(localMap, 0xf2, 0xf6, "o");
			mapRange(localMap, 0xf8, 0xf8, "o");
			mapRange(localMap, 0xf9, 0xfc, "u");
			mapRange(localMap, 0xfd, 0xfd, "y");
			mapRange(localMap, 0xfe, 0xfe, "p");
			mapRange(localMap, 0xff, 0xff, "y");
			
			mapRangeAlt(localMap, 0x100, 0x105, "A", "a");
			mapRangeAlt(localMap, 0x106, 0x10d, "C", "c");
			mapRangeAlt(localMap, 0x10e, 0x111, "D", "d");
			mapRangeAlt(localMap, 0x112, 0x11b, "E", "e");
			mapRangeAlt(localMap, 0x11c, 0x123, "G", "g");
			mapRangeAlt(localMap, 0x124, 0x127, "H", "h");
			mapRangeAlt(localMap, 0x128, 0x131, "I", "i");
			mapRangeAlt(localMap, 0x132, 0x133, "Ij", "ij");
			mapRangeAlt(localMap, 0x134, 0x135, "J", "j");
			mapRangeAlt(localMap, 0x136, 0x137, "K", "k");
			mapRange(localMap, 0x138, 0x138, "k");
			mapRangeAlt(localMap, 0x139, 0x142, "L", "l");
			mapRangeAlt(localMap, 0x143, 0x14b, "N", "n");
			mapRangeAlt(localMap, 0x14c, 0x151, "O", "o");
			mapRangeAlt(localMap, 0x152, 0x153, "OE", "oe");
			mapRangeAlt(localMap, 0x154, 0x159, "R", "r");
			mapRangeAlt(localMap, 0x15a, 0x161, "S", "s");
			mapRangeAlt(localMap, 0x162, 0x167, "T", "t");
			mapRangeAlt(localMap, 0x168, 0x173, "U", "u");
			mapRangeAlt(localMap, 0x174, 0x175, "W", "w");
			mapRangeAlt(localMap, 0x176, 0x178, "Y", "y");
			mapRangeAlt(localMap, 0x179, 0x17e, "Z", "z");
			mapRange(localMap, 0x17f, 0x17f, "s");
			mapRange(localMap, 0x18f, 0x18f, "e");
			mapRange(localMap, 0x192, 0x192, "f");
			mapRangeAlt(localMap, 0x1a0, 0x1a1, "O", "o");
			mapRangeAlt(localMap, 0x1af, 0x1b0, "U", "u");
			mapRangeAlt(localMap, 0x1cd, 0x1ce, "A", "a");
			mapRangeAlt(localMap, 0x1cf, 0x1d0, "I", "i");
			mapRangeAlt(localMap, 0x1d1, 0x1d2, "O", "o");
			mapRangeAlt(localMap, 0x1d3, 0x1dc, "U", "u");
			mapRangeAlt(localMap, 0x1fa, 0x1fb, "A", "a");
			mapRangeAlt(localMap, 0x1fe, 0x1ff, "O", "o");
			mapRangeAlt(localMap, 0x1fc, 0x1fd, "AE", "ae");
			mapRange(localMap, 0x259, 0x259, "e");

			mapRangeAlt(localMap, 0x1e80, 0x1e85, "W", "w");
			mapRangeAlt(localMap, 0x1ea0, 0x1eb7, "A", "a");
			mapRangeAlt(localMap, 0x1eb8, 0x1ec7, "E", "e");
			mapRangeAlt(localMap, 0x1ec8, 0x1ecb, "I", "i");
			mapRangeAlt(localMap, 0x1ecc, 0x1ee3, "O", "o");
			mapRangeAlt(localMap, 0x1ee4, 0x1ef1, "U", "u");
			mapRangeAlt(localMap, 0x1ef2, 0x1ef9, "Y", "y");
			
			urlSafeMap = localMap;
		}
		return urlSafeMap;
	}
	
	private static void mapRange(Map<String, String> map, int from, int to, String mapTo)
	{
		for (int i=from; i<=to; i++)
		{
			char ch = (char) i;
			map.put(String.valueOf(ch), mapTo);
		}
	}
	
	private static void mapRangeAlt(Map<String, String> map, int from, int to, String upper, String lower)
	{
		for (int i=from; i<=to; i++)
		{
			char ch = (char) i;
			if ((i-from) % 2 == 0)
			{
				map.put(String.valueOf(ch), upper);
			}
			else
			{
				map.put(String.valueOf(ch), lower);
			}
		}
	}

	/**
	 * Remove unsafe characters from the string so that it can be used
	 * as the name of an email sender or recipient.
	 * @param raw The string to check and convert
	 * @return The converted string will have no non A-Z, a-z, 0-9 characters.
	 */
	public static String emailSafe(String raw)
	{
		if (raw==null) return null;
		
		int n = raw.length();
		StringBuffer result = new StringBuffer(n);
		for (int i=0; i<n; i++)
		{
			char ch = raw.charAt(i);
			if ((ch>='a' && ch<='z') || (ch>='A' && ch<='Z') || (ch>='0' && ch<='9') ||
				ch=='-' || ch=='.' || ch=='_' || ch=='-' || ch!=',')
			{
				// Standard chars
				result.append(ch);
			}
			else if (Character.isWhitespace(ch))
			{
				result.append(" ");
			}
			else
			{
				Map<String, String> map = getUrlSafeMap();
				String mapped = map.get(String.valueOf(ch));
				if (mapped!=null)
				{
					result.append(mapped);
				}
			}
		}
		return result.toString();
	}
	
	public static void base64Encode(InputStream raw, OutputStream encoded, boolean chunked) throws IOException
	{
//		BASE64Encoder encoder = new BASE64Encoder();
//		encoder.encode(raw, encoded);

		byte[] buffer = new byte[57*64]; // Must be multiplication of 57 for even chunking
		while (true)
		{
			int count = raw.read(buffer);
			if (count<=0) break;
			
			while (count<buffer.length)
			{
				int count2 = raw.read(buffer, count, buffer.length-count);
				if (count2<=0) break;
				count += count2;
			}
			
			if (count==buffer.length)
			{
				encoded.write(Base64.encodeBase64(buffer, chunked));
			}
			else
			{
				byte[] partial = new byte[count];
				System.arraycopy(buffer, 0, partial, 0, count);
				encoded.write(Base64.encodeBase64(partial, chunked));
			}
		}
	}
	
	/**
	 * Returns an abstract of the text. Typically, the first few sentences that are no longer than <code>maxLength</code>.
	 * @param text
	 * @return
	 */
	public static String getTextAbstract(String text, int maxLength)
	{
		if (text==null) return null;
		if (text.length()<=maxLength) return text;
		
		// Find first, second and third end of sentences
		int p = text.indexOf(". ");
		int q = -1;
		if (p>=0)
		{
			q = text.indexOf(". ", p+2);
		}
		int r = -1;
		if (q>=0)
		{
			r = text.indexOf(". ", q+2);
		}
		
		// If 3 sentences fit in MAX_LEN chars, return them
		if (r>=0 && r<=maxLength)
		{
			return text.substring(0, r+1);
		}

		// If 2 sentences fit in MAX_LEN chars, return them
		if (q>=0 && q<=maxLength)
		{
			return text.substring(0, q+1);
		}
		
		// If 1 sentence fits in MAX_LEN chars, return it
		if (p>=0 && p<=maxLength)
		{
			return text.substring(0, p+1);
		}
		
		// Return as many words as fit in MAX_LEN chars
		String result = text.substring(0, maxLength);
		char ch;
		do
		{
			ch = result.charAt(result.length()-1);
			result = result.substring(0, result.length()-1);
		}
		while (Character.isWhitespace(ch)==false);
		
		return result;
	}
	
	/**
	 * Returns the name of the machine running this instance of the application.
	 * Based on its network name.
	 * @return
	 */
	public static String getLocalHostName()
	{
		if (localHostName==null)
		{
			try
			{
				localHostName = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException exc)
			{
				// Nothing to do here
			}
		}
		return localHostName;
	}

	public static String hashSHA256(String text)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.reset();
			md.update(text.getBytes("UTF-8"));
			return byteArrayToHexString(md.digest());
		}
		catch (Exception e)
		{
			// Shouldn't happen
			return null;
		}
	}

	public static String byteArrayToHexString(byte[] data)
	{
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++)
        {
        	int halfbyte = (data[i] >>> 4) & 0x0F;
        	int two_halfs = 0;
        	do
        	{
	        	if ((0 <= halfbyte) && (halfbyte <= 9))
	        	{
	                buf.append((char) ('0' + halfbyte));
	        	}
	            else
	            {
	            	buf.append((char) ('a' + (halfbyte - 10)));
	            }
	        	halfbyte = data[i] & 0x0F;
        	}
        	while (two_halfs++ < 1);
        }
        return buf.toString();
    }

	/**
	 * Returns <code>true</code> if the source HTML is <code>null</code>
	 * or contains only tags and whitespaces, i.e. no readable characters.
	 * @param html The source HTML to check.
	 * @return
	 */
	static public boolean isEmptyHTML(String html)
	{
		if (html==null) return true;

		boolean inTag = false;
		boolean inAmp = false;
		int ampStart = 0;
		int n = html.length();
		for (int i=0; i<n; i++)
		{
			char ch = html.charAt(i);
			if (inTag==false && ch=='<')
			{
				inTag = true;
			}
			else if (inTag==true && ch=='>')
			{
				inTag = false;
			}
			else if (inTag==false && inAmp==false && ch=='&')
			{
				inAmp = true;
				ampStart = i;
			}
			else if (inAmp==true && ch==';')
			{
				inAmp = false;
				String ampCode = html.substring(ampStart+1, i);
				if (ampCode.equalsIgnoreCase("nbsp")==false)
				{
					return false;
				}
			}
			else if (inTag==false && inAmp==false && Character.isWhitespace(ch)==false)
			{
				return false;
			}
		}
		return true;
	}
	
	public final static String trimHTML(String html)
	{
		do
		{
			html = html.trim();
			if (html.startsWith("<br>"))
			{
				html = html.substring(4);
				continue;
			}
			if (html.startsWith("<br/>"))
			{
				html = html.substring(5);
				continue;
			}
			if (html.startsWith("<p>"))
			{
				html = html.substring(3);
				continue;
			}
			if (html.endsWith("<br>"))
			{
				html = html.substring(0, html.length()-4);
				continue;
			}
			if (html.endsWith("<br/>"))
			{
				html = html.substring(0, html.length()-5);
				continue;
			}
			if (html.endsWith("<p>"))
			{
				html = html.substring(0, html.length()-3);
				continue;
			}
		}
		while (false);
		
		return html;
	}
	
	/**
	 *  Returns a bitset containing the values in bytes.
	 *  The byte-ordering of bytes must be big-endian which means the most significant bit is in element 0.
	 */	
	public static BitSet bytesToBitSet(byte[] bytes)
	{
	    BitSet bits = new BitSet();
	    for (int i=0; i<bytes.length*8; i++)
	    {
	        if ((bytes[bytes.length-i/8-1]&(1<<(i%8))) > 0)
	        {
	            bits.set(i);
	        }
	    }
	    return bits;
	}

	/**
	 *  Returns a byte array of at least length 1. 
	 *  The most significant bit in the result is guaranteed not to be a 1 (since BitSet does not support sign extension). 
	 *  The byte-ordering of the result is big-endian which means the most significant bit is in element 0.
	 *  The bit at index 0 of the bit set is assumed to be the least significant bit.
	  */	
	public static byte[] bitSetToBytes(BitSet bits)
	{
	    byte[] bytes = new byte[bits.length()/8+1];
	    for (int i=0; i<bits.length(); i++)
	    {
	        if (bits.get(i))
	        {
	            bytes[bytes.length-i/8-1] |= 1<<(i%8);
	        }
	    }
	    return bytes;
	}
	
	public static String randomPassword(int size)
	{
		final String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"; 
		
		Random rnd = new Random();
		String pw = "";
		for (int j=0; j<size; j++)
		{
			pw += str.charAt(rnd.nextInt(str.length()));
		}

		return pw;
	}
	
	public static byte[] inputStreamToBytes(InputStream stm) throws IOException
	{
		if (stm==null) return null;
		
		ByteOutputStream os = new ByteOutputStream();
		byte[] buffer = new byte[1024];
		int totalCount = 0;
		while (true)
		{
			int count = stm.read(buffer);
			if (count<=0) break;
			totalCount += count;
			os.write(buffer, 0, count);
		}
		byte[] result = new byte[totalCount];
		System.arraycopy(os.getBytes(), 0, result, 0, totalCount);
		os.close();
		return result;
	}

	public static String inputStreamToString(InputStream stm, String charset) throws IOException
	{
		if (stm==null) return null;
		
		ByteOutputStream os = new ByteOutputStream();
		byte[] buffer = new byte[1024];
		int totalCount = 0;
		while (true)
		{
			int count = stm.read(buffer);
			if (count<=0) break;
			totalCount += count;
			os.write(buffer, 0, count);
		}
		byte[] bytes = os.getBytes();
		os.close();
		return new String(bytes, 0, totalCount, charset);
	}

	public static byte[] hexStringToByteArray(String s)
	{
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2)
	    {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	public static void shutdownAndAwaitTermination(ExecutorService exec)
	{
		if (exec==null) return;
		
		// Disable new tasks from being submitted
		exec.shutdown();
		try
		{
			// Wait a while for existing tasks to terminate
			if (!exec.awaitTermination(60, TimeUnit.SECONDS))
			{
				// Cancel currently executing tasks
				exec.shutdownNow();
				
				// Wait a while for tasks to respond to being cancelled
				if (!exec.awaitTermination(60, TimeUnit.SECONDS))
				{
					Debug.logln("ExecutorService did not terminate");
				}
			}
		}
		catch (InterruptedException ie)
		{
			// (Re-)Cancel if current thread also interrupted
			exec.shutdownNow();
			
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * This method strips the country code to return only the number. For example, "US/18005551234" -> "18005551234". 
	 * @param phoneNumber In the format "US/18005551234".
	 * @return
	 */
	public static String stripCountryCodeFromPhoneNumber(String phoneNumber)
	{
		if (phoneNumber==null)
		{
			return null;
		}
		int slash = phoneNumber.indexOf("/");
		if (slash<0)
		{
			return phoneNumber;
		}
		else
		{
			return phoneNumber.substring(slash+1);
		}
	}

	/**
	 * Encode special characters in the <code>raw</code> string to use backslash encoding. For example,
	 * double-quotes are converted to backslah+double quotes.
	 */
	public static String jsonEncode(String raw)
	{
		boolean converted = false;
		int n = raw.length();
		StringBuffer result = new StringBuffer(n);
		for (int i=0; i<n; i++)
		{
			char ch = raw.charAt(i);
			if (ch=='\"')
			{
				result.append("\\\"");
				converted = true;
			}
			else if (ch=='\\')
			{
				result.append("\\\\");
				converted = true;
			}
			else if (ch>=128 || ch<32)
			{
				result.append("\\u");
				String numStr = Integer.toHexString((int) ch);
				for (int z=0; z<4-numStr.length(); z++)
				{
					result.append("0");
				}
				result.append(numStr);
				converted = true;
			}
			else
			{
				result.append(ch);
			}
		}
		
		if (converted==true)
			return result.toString();
		else
			return raw;
	}
}
 