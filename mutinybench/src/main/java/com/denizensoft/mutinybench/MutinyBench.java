package com.denizensoft.mutinybench;

import com.denizensoft.mutinyxml.MutinyElement;
import com.denizensoft.mutinyxml.MutinyXml;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class MutinyBench
{
	static public Pattern pattern = Pattern.compile("\\#(\\w+)");

	public static void traverseNodeDOM(Node node, int nDepth)
	{
		if(node != null)
		{
			if(node.getNodeType() == Node.TEXT_NODE)
			{
				System.out.printf("%s%s\n", StringUtils.repeat("\t", nDepth), node.getNodeValue());
			}
			else
			{
				Node nameNode = node.getAttributes().getNamedItem("name");

				if(nameNode != null)
					System.out.printf("%s<%s:%s>\n", StringUtils.repeat("\t", nDepth), node.getNodeName(), nameNode.getNodeValue());
				else
					System.out.printf("%s<%s>\n", StringUtils.repeat("\t", nDepth), node.getNodeName());

				NodeList nodeList = node.getChildNodes();

				for(int i = 0; i < nodeList.getLength(); ++i)
					traverseNodeDOM(nodeList.item(i), nDepth + 1);

				System.out.printf("%s</%s>\n", StringUtils.repeat("\t", nDepth), node.getNodeName());
			}
		}
	}

	public static void parseWithDOM(String[] args)
	{
		File f1 = new File(args[1]);

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

		try
		{
			DocumentBuilder documentBuilder = domFactory.newDocumentBuilder();

			Document document = documentBuilder.parse(f1);

			Element root = document.getDocumentElement();

			System.out.print(String.format("%sEnter: %s\n", StringUtils.repeat("\t", 0), root.getNodeName()));

			for(int i = 0; i < root.getChildNodes().getLength(); ++i)
			{
				traverseNodeDOM(root.getChildNodes().item(i), 1);
			}

			System.out.print(String.format("%s Exit: %s\n", StringUtils.repeat("\t", 0), root.getNodeName()));
		}
		catch(ParserConfigurationException e)
		{
			e.printStackTrace();
		}
		catch(FileNotFoundException e)
		{
			System.out.print(String.format("Couldn't open input file: %s\n", f1.getAbsolutePath()));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		catch(SAXParseException e)
		{
			System.out.print(String.format("XML Parser: %d:%d - %s\n", e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
		}
		catch(SAXException e)
		{
			System.out.print(String.format("XML exception: %s\n", e.getMessage()));
		}
	}

	public static void traverseNodePULL(XmlPullParser parser, int nDepth)
	{
	}

	public static Map<String,String> parseAttributeMapPull(XmlPullParser parser)
	{
		int n = parser.getAttributeCount();

		if(n > 0)
		{
			HashMap<String,String> hashMap = new HashMap<String,String>();

			for(int i=0; i<n; ++i)
			{
				String s1=parser.getAttributeName(i), s2 = parser.getAttributeValue(i), s3 = parser.getAttributePrefix(i);

				hashMap.put(s1,s2);
			}
			return hashMap;
		}

		return null;
	}

	public static void parseWithPULL(XmlPullParser parser, int nDepth)
	{
		try
		{
			int nEventType = 0;

			while(( nEventType = parser.getEventType()) != XmlPullParser.END_DOCUMENT)
			{
				System.out.printf("%s", StringUtils.repeat("\t", parser.getDepth()));

				switch(nEventType)
				{
					case XmlPullParser.END_DOCUMENT :
					{
						System.out.printf("----> EOD <----\n");
					}
					break;

					case XmlPullParser.START_DOCUMENT :
					{
						System.out.printf("----> SOD <----\n");
					}
					break;

					case XmlPullParser.START_TAG:
					{
						System.out.printf("<%s>\n", parser.getName());

						Map<String,String> map = parseAttributeMapPull(parser);

						for(Map.Entry<String,String> entry: map.entrySet())
						{
							System.out.printf("%s%s - %s\n", StringUtils.repeat("\t", parser.getDepth()+1),
									entry.getKey(),entry.getValue());
						}
					}
					break;

					case XmlPullParser.END_TAG:
					{
						System.out.printf("</%s>\n", parser.getName());
					}
					break;

					case XmlPullParser.TEXT:
					{
						System.out.printf("\tTEXT: %s\n", parser.getText());
					}
					break;

				}

				parser.next();
			}
		}
		catch(XmlPullParserException e)
		{

			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		switch(args[0].toLowerCase())
		{
			case "dom":
			{
				parseWithDOM(args);
			}
			break;

			case "pull":
			{
				try
				{
					MXParser mxp = new MXParser();

					mxp.setInput(new FileReader(args[1]));

					parseWithPULL(mxp,0);
				}
				catch(XmlPullParserException e)
				{

					e.printStackTrace();
				}
				catch(FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
			break;

			case "mutiny" :
			{
				try
				{
					MutinyElement element = MutinyXml.parseFile(args[1]);

					System.out.printf("Element name: %s\n",element.attribute("name"));
				}
				catch(FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
}
