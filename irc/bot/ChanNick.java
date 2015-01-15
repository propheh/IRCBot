package irc.bot;

public class ChanNick {
	ChanNick(Channel channel, Nick nick, String modes) {
		this(channel, nick);
		setModes(modes);
	}
	ChanNick(Channel channel, Nick nick) {
		this.channel = channel;
		this.nick = nick;
	}
	
	private Channel channel;
	private Nick nick;
	private boolean op, voice, halfop;
	
	public Channel getChannel() { return channel; }
	public Nick getNick() { return nick; }
	
	public boolean isOp() { return op; }
	public boolean isVoice() { return voice; }
	public boolean isHalfOp() { return halfop; }
	public void setOp(boolean op) { this.op = op; }
	public void setVoice(boolean voice) { this.voice = voice; }
	public void setHalfOp(boolean halfop) { this.halfop = halfop; }
	
	public void setModes(String modes) {
		char[] ma = modes.toCharArray();
		int len = ma.length;
		for (int i = 0; i < len; i++) {
			switch (ma[i]) {
				case('@'):
					setOp(true); break;
				case('%'):
					setHalfOp(true); break;
				case('+'):
					setVoice(true); break;
				default:
					return;
			}
		}
	}
}
