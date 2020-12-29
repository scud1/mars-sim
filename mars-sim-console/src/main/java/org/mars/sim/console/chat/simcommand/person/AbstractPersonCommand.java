package org.mars.sim.console.chat.simcommand.person;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars_sim.msp.core.person.Person;

public abstract class AbstractPersonCommand extends ChatCommand {

	protected AbstractPersonCommand(String shortCommand, String longCommand, String desc) {
		super(PersonChat.PERSON_GROUP, shortCommand, longCommand, desc);
	}
	
	@Override
	public void execute(Conversation context, String input) {
		PersonChat parent = (PersonChat) context.getCurrentCommand();
		Person person = parent.getPerson();
		
		execute(context, input, person);
	}

	protected abstract void execute(Conversation context, String input, Person person);
}
