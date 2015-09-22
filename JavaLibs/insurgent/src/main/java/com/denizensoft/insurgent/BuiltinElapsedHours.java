package com.denizensoft.insurgent;

import com.denizensoft.jlib.Tempus;
import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.Scope;

import java.util.Date;

public class BuiltinElapsedHours extends Function
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

		return String.format("%.2f", Tempus.calcElapsedHours(date1, date2));
	}

	public BuiltinElapsedHours()
	{
		// Call with jsCalcElapsedHours( String date1, String date2 )
		//
		super("jsCalcElapsedHours");
	}
}
