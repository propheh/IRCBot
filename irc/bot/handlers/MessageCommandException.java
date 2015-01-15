package irc.bot.handlers;

public class MessageCommandException extends Throwable {
	public MessageCommandException(String msg) {
		super(msg);
	}
	public static final long serialVersionUID = 44321; // no clue wtf this does
}
