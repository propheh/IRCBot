package irc.bot;

public interface Handler {
	public int handle(Connection con, String source, String target, String msg);
}
