/**
 * Mars Simulation Project
 * PeopleCommand.java
 * @date 2023-06-14
 * @author Barry Evans
 */

package org.mars.sim.console.chat.simcommand.settlement;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.CommandHelper;
import org.mars.sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * Command to display people in a Settlement.
 * This is a singleton.
 */
public class PeopleCommand extends AbstractSettlementCommand {

	public static final ChatCommand PEOPLE = new PeopleCommand();

	private PeopleCommand() {
		super("pe", "people", "Settlement population");
	}

	/** 
	 * Outputs the population info.
	 * 
	 * @return 
	 */
	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {
		StructuredResponse response = new StructuredResponse();
		
		Collection<Person> citizens = settlement.getAllAssociatedPeople();
		Collection<Person> eva = settlement.getOutsideEVAPeople();
		Collection<Person> indoorP = settlement.getIndoorPeople();
		Collection<Person> deceasedP = settlement.getDeceasedPeople();
		Collection<Person> buriedP = settlement.getBuriedPeople();

		Set<Person> everyone = new TreeSet<>();
		everyone.addAll(citizens);
		everyone.addAll(eva);
		everyone.addAll(indoorP);
		everyone.addAll(buriedP);
		everyone.addAll(deceasedP);
		
		response.appendHeading("Summary");
		response.appendLabelledDigit("Registered", citizens.size());
		response.appendLabelledDigit("Inside", indoorP.size());
		response.appendLabelledDigit("EVA Operation", eva.size());
		response.appendLabeledString("Deceased (Buried)", deceasedP.size() + "(" + buriedP.size() + ")");

		response.appendTableHeading("Name", CommandHelper.PERSON_WIDTH,
									"Citizen", "Inside", CommandHelper.BUILIDNG_WIDTH,
									"Mission", "EVA",
									"Dead", 6);
		for (Person person : everyone) {
			response.appendTableRow(person.getName(),
									citizens.contains(person),
									(indoorP.contains(person) ? person.getBuildingLocation().getName() : "No"),
									(person.getMission() != null),
									eva.contains(person),
									(buriedP.contains(person) ? "Buried"
											: (deceasedP.contains(person) ?
													"Yes" : "No"))
									);
		}
		
		context.println(response.getOutput());
		
		return true;
	}
}
