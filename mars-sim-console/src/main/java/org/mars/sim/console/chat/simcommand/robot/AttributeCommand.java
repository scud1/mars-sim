package org.mars.sim.console.chat.simcommand.robot;

import java.util.List;
import java.util.Map;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RoboticAttributeManager;
import org.mars_sim.msp.core.robot.RoboticAttributeType;

/** 
 * 
 */
public class AttributeCommand extends ChatCommand {
	public static final ChatCommand ATTRIBUTES = new AttributeCommand();
	
	private AttributeCommand() {
		super(RobotChat.ROBOT_GROUP, "at", "attributes", "About my attributes");
	}

	@Override
	public void execute(Conversation context, String input) {
		RobotChat rchat = (RobotChat) context.getCurrentCommand();
		Robot robot = (Robot) rchat.getUnit();
		
		StructuredResponse response = new StructuredResponse();

		response.appendTableHeading("Attributes", 20, "Score");

		RoboticAttributeManager r_manager = robot.getRoboticAttributeManager();
		Map<RoboticAttributeType, Integer> r_attributes = r_manager.getAttributeMap();
		List<String> attributeList = r_manager.getAttributeList();

		for (String attr : attributeList) {
			response.appendTableRow(attr,
					r_attributes.get(RoboticAttributeType.valueOfIgnoreCase(attr)));
		}
		context.println(response.getOutput());
	}
}
