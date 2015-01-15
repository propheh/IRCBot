package irc.bot;

import java.util.HashMap;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

public class UserParser {
	UserParser(String file) {
		parseUsers(file);
	}
	
	private class UserHandler extends DefaultHandler {
		@Override
		public void startElement( String namespaceURI, String localName, String qName, Attributes atts ) throws SAXException {
			if ("group".equalsIgnoreCase(qName)) {
				if (user) { // group entry of a user
					if (cur != null) {
						String name = "";
						for( int i = 0; i < atts.getLength(); i++ ) {
							if ("name".equalsIgnoreCase(atts.getQName(i))) { name = atts.getValue(i); }
						}
						User u = groups.get(name);
						if (u != null) { cur.addGroup(u); }
					}
				}
				else {
					String name = "";
					int defaultlvl = 0;
					for( int i = 0; i < atts.getLength(); i++ ) {
						if ("name".equalsIgnoreCase(atts.getQName(i))) { name = atts.getValue(i); }
						else if ("defaultlvl".equalsIgnoreCase(atts.getQName(i))) {
							defaultlvl = Integer.parseInt(atts.getValue(i).trim());
						}
					}
					cur = new User(name, defaultlvl);
					groups.put(name, cur);
				}
			}
			else if ("user".equalsIgnoreCase(qName)) {
				String name = "";
				int defaultlvl = 0;
				for( int i = 0; i < atts.getLength(); i++ ) {
					if ("name".equalsIgnoreCase(atts.getQName(i))) { name = atts.getValue(i); }
					else if ("defaultlvl".equalsIgnoreCase(atts.getQName(i))) {
						defaultlvl = Integer.parseInt(atts.getValue(i).trim());
					}
				}
				cur = new User(name, defaultlvl);
				users.put(name, cur);
				user = true;
			}
			else if ("entry".equalsIgnoreCase(qName)) {
				//System.out.println("cur = "+cur);
				if (cur != null) {
					String name = "";
					int lvl = 0;
					for( int i = 0; i < atts.getLength(); i++ ) {
						if ("name".equalsIgnoreCase(atts.getQName(i))) { name = atts.getValue(i); }
						else if ("lvl".equalsIgnoreCase(atts.getQName(i))) {
							lvl = Integer.parseInt(atts.getValue(i).trim());
						}
					}
					cur.addLvl(name, lvl);
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
			if ("user".equalsIgnoreCase(qName)) cur = null;
			if (!user && "group".equalsIgnoreCase(qName)) cur = null;
			if (user && cur != null) {
				if ("password".equalsIgnoreCase(qName)) { cur.setPass(chars); }
				else if (!"entry".equalsIgnoreCase(qName) && !"group".equalsIgnoreCase(qName)) {
					cur.setVar(qName, chars);
				}
			}
		}
	}
	
	private User cur;
	private boolean user;
	private String chars;
	private HashMap<String, User> users;
	private HashMap<String, User> groups;
	
	public void parseUsers() { parseUsers("users.xml"); }
	public void parseUsers(String file) {
		users = new HashMap<String, User>();
		groups = new HashMap<String, User>();
		user = false;
		cur = null;
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(new File(file), new UserHandler());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public HashMap<String, User> getUsers() {
		return users;
	}

	public HashMap<String, User> getGroups() {
		return groups;
	}
}
