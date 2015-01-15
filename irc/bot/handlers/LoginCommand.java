package irc.bot.handlers;

import java.util.regex.Pattern;
import irc.bot.*;

public class LoginCommand extends MessageCommand {
	public LoginCommand(String name) {
		super(name, "Login Command");
		pattern = Pattern.compile("^[^ ]+ +[^ ]+ +[^ ]+$");
		chan = false;
	}
	public String arguments() { return "<username> <password>"; };
	@Override
	public int call(Connection con, String source, String target, boolean inchan, String[] args) throws Exception, MessageCommandException {
		User u = con.getParent().getUser(args[1]);
		Nick n = con.getNick(source);
		if (Connection.isChan(target)) return 1; // we dont like ppl putting login command in channels
		if (u != null) {
			if (n != null) {
				if (!n.isLoggedIn()) {
					if (u.checkPass(args[2])) {
						n.setUser(u);
						sendNotice(con, source, "Logged in successfully!");
					}
					else sendNotice(con, source, "Wrong password!");
				}
				else sendNotice(con, source, "You are already logged in!");
			}
			else sendNotice(con, source, "You must share a channel with this bot!");
		}
		else sendNotice(con, source, "Username not found!");
		return 1;
	}
	public boolean match_args(String str) {
		return pattern.matcher(str).matches();
	}
}
