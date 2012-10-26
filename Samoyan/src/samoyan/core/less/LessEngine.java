/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package samoyan.core.less;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import samoyan.core.Debug;
import samoyan.core.Util;
import samoyan.servlet.Controller;

/**
 * @author Rostislav Hristov
 * @author Uriah Carpenter
 * @author Noah Sloan
 * @see http://lesscss.org/
 * @see http://www.asual.com/lesscss/
 */
public class LessEngine
{	
	private Scriptable scope;
	private ClassLoader classLoader;
	private Function compileString;
	private Function compileFile;
	
	public LessEngine()
	{
		try
		{
			Debug.logln("Initializing LESS Engine");
			
			this.classLoader = getClass().getClassLoader();

			InputStream env = Controller.getResourceAsStream("WEB-INF/less/env.js");
			InputStream engine = Controller.getResourceAsStream("WEB-INF/less/engine.js");
			InputStream cssmin= Controller.getResourceAsStream("WEB-INF/less/cssmin.js");
			InputStream less = Controller.getResourceAsStream("WEB-INF/less/less.js");
			
			Context cx = Context.enter();
			Debug.logln("Using implementation version: " + cx.getImplementationVersion());
			cx.setOptimizationLevel(9);
			Global global = new Global();
			global.init(cx);
			this.scope = cx.initStandardObjects(global);

			cx.evaluateReader(this.scope, new InputStreamReader(env), "env.js", 1, null);
			cx.evaluateString(this.scope, "lessenv.charset = 'UTF-8';", "charset", 1, null);
			cx.evaluateString(this.scope, "lessenv.css = false;", "css", 1, null); // whether or not to allow parsing of CSS files
			cx.evaluateReader(this.scope, new InputStreamReader(less), "less.js", 1, null);
			cx.evaluateReader(this.scope, new InputStreamReader(cssmin), "cssmin.js", 1, null);
			cx.evaluateReader(this.scope, new InputStreamReader(engine), "engine.js", 1, null);
			
			this.compileString = (Function) this.scope.get("compileString", scope);
			this.compileFile = (Function) this.scope.get("compileFile", scope);
			Context.exit();
		}
		catch (Exception e)
		{
			Debug.logStackTrace(e);
		}
	}
		
	public String compile(String input, boolean compress, ImportResolver resolver) throws LessException
	{
		try
		{
			// Resolve @import directives
			StringBuilder parsedInput = null;
			int pointer = 0;
			while (true)
			{
				int at = input.indexOf("@import", pointer);
				if (at<0)
				{
					if (parsedInput!=null)
					{
						parsedInput.append(input.substring(pointer));
					}
					break;
				}
				
				if (resolver==null)
				{
					throw new Exception("@import resolver not specified");
				}
				
				if (parsedInput==null)
				{
					parsedInput = new StringBuilder(input.length());
				}
							
				int quote1 = input.indexOf("\"", at);
				if (quote1<0)
				{
					throw new Exception("@import directive opening quote not found");
				}
				int quote2 = input.indexOf("\"", quote1+1);
				if (quote2<0)
				{
					throw new Exception("@import directive closing quote not found");
				}
				
				String importName = input.substring(quote1+1, quote2);
				InputStream importStream = resolver.getInputStream(importName);
				if (importStream==null && importName.indexOf(".")<0)
				{
					importStream = resolver.getInputStream(importName + ".less");
				}
				if (importStream==null)
				{
					throw new Exception("@import file \"" + importName + "\" could not be resolved");
				}
				
				int semi = input.indexOf(";", at);
				if (semi<0)
				{
					throw new Exception("@import directive semicolon not found");
				}
				
				parsedInput.append(input.substring(pointer, at));
				parsedInput.append(Util.inputStreamToString(importStream, "UTF-8"));
	
				pointer = semi + 1;
			}
			if (parsedInput!=null)
			{
				input = parsedInput.toString();
			}
			
			// Compile
			long time = System.currentTimeMillis();
			String result = call(compileString, new Object[] {input, compress});
			Debug.logln("LESS compilation took " + (System.currentTimeMillis () - time) + "ms");
			return result;
		}
		catch (Exception e)
		{
			throw parseLessException(e);
		}
	}
		
	private synchronized String call(Function fn, Object[] args)
	{
		return (String) Context.call(null, fn, scope, scope, args);
	}
	
	private static LessException parseLessException(Exception root) throws LessException
	{
		Debug.logStackTrace(root);
		
		if (root instanceof JavaScriptException)
		{
			Scriptable value = (Scriptable) ((JavaScriptException) root).getValue();
			String type = ScriptableObject.getProperty(value, "type").toString() + " Error";
			String message = ScriptableObject.getProperty(value, "message").toString();
			String filename = "";
			if (ScriptableObject.getProperty(value, "filename")!=null)
			{
				filename = ScriptableObject.getProperty(value, "filename").toString(); 
			}
			int line = -1;
			if (ScriptableObject.hasProperty(value, "line"))
			{
				line = ((Double) ScriptableObject.getProperty(value, "line")).intValue(); 
			}
			int column = -1;
			if (ScriptableObject.hasProperty(value, "column"))
			{
				column = ((Double) ScriptableObject.getProperty(value, "column")).intValue();
			}				
			List<String> extractList = new ArrayList<String>();
			if (ScriptableObject.hasProperty(value, "extract"))
			{
				NativeArray extract = (NativeArray) ScriptableObject.getProperty(value, "extract");
				for (int i = 0; i < extract.getLength(); i++)
				{
					if (extract.get(i, extract) instanceof String)
					{
						extractList.add(((String) extract.get(i, extract)).replace("\t", " "));
					}
				}
			}
			throw new LessException(message, type, filename, line, column, extractList);
		}
		throw new LessException(root);
	}
}