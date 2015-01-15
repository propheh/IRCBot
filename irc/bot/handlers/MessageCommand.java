package irc.bot.handlers;

import java.util.regex.Pattern;
import irc.bot.*;

public class MessageCommand {
	public MessageCommand(String name, String desc) {
		this.name = name;
		this.description = desc;
	}
	public MessageCommand(String name) {
		this(name, "Some easyCommandLine Command");
	}
	
	public String name, description;
	protected boolean chan = true, priv = true; // overwrite those if you want to make them invisible in chan/priv space
	protected String accessType = "";
	protected int minLvl = 0;
	transient public Pattern pattern;
	public String arguments() { return ""; };
	public int call(Connection con, String source, String target, boolean inchan, String[] args) throws Exception, MessageCommandException {
		return 0;
	}
	public boolean match_args(String str) {
		return false;
	}
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
	public boolean isChanCommand() {
		return chan;
	}
	public void setChanCommand(boolean chan) {
		this.chan = chan;
	}
	public boolean isPrivCommand() {
		return priv;
	}
	public void setPrivCommand(boolean priv) {
		this.priv = priv;
	}
	public String getAccessType() {
		return accessType;
	}
	public int getMinLvl() {
		return minLvl;
	}
}
