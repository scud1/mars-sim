/*
 * Mars Simulation Project
 * JobProspectCommand.java
 * @date 2022-08-24
 * @author Barry Evans
 */

package org.mars.sim.console.chat.simcommand.person;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.CommandHelper;
import org.mars.sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.util.Job;
import org.mars_sim.msp.core.person.ai.job.util.Assignment;
import org.mars_sim.msp.core.person.ai.job.util.AssignmentHistory;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.job.util.JobUtil;

/** 
 * 
 */
public class JobProspectCommand extends AbstractPersonCommand {
	public static final ChatCommand JOB_PROSPECT = new JobProspectCommand();
	private JobProspectCommand() {
		super("jp", "job prospect", "About my job prospects");
	}

	@Override
	public boolean execute(Conversation context, String input, Person person) {
		StructuredResponse response = new StructuredResponse();

		// Details of proposes to change Job
		response.appendTableHeading("Job", CommandHelper.JOB_WIDTH, "Capability Score", "Prospect Score");
		for (JobType job : JobType.values()) {
			Job jobSpec = JobUtil.getJobSpec(job);
			double capScore = Math.round(jobSpec.getCapability(person) * 10.0) / 10.0;
			double prospectScore = Math.round(
					JobUtil.getJobProspect(person, job, person.getAssociatedSettlement(), true) * 10.0)
					/ 10.0;

			response.appendTableRow(job.getName(), capScore, prospectScore);
		}
		
		// Job history
		response.appendBlankLine();
		response.appendTableHeading("Date", 18, "New Job", CommandHelper.JOB_WIDTH,
									"Application", "Approved By");
		AssignmentHistory jh = person.getJobHistory();
		for(Assignment j : jh.getJobAssignmentList()) {
			response.appendTableRow(j.getTimeSubmitted(),
					j.getType(),
					j.getStatus().getName(),
					j.getAuthorizedBy());
		}
		
		context.println(response.getOutput());
		
		return true;
	}
}
