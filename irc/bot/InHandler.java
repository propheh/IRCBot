package irc.bot;

import java.io.*;

public class InHandler implements Runnable {
	InHandler(Connection connection) {
		this.connection = connection;
	}
	
	private boolean run;
	private Connection connection;
	public void run() {
		LineNumberReader lnr = connection.getLineNumberReader();
		String line;
		run = true;
		while (run) {
			line = null;
			try {
				line = lnr.readLine();
			} catch (IOException e) { e.printStackTrace(); }
			if (line == null) connection.interrupt(); // error, close connection
		}
	}
	public void stop() { run = false; }
	
}
