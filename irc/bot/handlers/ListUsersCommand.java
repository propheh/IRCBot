package irc.bot.handlers;

import irc.bot.*;

public class ListUsersCommand extends MessageCommand {
	public ListUsersCommand(String name) {
		super(name, "List Users Command");
		chan = false;
		minLvl = 100;
		accessType = "usercommands";
	}
	public String arguments() { return ""; };
	@Override
	public int call(Connection con, String source, String target, boolean inchan, String[] args) throws Exception, MessageCommandException {
		for (User u : con.getParent().getUsers().values()) {
			sendNotice(con, source, u.toString());
		}
		return 1;
	}
	public boolean match_args(String str) {
		return true;
	}
}
