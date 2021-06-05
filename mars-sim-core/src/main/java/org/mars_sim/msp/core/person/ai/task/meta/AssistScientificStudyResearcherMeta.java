/**
 * Mars Simulation Project
 * AssistScientificStudyResearcherMeta.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.Collection;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.FavoriteType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.task.AssistScientificStudyResearcher;
import org.mars_sim.msp.core.person.ai.task.utils.MetaTask;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskTrait;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * Meta task for the AssistScientificStudyResearcher task.
 */
public class AssistScientificStudyResearcherMeta extends MetaTask {

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.assistScientificStudyResearcher"); //$NON-NLS-1$
    
    public AssistScientificStudyResearcherMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.RESEARCH);
		setTrait(TaskTrait.ACADEMIC);
		
		setPreferredJob(JobType.ACADEMICS);
	}

	@Override
    public Task constructInstance(Person person) {
        return new AssistScientificStudyResearcher(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;
        
        // Probability affected by the person's stress and fatigue.
        if (!person.getPhysicalCondition().isFitByLevel(1000, 70, 1000)) {
        	return 0;
        }
        
        if (person.isInside()) {
	        // Find potential researchers.
	        Collection<Person> potentialResearchers = AssistScientificStudyResearcher.getBestResearchers(person);
	        int size = potentialResearchers.size();
	        if (size == 0)
	        	return 0;
	        
	        else {
	            result += size * RandomUtil.getRandomInt(1, 10);

	            if (person.isInVehicle()) {	
	    	        // Check if person is in a moving rover.
	    	        if (Vehicle.inMovingRover(person)) {
	    		        // the bonus for proposing scientific study inside a vehicle, 
	    	        	// rather than having nothing to do if a person is not driving
	    	        	result += 20;
	    	        }
	            }
	            
                Person researcher = (Person) potentialResearchers.toArray()[0];

	            // If assistant is in a settlement, use crowding modifier.
                Building building = BuildingManager.getBuilding(researcher);
                if (building != null) {
                    result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, building);
                    result *= TaskProbabilityUtil.getRelationshipModifier(person, building);
                }
                result *= person.getAssociatedSettlement().getGoodsManager().getResearchFactor();
                
	            result = applyPersonModifier(result, person);
	        }
        }

        if (result <= 0) result = 0;
        
        return result;
    }
}
