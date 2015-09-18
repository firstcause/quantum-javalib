package com.denizensoft.mutinyxml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

/**
 * Created by sjm on 9/18/15.
 */
public class MutinyXml
{
	public static MutinyElement parseReader(Reader reader)
	{
		MutinyElement element = new MutinyElement(reader);

		return element;
	}

	public static MutinyElement parseFile(String stFileSpec) throws FileNotFoundException
	{
		return parseReader(new FileReader(stFileSpec));
	}
}
