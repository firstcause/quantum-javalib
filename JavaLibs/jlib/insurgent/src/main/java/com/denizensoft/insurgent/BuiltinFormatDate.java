package com.denizensoft.insurgent;

import com.denizensoft.jlib.LibException;
import com.denizensoft.jlib.Tempus;
import com.denizensoft.jlib.Function;
import com.denizensoft.jlib.Scope;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class BuiltinFormatDate extends Function
{
	@Override
	public void addedToScope(Scope scope)
	{
		// Add a bunch of variables!!!
		//
		try
		{
			scope.setVariable("DATESTAMP", Tempus.DATESTAMP);
			scope.setVariable("INVOICESTAMP", Tempus.INVOICESTAMP);

			scope.setVariable("MONTH", Tempus.MONTH);
			scope.setVariable("MONTHM", Tempus.MONTHM);
			scope.setVariable("MONTHMM", Tempus.MONTHMM);
			scope.setVariable("MONTHSHORT", Tempus.MONTHSHORT);

			scope.setVariable("TIMEAMPM", Tempus.TIMEAMPM);
			scope.setVariable("TIMEMILITARY", Tempus.TIMEMILITARY);
			scope.setVariable("TIMESTAMP", Tempus.TIMESTAMP);
			scope.setVariable("TZSTAMP", Tempus.TZSTAMP);
			scope.setVariable("UTCSTAMP", Tempus.TZSTAMP);

			scope.setVariable("LOCALTZ", TimeZone.getDefault().getID());
			scope.setVariable("LOCALTZHUMAN", TimeZone.getDefault().getDisplayName());
		}
		catch(LibException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public String invoke(Scope scope, String[] stArgs)
	{
		int nArgs = stArgs.length;

		String
			stDate = ( nArgs > 0 ? stArgs[0] : Tempus.utcStamp(null)),
			stFormat = ( nArgs > 1 ? stArgs[1] : Tempus.TIMEAMPM );

		TimeZone timeZone = (nArgs > 2 ? TimeZone.getTimeZone(stArgs[2]) : TimeZone.getDefault());

		SimpleDateFormat dateFormat = new SimpleDateFormat(Tempus.UTCSTAMP);

		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		Date date = null;

		try
		{
			date = (Date)dateFormat.parse(stDate);
		}
		catch(ParseException e)
		{
			e.printStackTrace();
		}
		return Tempus.formatDate(date, stFormat, timeZone);
	}

	public BuiltinFormatDate()
	{
		super("formatDate");
	}
}
