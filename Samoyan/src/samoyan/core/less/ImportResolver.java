package samoyan.core.less;

import java.io.InputStream;

public interface ImportResolver
{
	public InputStream getInputStream(String name);
}
