package com.denizensoft.jlib;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by sjm on 11/8/2014.
 */
public class Tempus
{
	public static final String
			DATESTAMP = "yyyy-MM-dd",

	        DOW = "EEEE",
	        DOWSHORT = "EEE",

			DATEINTEGER = "yyyyMMdd",
			INVOICESTAMP = "yyyyMMddHHmm",

			MONTH = "MMMM",
			MONTHM = "M",
			MONTHMM = "MM",
			MONTHSHORT = "MMM",

			MMSLASHDD = "MM/dd",

			FRIENDLYFULLAMPM = "hh:mm a - LLLL d, yyyy",
			DATETIMEAMPM = "MM/dd hh:mm a",
			TIMEAMPM = "h:mm a",
			TIMEMILITARY = "HH:mm",
			TIMESTAMP = "HH:mm:ss",

			TZFULL = "zzzz",
			TZSHORT = "zzz",

			TZSTAMP = "yyyy-MM-dd HH:mm:ss zzzz",
			UTCSTAMP = "yyyy-MM-dd HH:mm:ss";

	static public Date calcAddDays(Date startDate,int nDays)
	{
		if(startDate == null)
			startDate = new Date();

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);

		calendar.add(Calendar.DAY_OF_YEAR,nDays);

		Date date = calendar.getTime();

		return date;
	}

	static public Date calcAddHours(Date startDate,int nHours)
	{
		if(startDate == null)
			startDate = new Date();

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);

		calendar.add(Calendar.HOUR_OF_DAY,nHours);

		return calendar.getTime();
	}

	static public Date calcAddMinutes(Date startDate,int nMinutes)
	{
		if(startDate == null)
			startDate = new Date();

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);

		calendar.add(Calendar.MINUTE,nMinutes);

		return calendar.getTime();
	}

	static public Date calcAddSeconds(Date startDate,int nSeconds)
	{
		if(startDate == null)
			startDate = new Date();

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);

		calendar.add(Calendar.MILLISECOND,(nSeconds * 1000));

		Date date = calendar.getTime();

		return date;
	}

	static public String calcAddStampMinutes(String stTimeStamp,String stMinutes)
	{
		Date
				date1 = parseUtcStamp(stTimeStamp,null),
				date2 = calcAddMinutes(date1,Integer.parseInt(stMinutes));

		return formatDate(date2, Tempus.UTCSTAMP, TimeZone.getTimeZone("UTC"));
	}

	static public Date calcAddWeeks(Date startDate,int nWeeks)
	{
		return calcAddDays(startDate, (nWeeks * 7));
	}

	static public String calcElapsedHours(String stStampAfter,String stStampBefore)
	{
		if(stStampAfter == null)
			stStampAfter = utcStamp(null);

		Date
				dateAfter = parseUtcStamp(stStampAfter,null),
				dateBefore = parseUtcStamp(stStampBefore,null);

		return String.format("%.2f",calcElapsedHours(dateAfter, dateBefore));
	}

	static public float calcElapsedHours(Date dateAfter, Date dateBefore)
	{
		Calendar
				calendar1 = Calendar.getInstance(),
				calendar2 = Calendar.getInstance();

		calendar1.setTime(dateAfter);
		calendar2.setTime(dateBefore);

		long
				nElapsedMillis = ( calendar1.getTimeInMillis() - calendar2.getTimeInMillis()),
				nElapsedSeconds = ( nElapsedMillis / 1000 );

		float fElapsedHours = (((float)nElapsedSeconds ) / (float)3600 );

		return fElapsedHours;
	}

	static public Date calcNextDOW(Date startDate, int nTargetDOW)
	{
		int nDifference = 0;

		if(startDate == null)
			startDate = new Date();

		if( nTargetDOW > 7)
			nTargetDOW = ( nTargetDOW % 7 );

		nDifference = calcDiffForNextDOW(startDate, nTargetDOW);

		return calcAddDays(startDate, nDifference);
	}

	static public Date calcPriorDOW(Date startDate, int nTargetDOW)
	{
		int nDifference = 0;

		if(startDate == null)
			startDate = new Date();

		if( nTargetDOW > 7)
			nTargetDOW = ( nTargetDOW % 7 );

		nDifference = calcDiffForPriorDOW(startDate, nTargetDOW);

		return calcAddDays(startDate, nDifference);
	}

	static public int calcDiffForNextDOW(Date startDate,int nTargetDOW)
	{
		if(startDate == null)
			startDate = new Date();

		if(nTargetDOW > 7)
			nTargetDOW = ( nTargetDOW % 7 );

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);

		int nStartFromDOW = calendar.get(Calendar.DAY_OF_WEEK);

		if(nStartFromDOW < nTargetDOW)
			return ( nTargetDOW - nStartFromDOW );

		return (( nTargetDOW+7 ) - nStartFromDOW );
	}

	static public int calcDiffForPriorDOW(Date startDate,int nTargetDOW)
	{
		if(startDate == null)
			startDate = new Date();

		if(nTargetDOW > 7)
			nTargetDOW = ( nTargetDOW % 7 );

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(startDate);

		int nStartFromDOW = calendar.get(Calendar.DAY_OF_WEEK);

		if(nStartFromDOW < nTargetDOW)
			return -(( nStartFromDOW+7 )-nTargetDOW );

		return ( nTargetDOW - nStartFromDOW );
	}

	static public long calcDiffInMillis(Date date1, Date date2)
	{
		long n1;

		Calendar calendar = Calendar.getInstance();

		calendar.setTime(date1);

		n1 = calendar.getTimeInMillis();

		calendar.setTime(date2);

		return( n1 - calendar.getTimeInMillis());
	}

	static public Date calcStartOfWeek(Date fromDate, int nWeekStartsOn,int nRelativeWeek)
	{
		if(fromDate == null)
			fromDate = new Date();

		Date
				startOfWeek = calcPriorDOW(fromDate,nWeekStartsOn),
				calcDate = calcAddWeeks(startOfWeek,nRelativeWeek);

		return calcDate;
	}

	static public int calcElapsedDays(Date date1, Date date2)
	{
		Calendar
				calendar1 = Calendar.getInstance(),
				calendar2 = Calendar.getInstance();

		calendar1.setTime(date1);
		calendar2.setTime(date2);

		long
				nMillisPerDay = ( 60*60*24*1000 ),
				nMillis1 = calendar1.getTimeInMillis(),
				nMillis2 = calendar2.getTimeInMillis(),
				nDays1 = ( nMillis1 / nMillisPerDay ),
				nDays2 = ( nMillis2 / nMillisPerDay );

		int nElapsedDays = (int)( nDays1 - nDays2 );

		return nElapsedDays;
	}

	static public String formatDate(Date date,String stFormat,TimeZone timeZone)
	{
		if(date == null)
			date = new Date();

		if(stFormat == null)
			stFormat = Tempus.TIMEAMPM;

		if(timeZone == null)
			timeZone = TimeZone.getDefault();

		SimpleDateFormat dateFormat = new SimpleDateFormat(stFormat);

		dateFormat.setTimeZone(timeZone);

		return dateFormat.format(date);
	}

	public String formatStamp(String stUtcStamp,String stFormat,String stTimeZone)
	{
		if(stUtcStamp == null)
			stUtcStamp = Tempus.utcStamp(null);

		if(stTimeZone == null)
			stTimeZone = TimeZone.getDefault().getID();

		if(stFormat == null)
			stFormat = Tempus.TIMEAMPM;

		return Tempus.formatUtcStamp(stUtcStamp, stFormat, TimeZone.getTimeZone(stTimeZone));
	}

	static public String formatUtcStamp(String stUtcStamp,String stFormat,TimeZone timeZone)
	{
		Date date = parseUtcStamp(stUtcStamp,null);

		if(stFormat == null)
			stFormat = Tempus.TIMEAMPM;

		if(timeZone == null)
			timeZone = TimeZone.getDefault();

		SimpleDateFormat dateFormat = new SimpleDateFormat(stFormat);

		dateFormat.setTimeZone(timeZone);

		String s1 = dateFormat.format(date);

		return s1;
	}

	static public Date parseFormatStamp(String stStampString,String stFormat,TimeZone timeZone)
	{
		Date date = null;

		SimpleDateFormat dateFormat = new SimpleDateFormat(stFormat);

		if(timeZone == null)
			timeZone = TimeZone.getTimeZone("UTC");

		dateFormat.setTimeZone(timeZone);

		try
		{
			date = dateFormat.parse(stStampString);
		}
		catch (ParseException e)
		{
			e.printStackTrace();
		}
		return date;
	}

	static public Date parseDateStamp(String stDateStamp,TimeZone timeZone)
	{
		return parseFormatStamp(stDateStamp,DATESTAMP,timeZone);
	}

	static public Date parseUtcStamp(String stUTC,TimeZone timeZone)
	{
		return parseFormatStamp(stUTC,UTCSTAMP,timeZone);
	}

	static public String tzStamp()
	{
		return formatDate(new Date(), TZSTAMP, TimeZone.getDefault());
	}

	static public String tzString(TimeZone timeZone)
	{
		if(timeZone == null)
			timeZone = TimeZone.getDefault();

		return timeZone.getDisplayName();
	}

	static public String utcStamp(Date date)
	{
		if(date == null)
			date = new Date();

		return formatDate(date,UTCSTAMP, TimeZone.getTimeZone("UTC"));
	}
}
