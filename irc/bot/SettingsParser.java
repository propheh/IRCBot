package irc.bot;

import java.io.File;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class SettingsParser {
	SettingsParser(String file) {
		parseSettings(file);
	}
	
	private class SettingsHandler extends DefaultHandler {
		@Override
		public void startElement( String namespaceURI, String localName, String qName, Attributes atts ) throws SAXException {
			if (qName.equalsIgnoreCase("files")) infile = true;
			chars = "";
		}
		
		@Override
		public void characters( char[] ch, int start, int length ) {
			for(int i = start; i < (start + length); i++) chars += ch[i];
		}
		
		@Override
		public void endElement(String namespaceURI, String localName, String qName) {
			if (qName.equalsIgnoreCase("files")) infile = false;
			
			if (infile) {
				files.put(qName, chars);
			}
		}
	}
	
	private String chars;
	private HashMap<String, String> files;
	private boolean infile;
	
	public HashMap<String, String> getFiles() {
		return files;
	}
	public void parseSettings() { parseSettings("settings.xml"); }
	public void parseSettings(String file) {
		files = new HashMap<String, String>();
		infile = false;
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new File(file), new SettingsHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
