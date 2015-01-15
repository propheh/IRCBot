package irc.bot;

import java.io.*;

public class OutHandler implements Runnable {
	OutHandler(Connection connection) {
		this.connection = connection;
	}
	
	private Connection connection;
	public void run() {}
}
