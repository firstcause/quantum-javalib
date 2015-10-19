package com.denizensoft.mutinybench;

import com.denizensoft.mutinyxml.MutinyDocument;
import com.denizensoft.mutinyxml.MutinyElement;
import com.denizensoft.mutinyxml.MutinyXml;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.regex.Pattern;

public class MutinyBench
{
	private static class AppRequester implements MutinyElement.Requester
	{
		@Override
		public Reader openFileReader(String stFileSpec)
		{
			Reader reader = null;

			String s1 = String.format("./%s",stFileSpec);

			try
			{
				InputStream fis = new FileInputStream(s1);

				reader = new BufferedReader(new InputStreamReader(fis));
			}
			catch(FileNotFoundException e)
			{
				throw new RuntimeException(String.format("Mutiny: couldn't open requested file: %s",stFileSpec));
			}
			return reader;
		}

		@Override
		public String invokeRequest(MutinyElement element,String stRequest)
		{
			System.out.printf("Mutiny request received, context element: %s, request: %s\r\n",
					element.elementPath(),stRequest);

			return "OK";
		}
	}

	static public Pattern pattern = Pattern.compile("\\#(\\w+)");

	static AppRequester appRequester = new AppRequester();

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

	public static void main(String[] args)
	{
		switch(args[0].toLowerCase())
		{
			case "dom":
			{
				parseWithDOM(args);
			}
			break;

			case "mutiny" :
			{
				MutinyXml mutinyLoader = new MutinyXml(appRequester);

				MutinyDocument mutinyDocument = mutinyLoader.loadDocument(args[1]);

				mutinyDocument.exec(args);
			}
			break;
		}
	}
}
