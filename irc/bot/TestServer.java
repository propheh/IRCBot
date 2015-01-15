package irc.bot;

/*import java.net.*;
import java.io.*;
import java.util.*;*/

public class TestServer {
	public static void main(String[] args) {
		//System.out.printf("%1$te.%1$tm.%1$ty %tT", new Date());
		//System.out.println("abcd".indexOf('q'));
		Bot b = new Bot();
		
		//b.connect("local", "localhost", 6667, "IRCBot");
		/*UserParser parser = new UserParser();
		System.out.println("Groups:");
		for (User u : parser.getGroups().values()) {
			System.out.println("\t"+u);
		}
		System.out.println("Users:");
		for (User u : parser.getUsers().values()) {
			System.out.println("\t"+u);
		}*/
		/*
		Connection conn = new Connection("localhost", 6667, "IRCBot");
		conn.run();
		/*
		try {
			ServerSocket server = new ServerSocket(6969);
			while (true) {
				Socket client = server.accept();
				LineNumberReader lnr = new LineNumberReader(new InputStreamReader(client.getInputStream()));
				while (true) {
					String nc = lnr.readLine();
					System.out.println(nc);
				}
			}
		}
		catch (Exception e) { e.printStackTrace(); }*/
	}
}
