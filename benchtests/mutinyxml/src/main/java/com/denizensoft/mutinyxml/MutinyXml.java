package com.denizensoft.mutinyxml;

import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by sjm on 9/18/15.
 */
public class MutinyXml
{
	public static MutinyElement parseReader(Reader reader)
	{
		MutinyElement element = null;

		MXParser parser = new MXParser();

		try
		{
			parser.setInput(reader);

			if(parser.getEventType() != XmlPullParser.START_DOCUMENT)
				throw new RuntimeException("MutinyXml - Expected SOD in parseReader()", null);

			if(parser.next() == XmlPullParser.START_TAG)
				element = new MutinyElement(null,parser);
		}
		catch(XmlPullParserException e)
		{
			throw new RuntimeException("MutinyXml.Element parser exception!", e);
		}
		catch(IOException e)
		{
			throw new RuntimeException("MutinyXml.Element io exception!", e);
		}
		return element;
	}

	public static MutinyElement parseFile(String stFileSpec) throws FileNotFoundException
	{
		return parseReader(new FileReader(stFileSpec));
	}
}
