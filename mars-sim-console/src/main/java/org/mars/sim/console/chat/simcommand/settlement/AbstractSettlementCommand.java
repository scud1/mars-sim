package org.mars.sim.console.chat.simcommand.settlement;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * This is a wrapper class to handle common code for Settlement commands.
 * The high number of Settlement commands justifies the class.
 */
public abstract class AbstractSettlementCommand extends ChatCommand {

	protected AbstractSettlementCommand(String shortCommand, String longCommand, String desc) {
		super(SettlementChat.SETTLEMENT_GROUP, shortCommand, longCommand, desc);
	}

	/**
	 * Execute this command. This will identify the target Settlement for the current ChatCommand in
	 * the Conversation.
	 */
	@Override
	public void execute(Conversation context, String input) {
		SettlementChat parent = (SettlementChat) context.getCurrentCommand();		
		Settlement settlement = parent.getSettlement();
		
		execute(context, input, settlement);
	}

	/**
	 * Execute the command for the target Settlement.
	 * @param context
	 * @param input
	 * @param settlement
	 * @param response All output goes here
	 */
	protected abstract void execute(Conversation context, String input, Settlement settlement);

}
