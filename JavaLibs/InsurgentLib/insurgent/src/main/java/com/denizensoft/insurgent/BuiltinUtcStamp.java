package com.denizensoft.insurgent;

import com.denizensoft.jlib.Tempus;
import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.Scope;


public class BuiltinUtcStamp extends Function
{
	@Override
	public String invoke(Scope scope, String[] stArgs)
	{
		return Tempus.utcStamp(null);
	}

	public BuiltinUtcStamp()
	{
		super("utcStamp");
	}
}
