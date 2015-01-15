package irc.bot;

import java.io.*;
import java.net.Socket;
import java.util.regex.*;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Date;
import static irc.bot.Constants.*;

//import java.util.Map.Entry;

public class Connection implements Runnable {
	Connection(String handle, String address, int port, String nick) {
		this(handle, address, port, nick, true);
	}
	Connection(String handle, String address, int port, String nick, boolean keepalive) {
		this.handle = handle;
		this.address = address;
		this.port = port;
		this.nick = nick;
		this.originalnick = nick;
		this.keepalive = keepalive;
		logfile = createLogFile();
	}
	
	// connection stuff
	private String handle, address, nick, originalnick, date;
	private int port;
	private long reconnectDelay = 10000; // 10s = default
	private boolean keepalive, connected = false;
	private Socket connection;
	private Bot parent;
	private LineNumberReader lnr;
	private PrintStream out;
	private TreeMap<String, Channel> channels = new TreeMap<String, Channel>();
	private TreeMap<String, Nick> nicks = new TreeMap<String, Nick>();
	private HashMap<String, Object> variables = new HashMap<String, Object>();
	
	// handlers
	private TreeMap<String, Handler> messageHandlers = new TreeMap<String, Handler>();
	private int messageHandlerC = 0;
	private TreeMap<String, Handler> actionHandlers = new TreeMap<String, Handler>();
	private int actionHandlerC = 0;
	private TreeMap<String, Handler> CTCPHandlers = new TreeMap<String, Handler>();
	private int CTCPHandlerC = 0;
	private TreeMap<String, Handler> CTCPReplyHandlers = new TreeMap<String, Handler>();
	private int CTCPReplyHandlerC = 0;
	private TreeMap<String, Handler> noticeHandlers = new TreeMap<String, Handler>();
	private int noticeHandlerC = 0;
	private TreeMap<String, Handler> snoticeHandlers = new TreeMap<String, Handler>();
	private int snoticeHandlerC = 0;
	
	private static Pattern nickcheck = Pattern.compile("^:?.*!.*@.*$"); // :nick!ident@address
	private static Pattern pointcheck = Pattern.compile("\\."); // is a point in the string?
	private static Pattern chancheck = Pattern.compile("^[#&]{1}.*$");
	//private static Pattern splitter = Pattern.compile("!");
	private PrintStream logfile;
	//private InHandler inhandler = new InHandler(this);
	
	public void run() {
		boolean run = true; // ensures that its run at least once
		//Thread inThread;
		//Pattern pattern = Pattern.compile(" +");
		while (run) {
			connected = false;
			connection = null;
			log("Connecting...");
			try {
				connection = new Socket(address, port);
				lnr = new LineNumberReader(new InputStreamReader(connection.getInputStream()));
				out = new PrintStream(connection.getOutputStream());
			}
			catch (Exception e) { logError("Unable to Connect ("+e.getMessage()+")"); }
			if (connection == null) run = false;
			else log("Connecting to: "+address+":"+port);
			/*inThread = new Thread(inhandler);
			inThread.start();*/
			String line = null;
			int numeric = 0;
			boolean init = true;
			// init
			if (run) {
				println("NICK "+getMe());
				println("USER IRCBot@ 8 * :Proph's IRCBot v"+getVersion());
			}
			//System.out.println("test");
			while (run && init) { // server is sending us some shit like chanmodes and motd, but we dont care
				line = "";
				try { line = lnr.readLine(); } catch (IOException e) {
					logError("Couldn't read from socket ("+e.getMessage()+")");
					run = false; break;
				}
				if (line == null) { run = false; break; }
				log("<< "+line);
				String[] args = line.split(" ");
				if (args.length > 0) {
					if (EVT_PING.equalsIgnoreCase(args[0])) {
						String pong = "PONG ";
						if (args.length > 1) { pong += line.substring(5); } // remove "PING " from the line
						println(pong);
					}
					else {
						numeric = -1;
						try { numeric = Integer.parseInt(args[1]); } catch (Exception e) {}
						switch (numeric) {
							case(ERR_NICKNAMEINUSE) : // Bad Nick
								nick = getRandMe(4); println("NICK "+nick); break;
							case(ERR_NOMOTD) : // motd not found -> connected
								init = false; break;
							case(RPL_ENDOFMOTD) : // motd finished -> connected
								init = false; break;
							case(-1) : // string command
								if (EVT_NICK.equalsIgnoreCase(args[1])) { // server aknowledges our nick
									String oldnick = getNickofAddress(args[0]);
									if (getMe().equals(oldnick)) {
										nick = stripdd(args[2]);
									} // we are not yet connected, so we dont care about other nickchanges
								}
							
						}
					}
				}
			}
			if (run) {
				connected = true;
				nicks.clear();
				//channels.clear();
				for (Channel ch : channels.values()) {
					println("JOIN "+ch.getName());
				}
				
				// we are connected
			}
			while (run) {
				line = "";
				try { line = lnr.readLine(); }
				catch (IOException e) { logError("Couldn't read from socket ("+e.getMessage()+")"); }
				if (line == null || line.isEmpty()) { run = false; break; }
				log("<< "+line);
				String[] args = line.split(" ");
				if (args.length > 0) {
					if (EVT_PING.equalsIgnoreCase(args[0])) {
						String pong = "PONG";
						if (args.length > 1) { pong += line.substring(4); } // remove "PING " from the line
						println(pong);
					}
					else {
						numeric = -1;
						try { numeric = Integer.parseInt(args[1]); } catch (Exception e) {}
						if (numeric == -1) { // isnt numeric -> must be a string
							if (EVT_NICK.equalsIgnoreCase(args[1])) { // server aknowledges our nick
								handleNick(stripdd(args[0]), stripdd(args[2]));
							}
							else if (EVT_JOIN.equalsIgnoreCase(args[1])) {
								handleJoin(stripdd(args[0]), stripdd(args[2]));
							}
							else if (EVT_KICK.equalsIgnoreCase(args[1])) {
								handleKick(stripdd(args[0]), stripdd(args[2]), stripdd(args[3])); // sender KICK channel kickednick
							}
							else if (EVT_PART.equalsIgnoreCase(args[1])) {
								handlePart(stripdd(args[0]), stripdd(args[2]));
							}
							else if (EVT_PRIVMSG.equalsIgnoreCase(args[1])) {
								handlePrivmsg(args, stripdd(line));
							}
							else if (EVT_NOTICE.equalsIgnoreCase(args[1])) {
								handleNotice(args, stripdd(line));
							}
							else if (EVT_MODE.equalsIgnoreCase(args[1])) {
								handleMode(stripdd(args[0]), args[2], args); // << :Proph!proph@pro-B5406383 MODE #uebsh +v Proph
							}
						}
						else {
							switch (numeric) {
								case (RPL_NAMREPLY):
									String[] nicks;
									if (args.length < 5) break;
									else if (args.length == 5) break; // empty names = we are not on chan and all nicks on there are invisible = we #care
									else {
										nicks = new String[args.length - 5];
										nicks[0] = stripdd(args[5]);
										if (args.length > 6) {
											for (int i = 6; i < args.length; i++) nicks[i - 5] = args[i];
										}
									}
									handleNamesReply(args[4], nicks);
									break;
							}
						}
					}
				}
			}
			
			if (connection != null && !connection.isClosed()) {
				try { connection.close(); }
				catch (Exception e) { logError("Couldnt close connection ("+e.getMessage()+")"); }
			}
			log("Disconnected");
			connected = false;
			nicks.clear();
			//channels.clear();
			for (Channel ch : channels.values()) { ch.clear(); }
			run = keepalive;
			if (run) {
				try { Thread.sleep(reconnectDelay); } catch (Exception e) {} // wait to retry
				log("Reconnecting...");
			}
		}
	}
	
	// raw handlers
	private void handleJoin(String nick, String channel) {
		//System.out.println("JOIN_EVT: "+nick+" joins "+channel+" (Me == "+getMe()+")");
		String[] split = splitAddress(nick);
		if (split[0].equalsIgnoreCase(getMe())) { // we joined a channel
			Channel tchan = getChannel(channel);
			if (tchan == null) channels.put(channel, new Channel(channel));
			else tchan.clear();
		}
		else {
			Nick tnick = getNick(split[0]);
			Channel tchan = getChannel(channel);
			if (tchan != null) { // if this returns null we've got a problem ;P
				if (tnick == null) { // new nick
					if (split.length >= 3) tnick = new Nick(split[0], split[1], split[2]);
					else tnick = new Nick(split[0]);
					nicks.put(tnick.getName(), tnick);
				}
				tnick.join(tchan);
			} else System.err.println("Error: Couldn't find Channel "+channel); // we get nickchanges from nicks that arent on our channels
		}
	}
	private void handleNick(String oldnick, String newnick) {
		String[] split = splitAddress(oldnick);
		if (split[0].equals(nick)) {
			nick = newnick;
		}
		else {
			Nick nick = getNick(split[0]);
			if (nick == null) { // nick must've evaded us somehow, lets add him
				if (split.length == 3) nick = new Nick(newnick, split[1], split[2]);
				else nick = new Nick(newnick);
			}
			else {
				nicks.remove(split[0]);
				nicks.put(newnick, nick);
				nick.setName(newnick);
			}
		}
	}
	private void handleKick(String nick, String channel, String knick) {
		Channel c = channels.get(channel);
		if (c != null) {
			if (getMe().equals(knick)) {
				cleanupChannel(c);
				join(c.getName());
			}
			else {
				Nick n = nicks.get(knick);
				if (n != null) {
					n.part(c);
					if (n.channelCount() == 0) nicks.remove(knick); // nick left all the channels we are on
				}
			}
		}
	}
	private void handlePart(String nick, String channel) {
		nick = getNickofAddress(nick);
		Channel c = getChannel(channel);
		if (c != null) {
			if (getMe().equals(nick)) {
				cleanupChannel(c);
				
			}
			else {
				Nick n = nicks.get(nick);
				if (n != null) {
					n.part(c);
					if (n.channelCount() == 0) nicks.remove(nick); // nick left all the channels we are on
				}
			}
		}
	}
	private void handleMode(String nick, String target, String[] args) {
		if (isChan(target)) {
			//nick = getNickofAddress(nick);
			Channel c = getChannel(target);
			//Nick n = getNick(nick);
			if (args.length < 4 || c == null) return; // no +o-v part
			String modes = "";
			for (int i = 3; i < args.length; i++) modes += args[i]+" ";
			processChanModes(c, modes);
		}
	}
	private void handlePrivmsg(String[] args, String line) {
		int words = args.length;
		if (words < 4) { System.err.println("Error: Empty PRIVMSG!"); return; }
		String[] parts;
		String last = args[words - 1];
		if ((char)1 == last.charAt(last.length() - 1)) { // 
			parts = line.split(""+(char)1);
			if ((':'+(char)1+"ACTION").equalsIgnoreCase(args[3])) {
				handleAction(args[0], args[2], parts[1].substring(6));
				return;
			}
			else if ((":"+(char)1).equals(args[3].substring(0, 2))) {
				handleCTCP(args[0], args[2], parts[1]);
				return;
			}
		}
		String msg = stripdd(args[3]);
		if (words > 4) {
			for (int i = 4; i < words; i++) {
				msg += " "+args[i];
			}
		}
		handleMessage(stripdd(args[0]), stripdd(args[2]), msg);
	}
	private void handleNotice(String[] args, String line) {
		int words = args.length;
		if (words < 4) { System.err.println("Error: Empty NOTICE!"); return; }
		String last = args[words - 1];
		if ((char)1 == last.charAt(last.length() - 1)) { // 
			String[] parts = line.split(""+(char)1);
			if ((":"+(char)1).equals(args[3].substring(0, 2))) {
				handleCTCPReply(args[0], args[2], parts[1]);
				return;
			}
		}
		String msg = stripdd(args[3]);
		if (words > 4) {
			for (int i = 4; i < words; i++) {
				msg += " "+args[i];
			}
		}
		if (pointcheck.matcher(getNickofAddress(args[0])).matches()) handleSNotice(args[0], args[2], msg);
		else handleNotice(stripdd(args[0]), stripdd(args[2]), msg);
	}
	private void handleNamesReply(String channel, String[] nicks) {
		Channel chan = getChannel(channel);
		String[] split;
		ChanNick cn;
		Nick ni;
		if (chan == null) {
			chan = new Channel(channel);
			channels.put(channel, chan);
		}
		for (String n : nicks) {
			split = splitModes(n);
			if (!getMe().equalsIgnoreCase(split[1])) {
				ni = getNick(split[1]);
				if (ni == null) {
					ni = new Nick(split[1]);
					this.nicks.put(ni.getName(), ni);
				}
				cn = ni.join(chan);
				cn.setModes(split[0]);
			}
		}
	}
	
	// abstracted handlers
	private void handleMessage(String sender, String receiver, String msg) {
		String[] sa = msg.split(" ");
		if (sa[0].equalsIgnoreCase("join")) {
			if (sa.length > 1) {
				String cs = "";
				for (int i = 1; i < sa.length; i++) {
					cs += fixChannel(sa[i]);
					if (i-1 != sa.length) cs += ", ";
				}
				println("JOIN "+cs);
			}
		}
		else if (sa[0].equalsIgnoreCase("list") && (receiver.charAt(0) == '#')) {
			Channel c = getChannel(receiver);
			
			if (c != null) {
				println("PRIVMSG "+receiver+" :Amount of Nicks present: "+c.nickCount());
				for (ChanNick n : c.getNicks()) {
					Nick ni = n.getNick();
					println("PRIVMSG "+receiver+" :User: "+ni);
				}
			}
			else System.out.println("null");
		}
		callHandlers(messageHandlers, sender, receiver, msg);
	}
	private void handleAction(String sender, String receiver, String msg) {
		callHandlers(actionHandlers, sender, receiver, msg);
	}
	private void handleCTCP(String sender, String receiver, String msg) {
		callHandlers(CTCPHandlers, sender, receiver, msg);
	}
	private void handleCTCPReply(String sender, String receiver, String msg) {
		callHandlers(CTCPReplyHandlers, sender, receiver, msg);
	}
	private void handleNotice(String sender, String receiver, String msg) {
		callHandlers(noticeHandlers, sender, receiver, msg);
	}
	private void handleSNotice(String sender, String receiver, String msg) {
		callHandlers(snoticeHandlers, sender, receiver, msg);
	}
	
	private void callHandlers(TreeMap<String, Handler> handlers, String sender, String receiver, String msg) {
		for (Handler h : handlers.values()) {
			h.handle(this, sender, receiver, msg);
		}
	}
	
	// add diffrent handlers
	public void addMessageHandler(Handler handler) {
		messageHandlers.put(""+messageHandlerC, handler);
		messageHandlerC++;
	}
	public void addActionHandler(Handler handler) {
		actionHandlers.put(""+actionHandlerC, handler);
		actionHandlerC++;
	}
	public void addCTCPHandler(Handler handler) {
		CTCPHandlers.put(""+CTCPHandlerC, handler);
		CTCPHandlerC++;
	}
	public void addCTCPReplyHandler(Handler handler) {
		CTCPReplyHandlers.put(""+CTCPReplyHandlerC, handler);
		CTCPReplyHandlerC++;
	}
	public void addNoticeHandler(Handler handler) {
		noticeHandlers.put(""+noticeHandlerC, handler);
		noticeHandlerC++;
	}
	public void addSNoticeHandler(Handler handler) {
		snoticeHandlers.put(""+snoticeHandlerC, handler);
		snoticeHandlerC++;
	}
	
	// send methods return 0 if failed or 1 if successful
	public int sendRaw(String raw) {
		if (!connected) return 0;
		println(raw);
		return 1;
	}
	public int sendMessage(String target, String msg) {
		return sendRaw(EVT_PRIVMSG+" "+target+" :"+msg);
	}
	public int sendAction(String target, String msg) {
		return sendMessage(target, (char)1+"ACTION"+msg+(char)1);
	}
	public int sendCTCP(String target, String msg) {
		return sendMessage(target, (char)1+msg+(char)1);
	}
	
	public int sendNotice(String target, String msg) {
		return sendRaw(EVT_NOTICE+" "+target+" :"+msg);
	}
	public int sendCTCPReply(String target, String msg) {
		return sendNotice(target, (char)1+msg+(char)1);
	}

	private PrintStream createLogFile() {
		PrintStream logfile = null;
		date = getDate();
		File f = new File("logs/"+handle+"/"+address+"."+port+"."+date+".log");
		File d = new File("logs/"+handle+"/");
		if (!d.exists()) d.mkdirs();
		if (!f.exists()) {
			try { f.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
		}
		try {
			logfile = new PrintStream(new FileOutputStream(f, true));
		} catch (Exception e) { e.printStackTrace(); }
		return logfile;
	}
	public void log(String msg) {
		if (logfile == null) logfile = createLogFile();
		else if (!getDate().equals(date)) logfile = createLogFile();
		if (logfile != null) logfile.println(getTimeStamp()+" "+msg);
	}
	public void logError(String msg) {
		log("Error: "+msg);
	}
	
	private void cleanupChannel(Channel channel) {
		channel.leave();
		//channels.remove(channel.getName());
	}
	
	// manipulate the connection
	public void interrupt() { // abort connection and exit it
		keepalive = false;
		//inhandler.stop();
		try { connection.close(); } catch (IOException e) { e.printStackTrace(); }
	}
	
	public void reconnect() { // reconnect if keepalive is true
		if (connection.isConnected()) {
			try { connection.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		// the loop will reconnect
	}
	
	// request commands
	public void changeNick(String newnick) {
		println("NICK "+newnick);
	}
	
	// join a channel
	public void join(String channel) {
		if (isConnected()) println("JOIN "+channel);
		else {
			Channel tchan = getChannel(channel);
			if (tchan == null) channels.put(channel, new Channel(channel)); // will be joined on connecting
		}
	}
	
	// getters
	public String getMe() { return nick; }
	
	public String getRandMe(int rndlen) { // returns value of nick with random number of rndlen length appended
		java.util.Random rnd = new java.util.Random();
		String rndstr = "";
		for (int i = 0; i < rndlen; i++) rndstr += rnd.nextInt(9);
		return nick+rndstr;
	}
	
	public Nick getNick(String nick) {
		 return nicks.get(nick);
	}
	
	public static String getNickofAddress(String address) { // gets nick from :nick!ident@adress
		return stripdd(address.split("!")[0]);
	}
	
	public Channel getChannel(String channel) {
		return channels.get(channel);
	}
	
	public static float getVersion() { return 0.1f; }

	public PrintStream getPrintStream() { return out; }
	
	public LineNumberReader getLineNumberReader() { return lnr; }
	
	// access to a hashmap that stores any objects that are wanted with the connection
	
	public Object getVar(String handle) {
		return variables.get(handle);
	}
	
	public void setVar(String handle, Object var) {
		variables.put(handle, var);
	}
	
	public static String getDate() {
		return getDate(new Date());
	}
	public static String getDate(Date time) {
		return String.format("%1$te.%1$tm.%1$ty", time);
	}
	public static String getTimeStamp() {
		return getTimeStamp(new Date());
	}
	public static String getTimeStamp(Date time) {
		return String.format("[%tT]", time);
	}
	
	public static String[] splitModes(String nick) { // takes @%+nick and returns [@%+, nick]
		char[] ma = nick.toCharArray();
		int len = ma.length;
		String modes = "";
		for (int i = 0; i < len; i++) {
			switch (ma[i]) {
				case('@'):
				case('%'):
				case('+'):
					modes += ma[i]; break;
				case(':'): break;
				default:
					return new String[]{modes, nick.substring(i)};
			}
		}
		return new String[]{modes, ""};
	}
	
	public void processChanModes(Channel chan, String modes) {
		String[] sa = modes.split(" ");
		char[] ca = sa[0].toCharArray();
		boolean add = true;
		int sc = 1;
		for (int i = 0; i < ca.length; i++) {
			ChanNick n;
			switch (ca[i]) {
				case ('+'):
					add = true; break;
				case ('-'):
					add = false; break;
				case ('o'):
					if (sc < sa.length) {
						if (getMe().equalsIgnoreCase(sa[sc])) chan.setOp(add);
						else if ((n = chan.getNick(sa[sc])) != null) n.setOp(add);
					}
					sc++; break;
				case ('v'):
					if (sc < sa.length) {
						if (getMe().equalsIgnoreCase(sa[sc])) chan.setVoice(add);
						else if ((n = chan.getNick(sa[sc])) != null) n.setVoice(add);
					}
					sc++; break;
				case ('h'):
					if (sc < sa.length) {
						if (getMe().equalsIgnoreCase(sa[sc])) chan.setHalfOp(add);
						else if ((n = chan.getNick(sa[sc])) != null) n.setHalfOp(add);
					}
					sc++; break;
				case ('k'):
					if (add) if (sc < sa.length) chan.setKey(sa[sc]);
					else chan.setKey("");
					sc++; // -k also has the key in the modes
					break;
				default:
					if (add) chan.addMode(ca[i]);
					else chan.delMode(ca[i]);
			}
		}
	}

	public boolean isConnected() { return connected; }
	public static boolean isChan(String chan) { return chancheck.matcher(chan).matches(); }
	
	public void println(String write) {
		if ((connection != null) && (connection.isConnected())) {
			if (out != null) out.println(write);
			log(">> "+write);
		}
		else logError("Trying to write while disconnected! ("+write+")");
	}
	
	public static String stripdd(String in) { // strips a leading :
		if (in.charAt(0) == ':') return in.substring(1);
		else return in;
	}
	private static String[] splitAddress(String address) {
		address = stripdd(address); // strip leading :
		if (nickcheck.matcher(address).matches()) { // is it really nick!ident@address
			String[] str1 = address.split("!"), str2 = str1[1].split("@"); // split to [nick, [ident, address]]
			return new String[]{str1[0], str2[0], str2[1]};
		}
		else return new String[]{address};
	}
	private static String fixChannel(String chan) {
		if (chan.charAt(0) == '#') return chan;
		return '#'+chan;
	}
	public Bot getParent() {
		return parent;
	}
	public void setParent(Bot parent) {
		this.parent = parent;
	}
	public String getHandle() {
		return handle;
	}

	public long getReconnectDelay() {
		return reconnectDelay;
	}
	public void setReconnectDelay(long reconnectDelay) {
		this.reconnectDelay = reconnectDelay;
	}
	public String toXML(String indent, String it) {
		String xml = indent+"<connection name=\""+handle+"\" host=\""+address+"\" ";
		xml += "port=\""+port+"\" nick=\""+originalnick+"\">\n";
		xml += indent+it+"<channels>\n";
		for (Channel c : channels.values()) {
			xml += indent+it+it+"<entry name=\""+c.getName()+"\" />\n";
		}
		xml += indent+it+"</channels>\n";
		if (variables.size() > 0) {
			xml += indent+it+"<variables>\n";
			for (Entry<String, Object> e : variables.entrySet()) {
				xml += indent+it+it+"<entry name=\""+e.getKey()+"\">";
			}
			xml += indent+it+"</variables>\n";
		}
		xml += indent+"</connection>\n";
		return xml;
	}
	/*
	 * <connection name="Home1" host="localhost" port="6667" nick="IRCBot">
        <channels>
            <entry name="#opers" />
        </channels>
       </connection>
	 */
}
