package irc.bot.handlers;

import java.util.TreeMap;
import java.util.Map.Entry;

import irc.bot.*;

public class MessageHelpCommand extends MessageCommand {
	MessageHelpCommand(String name) { super(name, "Help command."); }
	MessageHelpCommand(String name, TreeMap<String, MessageCommand> mcs, TreeMap<String, String> mas) {
		super(name ,"Help command.");
		init(mcs, mas);
	}
	public void init(TreeMap<String, MessageCommand> mcs, TreeMap<String, String> mas) {
		this.mcs = mcs;
		this.mas = mas;
	}

	transient private TreeMap<String, MessageCommand> mcs;
	transient private TreeMap<String, String> mas;

	public String arguments() { return "(Command you want help for)"; };
	public boolean match_args(String str) {
		return true;
	}
	
	@Override
	public int call(Connection con, String source, String target, boolean inchan, String[] args) {
		if (args.length > 1) {
			MessageCommand c = mcs.get(args[1].toLowerCase());
			if (c == null) { sendNotice(con, source, "Error: No entry for "+args[1]+"!"); }
			else {
				printHelp(args[1], c, con, source, true);
			}
		}
		else {
			for(Entry<String, MessageCommand> tbc : mcs.entrySet()) {
				if (tbc != null) printHelp(tbc.getKey(), tbc.getValue(), con, source);
			}
		}
		return 1;
	}
	private void printHelp(String name, MessageCommand mc, Connection out, String target) {
		printHelp(name, mc, out, target, false);
	}
	private void printHelp(String name, MessageCommand mc, Connection out, String target, boolean force) {
		sendNotice(out, target, "Help for '"+name+"':");
		String s = mas.get(name.toLowerCase());
		if (s != null) {
			sendNotice(out, target, "  This is an alias of '"+s+"'.");
		}
		if ((s == null) || (force)) {
			sendNotice(out, target, "  "+mc.description);
			sendNotice(out, target, "  Expected Format: "+name+" "+mc.arguments());
		}
	}
}
