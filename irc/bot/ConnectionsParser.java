package irc.bot;

import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

public class ConnectionsParser {
	ConnectionsParser(Bot bot, String file) {
		this.bot = bot;
		parseConnections(file);
	}
	
	private class ConnectionHandler extends DefaultHandler {
		@Override
		public void startElement( String namespaceURI, String localName, String qName, Attributes atts ) throws SAXException {
			if ("connection".equalsIgnoreCase(qName)) {
				String name = "", host = "", nick = "";
				int port = 0, reconnectdelay = 10;
				for( int i = 0; i < atts.getLength(); i++ ) {
					String aqName = atts.getQName(i);
					if ("name".equalsIgnoreCase(aqName)) { name = atts.getValue(i); }
					else if ("host".equalsIgnoreCase(aqName)) { host = atts.getValue(i); }
					else if ("port".equalsIgnoreCase(aqName)) {
						port = Integer.parseInt(atts.getValue(i).trim());
					}
					else if ("reconnectdelay".equalsIgnoreCase(aqName)) {
						reconnectdelay = Integer.parseInt(atts.getValue(i).trim());
					}
					else if ("nick".equalsIgnoreCase(aqName)) { nick = atts.getValue(i); }
				}
				//System.out.println("<connection name=\""+name+"\" host=\""+host+"\" port=\""+port+"\" nick=\""+nick+"\"");
				if (!name.isEmpty() && (bot.getConnection(name) == null) && !host.isEmpty() && !nick.isEmpty() && (port > 0)) {
					cur = bot.connect(name, host, port, nick);
					cur.setReconnectDelay(reconnectdelay*1000L);
				}
			}
			else if ("channels".equalsIgnoreCase(qName)) {
				channel = true;
			}
			else if ("entry".equalsIgnoreCase(qName)) {
				if (channel && (cur != null)) {
					String name = "";
					for( int i = 0; i < atts.getLength(); i++ ) {
						if ("name".equalsIgnoreCase(atts.getQName(i))) { name = atts.getValue(i); }
					}
					if (!name.isEmpty()) cur.join(name);
				}
			}
			chars = "";
		}
		
		@Override
		public void characters( char[] ch, int start, int length ) {
			for(int i = start; i < (start + length); i++) chars += ch[i];
		}
		
		@Override
		public void endElement(String namespaceURI, String localName, String qName) {
			if ("connection".equalsIgnoreCase(qName)) cur = null;
			else if ("channels".equalsIgnoreCase(qName)) channel = false;
		}
	}
	
	private Bot bot;
	private Connection cur;
	private boolean channel;
	private String chars;
	
	public void parseConnections() { parseConnections("connections.xml"); }
	public void parseConnections(String file) {
		channel = false;
		cur = null;
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new File(file), new ConnectionHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
