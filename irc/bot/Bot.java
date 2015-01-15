package irc.bot;

import java.io.*;
import java.util.HashMap;
import java.util.*;
import irc.bot.handlers.*;

/**
 * A Class that represents an IRC Bot that can have multiple connections.
 * 
 * @author Nils Blattner
 * 
 */
public class Bot {
	/**
	 * Constructor
	 */
	Bot() {
		loadFiles();
	}

	/**
	 * The default settings file.
	 */
	public static final String SETTINGS_FILE = "settings.xml";
	
	/**
	 * The default users file.
	 */
	public static final String DEFAULT_USERS_FILE = "users.xml";
	
	/**
	 * The default connections file.
	 */
	public static final String DEFAULT_CONNECTIONS_FILE = "connections.xml";
	
	/**
	 * The currently open connections.
	 */
	private HashMap<String, Connection> connections = new HashMap<String, Connection>();
	
	/**
	 * The currently open threads, one for each connection.
	 */
	private HashMap<String, Thread> threads = new HashMap<String, Thread>();
	
	/**
	 * The users registered for the bot.
	 */
	private HashMap<String, User> users;
	
	/**
	 * The user groups.
	 */
	private HashMap<String, User> groups;
	
	/**
	 * The config files.
	 */
	private HashMap<String, String> files;

	/**
	 * Connect to an irc server.
	 * 
	 * @param handle The name to store the connection under.
	 * @param address Domain of the server.
	 * @param port Port of the server.
	 * @param nick Nick the bot should use.
	 * @return The connected Connection object.
	 */
	public Connection connect(String handle, String address, int port,
			String nick) {
		return connect(handle, address, port, nick, true, true);
	}

	/**
	 * Connect to an irc server.
	 * 
	 * @param handle The name to store the connection under.
	 * @param address Domain of the server.
	 * @param port Port of the server.
	 * @param nick Nick the bot should use.
	 * @param keepalive Whether or not to keep the connection alive (e.g. reconnect). 
	 * @param defaulthandlers Whether or not to insert the default handlers.
	 * @return The connected Connection object.
	 */
	public Connection connect(String handle, String address, int port,
			String nick, boolean keepalive, boolean defaulthandlers) {
		Connection c = getConnection(handle);
		if (c != null && c.isConnected())
			return c;
		Connection con = new Connection(handle, address, port, nick, keepalive);
		con.setParent(this);
		connections.put(handle, con);
		if (defaulthandlers)
			useDefaultHandlers(con);
		Thread t = new Thread(con);
		threads.put(handle, t);
		t.start();
		return con;
	}

	/**
	 * Disconnect from a server by the handle.
	 * 
	 * @param handle The connections handle.
	 */
	public void disconnect(String handle) {
		getConnection(handle).interrupt();
	}

	/**
	 * Returns a connection by it's handle.
	 * 
	 * @param handle The connection handle.
	 * @return The connection.
	 */
	public Connection getConnection(String handle) {
		return connections.get(handle);
	}

	/**
	 * Injects the default handlers to a connection.
	 * 
	 * @param con The connection to inject the default handlers to.
	 */
	public void useDefaultHandlers(Connection con) {
		MessageHandler mh = new MessageHandler();
		mh.defaultHelp();
		MessageCommand mc = new LoginCommand("Login");
		mh.addCommand("login", mc);
		mh.addAlias("auth", "login");
		mc = new ListUsersCommand("ListUsers");
		mh.addCommand("listusers", mc);
		mh.addAlias("lu", "listusers");
		con.addMessageHandler(mh);
	}

	/**
	 * @param handle
	 */
	public void useDefaultHandlers(String handle) {
		Connection c = connections.get(handle);
		if (c != null)
			useDefaultHandlers(c);
	}

	/**
	 * @return
	 */
	public HashMap<String, User> getGroups() {
		return groups;
	}

	/**
	 * @return
	 */
	public HashMap<String, User> getUsers() {
		return users;
	}

	/**
	 * @param name
	 * @return
	 */
	public User getUser(String name) {
		return users.get(name);
	}

	/**
	 * @param name
	 * @return
	 */
	public User getGroup(String name) {
		return groups.get(name);
	}

	/**
	 * @param handle
	 * @return
	 */
	public String getFile(String handle) {
		return files.get(handle);
	}

	/**
	 * @param handle
	 * @param file
	 */
	public void setFile(String handle, String file) {
		files.put(handle, file);
	}

	/**
	 * 
	 */
	public void loadFiles() {
		parseSettings();
		parseUsers();
		parseConnections();
	}

	/**
	 * 
	 */
	public void saveFiles() {
		// writeSettings();
		writeUsers();
		writeConnections();
	}

	/**
	 * 
	 */
	public void parseUsers() {
		String file = getFile("users");
		if (file == null || file.isEmpty())
			file = DEFAULT_USERS_FILE;
		UserParser parser = new UserParser(file);
		users = parser.getUsers();
		groups = parser.getGroups();
	}

	/**
	 * 
	 */
	public void writeUsers() {
		PrintStream p = null;
		String file = getFile("users");
		if (file == null || file.isEmpty())
			file = DEFAULT_USERS_FILE;
		try {
			p = new PrintStream(new FileOutputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (p == null)
			return;
		String indent = "    ";
		p.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<access>\n"
				+ indent + "<groups>\n");
		for (User g : groups.values()) {
			p.print(g.toXML(indent + indent, indent, false));
		}
		p.print(indent + "</groups>\n" + indent + "<users>\n");
		for (User g : users.values()) {
			p.print(g.toXML(indent + indent, indent, true));
		}
		p.print(indent + "</users>\n</access>");
	}

	/**
	 * 
	 */
	public void parseSettings() {
		SettingsParser parser = new SettingsParser(SETTINGS_FILE);
		files = parser.getFiles();
	}

	/**
	 * 
	 */
	public void parseConnections() {
		String file = getFile("connections");
		if (file == null || file.isEmpty())
			file = DEFAULT_CONNECTIONS_FILE;
		new ConnectionsParser(this, file);
	}

	/**
	 * 
	 */
	public void writeConnections() {
		PrintStream p = null;
		String file = getFile("connections");
		if (file == null || file.isEmpty())
			file = DEFAULT_CONNECTIONS_FILE;
		try {
			p = new PrintStream(new FileOutputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
		p.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<connections>\n");
		Connection[] ca = new Connection[connections.size()];
		connections.values().toArray(ca);
		/**
		 * @author nb
		 *
		 */
		class ConComp implements Comparator<Connection> {
			@Override
			public int compare(Connection c1, Connection c2) {
				return c1.getHandle().compareTo(c2.getHandle());
			}
		}
		Arrays.sort(ca, new ConComp());
		for (Connection c : ca) {
			p.print(c.toXML("    ", "    "));
		}
		p.print("</connections>");
	}
}
