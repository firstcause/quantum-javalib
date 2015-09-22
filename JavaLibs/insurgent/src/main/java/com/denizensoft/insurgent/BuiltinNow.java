package com.denizensoft.insurgent;

import com.denizensoft.jlib.Tempus;
import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.Scope;


public class BuiltinNow extends Function
{
	@Override
	public String invoke(Scope scope, String[] stArgs)
	{
		return Tempus.tzStamp();
	}

	public BuiltinNow()
	{
		super("now");
	}
}
