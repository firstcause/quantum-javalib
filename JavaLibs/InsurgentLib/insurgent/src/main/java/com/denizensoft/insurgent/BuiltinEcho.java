package com.denizensoft.insurgent;

import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.Scope;

public class BuiltinEcho extends Function
{
	@Override
	public String invoke(Scope scope,String[] stArgs)
	{
		String s1 = "";

		for(String s2: stArgs)
			s1+=s2;

		return s1;
	}

	public BuiltinEcho()
	{
		super("echo");
	}
}
