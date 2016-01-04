package com.denizensoft.insurgent;

import com.denizensoft.jlib.Tempus;
import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.Scope;

import java.util.Date;

public class BuiltinElapsedDays extends Function
{
	@Override
	public String invoke(Scope scope, String[] stArgs)
	{
		int nArgs = stArgs.length;

		if(nArgs < 1)
			return "insufficient args?";

		Date
			date1 = Tempus.parseUtcStamp(stArgs[0], null),
			date2 = ( nArgs > 1 ? Tempus.parseUtcStamp(stArgs[1], null) : new Date());

		return Integer.toString(Tempus.calcElapsedDays(date1, date2));
	}

	public BuiltinElapsedDays()
	{
		super("calcElapsedDays");
	}
}
