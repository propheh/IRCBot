package irc.bot;

import java.util.TreeMap;
import java.util.regex.Pattern;

public class Nick {
	Nick(String name) { // name can be either nick or nick!ident@address
		if (nickcheck.matcher(name).matches()) {
			String[] sp1 = name.split("!"), sp2 = sp1[1].split("@");
			this.name = sp1[0];
			this.ident = sp2[0];
			this.host = sp2[1];
		}
		else {
			this.name = name;
			this.ident = "";
			this.host = "";
		}
	}
	Nick(String name, String ident, String host) {
		this.name = name;
		this.ident = ident;
		this.host = host;
	}
	
	private String name, ident, host;
	private User user;
	private TreeMap<String, ChanNick> channels = new TreeMap<String, ChanNick>();
	private static Pattern nickcheck = Pattern.compile("^:?.*!.*@.*$"); // :nick!ident@address
	
	public void quit() {
		for (ChanNick c : channels.values()) {
			c.getChannel().removeNick(this);
		}
	}
	public ChanNick join(Channel channel) {
		ChanNick nick = new ChanNick(channel, this);
		channels.put(channel.getName(), nick);
		channel.addNick(nick);
		return nick;
	}
	public void part(Channel channel) { part(channel, true); }
	public void part(Channel channel, boolean cascade) {
		channels.remove(channel.getName());
		if (cascade) channel.removeNick(this);
	}
	
	
	// setters
	public void setUser(User user) { this.user = user; }
	public void setName(String name) { if (name != null) this.name = name; }
	public void setIdent(String ident) { if (ident != null) this.ident = ident; }
	public void setHost(String host) { if (host != null) this.host = host; }
	
	// getters
	public User getUser() { return user; }
	public String getName() { return name; }
	public String getIdent() { return ident; }
	public String getHost() { return host; }
	public int channelCount() { return channels.size(); }
	public ChanNick[] getChannels() {
		ChanNick[] chans = new ChanNick[channels.size()];
		channels.values().toArray(chans);
		return chans;
	}
	
	public boolean isLoggedIn() { if (user == null) return false; return true; }
	
	@Override
	public String toString() {
		String out = "Nick("+name+"!"+ident+"@"+host+", ";
		if (isLoggedIn()) out += "logged in as "+user.getName();
		else out += "not logged in";
		out += ", channels[";
		int i = 1;
		for (ChanNick c : channels.values()) {
			if (c.isOp()) out += '@';
			if (c.isHalfOp()) out += '%';
			if (c.isVoice()) out += '+';
			out += c.getChannel().getName();
			if (i < channels.size()) out += ", ";
			i++;
		}
		return out+"])";
	}
}
