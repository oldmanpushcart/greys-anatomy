package com.googlecode.greysanatomy.console.command;

import java.io.PrintWriter;

import com.googlecode.greysanatomy.console.command.annotation.Cmd;

@Cmd("quit")
public class QuitCommand extends Command {

	public QuitCommand() {
		
		final PrintWriter pw = new PrintWriter(System.out);
		pw.println("Bye.");
		System.exit(0);
		
	}
	
	@Override
	public Action getAction() {
		return null;
	}

}
