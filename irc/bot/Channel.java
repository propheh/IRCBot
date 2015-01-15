package irc.bot;

import java.util.TreeMap;

public class Channel {
	Channel(String name) {
		this.name = name;
	}
	
	private String name, modes = "", topic = "", key = "";
	private boolean op = false, hop = false, voice = false;
	
	private TreeMap<String, ChanNick> nicks = new TreeMap<String, ChanNick>();
	
	public void leave() {
		for (ChanNick n : nicks.values()) {
			n.getNick().part(this, false);
		}
		nicks.clear();
	}
	public void clear() { nicks.clear(); }
	public void addNick(ChanNick nick) {
		nicks.put(nick.getNick().getName(), nick);
	}
	public void removeNick(Nick nick) {
		nicks.remove(nick.getName());
	}
	public void setModes(String modes) { this.modes = modes; }
	public void addMode(char c) {
		if (modes.indexOf(c) == -1) modes += c;
	}
	public void delMode(char c) {
		String modes = "";
		char[] ca = this.modes.toCharArray();
		for (int i = 0; i < ca.length; i++) {
			if (ca[i] != c) modes += ca[i];
		}
		this.modes = modes;
	}

	public void setKey(String key) {
		this.key = key;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	// getter
	public String getName() { return name; }
	public String getModes() { return modes; }
	public int nickCount() { return nicks.size() + 1; } // at least we should be on the channel
	public ChanNick[] getNicks() {
		ChanNick[] na = new ChanNick[nicks.size()];
		nicks.values().toArray(na);
		return na;
	}
	public ChanNick getNick(String name) {
		return nicks.get(name);
	}
	public String getKey() {
		return key;
	}
	public boolean isKeyed() { if (key != null && !key.isEmpty()) return true; return false; }
	public String getTopic() {
		return topic;
	}
	
	@Override
	public String toString() {
		String out = "Channel("+name+", ";
		if (!isKeyed()) out += "not ";
		out += "keyed, modes["+modes+"], topic["+topic+"], nicks[";
		int i = 1;
		for (ChanNick c : nicks.values()) {
			if (c.isOp()) out += '@';
			if (c.isHalfOp()) out += '%';
			if (c.isVoice()) out += '+';
			out += c.getNick().getName();
			if (i < nicks.size()) out += ", ";
			i++;
		}
		return out+"])";
	}
	public boolean isHalfOp() {
		return hop;
	}
	public void setHalfOp(boolean hop) {
		this.hop = hop;
	}
	public boolean isOp() {
		return op;
	}
	public void setOp(boolean op) {
		this.op = op;
	}
	public boolean isVoice() {
		return voice;
	}
	public void setVoice(boolean voice) {
		this.voice = voice;
	}
}
