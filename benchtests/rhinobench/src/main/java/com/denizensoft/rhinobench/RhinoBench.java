package com.denizensoft.rhinobench;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.*;

/**
 * Created by sjm on 11/6/15.
 */
public class RhinoBench
{
	public static void rhinoLoadJS(Context jsContext, Scriptable scope, String stFileSpec)
	{
		try
		{
			InputStream inputStream = new FileInputStream(stFileSpec);

			Reader reader = new InputStreamReader(inputStream);

			jsContext.evaluateReader(scope, reader, stFileSpec, 1, null);
		}
		catch(FileNotFoundException e)
		{
			throw new RuntimeException("File not found: "+stFileSpec);
		}
		catch(IOException e)
		{
			throw new RuntimeException("IO exception while loading: "+stFileSpec);
		}
	}

	public static void main(String[] args)
	{
		Context jsContext = Context.enter();

		Scriptable globalScope = jsContext.initStandardObjects();

		Object wrappedOut = Context.javaToJS(System.out, globalScope);

		ScriptableObject.putProperty(globalScope, "out", wrappedOut);

		rhinoLoadJS(jsContext, globalScope, "appapi.js");
		rhinoLoadJS(jsContext, globalScope, "moment.js");
		rhinoLoadJS(jsContext, globalScope, "howdy.js");

		jsContext.evaluateString(globalScope, "scriptMain();", "<howdy>", 1, null);

		Context.exit();
	}
}
