package irc.bot.handlers;

import java.util.TreeMap;
import java.util.regex.Pattern;

import irc.bot.*;

public class MessageHandler implements Handler {
	public int handle(Connection con, String source, String target, String msg) {
		//String addr = source;
		String snick = Connection.getNickofAddress(source);
		Pattern p = Pattern.compile(" +");
		String[] nca = p.split(msg);
		boolean exec = false, chan = false, priv = false;
		if (Connection.isChan(target)) {
			if (msg.charAt(0) == chanprefix) { // we ignore chanmsgs that dont start with chanprefix (! is default)
				msg = msg.substring(1);
				nca[0] = nca[0].substring(1);
				chan = true;
			}
		}
		else priv = true;
		if (nca.length > 0) {
			MessageCommand c = coms.get(nca[0].toLowerCase());
			if (c == null) {
				sendNotice(con, snick, "Error: Unknown Command!");
				return 1;
			}
			if (chan) {
				if (c.isChanCommand()) exec = true;
				else sendNotice(con, snick, "Error: You cannot use this command in a channel!");
			}
			else if (priv) {
				if (c.isPrivCommand()) exec = true;
				else sendNotice(con, snick, "Error: You cannot use this command in a private message!");
			}
			if (exec) { // lets check for permissions
				User u = con.getNick(snick).getUser();
				int userlvl = 0;
				if (u != null) {
					userlvl = u.getDefaultLvl();
					if ((c.getAccessType() != null) && (!c.getAccessType().isEmpty())) userlvl = u.getLvl(c.getAccessType());
				}
				if (userlvl < c.getMinLvl()) {
					exec = false;
					sendNotice(con, snick, "Error: No permission to use this command!");
				}
			}
			if (exec) {
					if (c.match_args(msg)) {
						try {
							return c.call(con, snick, target, chan, nca);
						}
						catch (MessageCommandException e) { e.printStackTrace(); } // Command specific exception, ignored if its supposed to stop
						catch (Exception e) { e.printStackTrace(); } // general exception, ignored if its supposed to stop
					}
					else sendNotice(con, snick, "Error: Expected Format: "+nca[0]+" "+c.arguments());
			}
		}
		return 1;
	}
	
	
	private char chanprefix = '!';
    private TreeMap<String, MessageCommand> coms = new TreeMap<String, MessageCommand>();
	private TreeMap<String, String> aliases = new TreeMap<String, String>();
	public void addCommand(String name, MessageCommand mc) {
		coms.put(name.toLowerCase(), mc);
	}
	public void addAlias(String name, String main) {
		main = main.toLowerCase();
		MessageCommand eclc = coms.get(main);
		if (eclc != null) {
			addCommand(name, eclc);
			aliases.put(name.toLowerCase(), main);
		}
	}
	public MessageCommand getCommand(String name) { return coms.get(name.toLowerCase()); }
	public String getAlias(String name) { return aliases.get(name.toLowerCase()); }
	public static void println(Connection con, String write) {
		con.println(write);
	}
	public static void sendMessage(Connection con, String target, String msg) {
		con.sendMessage(target, msg);
	}
	public static void sendAction(Connection con, String target, String msg) {
		con.sendAction(target, msg);
	}
	public static void sendNotice(Connection con, String target, String msg) {
		con.sendNotice(target, msg);
	}
	public static void sendCTCP(Connection con, String target, String msg) {
		con.sendCTCP(target, msg);
	}
	public static void sendCTCPReply(Connection con, String target, String msg) {
		con.sendCTCPReply(target, msg);
	}
	public void initHelp(MessageHelpCommand mch) {
		mch.init(coms, aliases);
	}
	public void defaultHelp() {
		MessageHelpCommand mhc = new MessageHelpCommand("Help", coms, aliases);
		addCommand("help", mhc);
		addAlias("h", "help");
		addAlias("?", "help");
	}
	public char getChanprefix() {
		return chanprefix;
	}
	public void setChanprefix(char chanprefix) {
		this.chanprefix = chanprefix;
	}
}
