/**
 * Mars Simulation Project
 * PhysicalCondition.java
 * @version 3.1.2 2020-09-02
 * @author Barry Evans
 */
package org.mars_sim.msp.core.person;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LifeSupportInterface;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.NaturalAttributeManager;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.task.meta.EatDrinkMeta;
import org.mars_sim.msp.core.person.health.Complaint;
import org.mars_sim.msp.core.person.health.ComplaintType;
import org.mars_sim.msp.core.person.health.DeathInfo;
import org.mars_sim.msp.core.person.health.HealthProblem;
import org.mars_sim.msp.core.person.health.MedicalManager;
import org.mars_sim.msp.core.person.health.Medication;
import org.mars_sim.msp.core.person.health.RadiationExposure;
import org.mars_sim.msp.core.person.health.RadioProtectiveAgent;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * This class represents the Physical Condition of a Person. It models a
 * person's health and physical characteristics.
 */
public class PhysicalCondition implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(PhysicalCondition.class.getName());

	/** The amount of thirst threshold [millisols]. */
	public static final int THIRST_THRESHOLD = 150;// + RandomUtil.getRandomInt(20);
	/** The amount of thirst threshold [millisols]. */
	public static final int HUNGER_THRESHOLD = 500;// + RandomUtil.getRandomInt(30);
	/** The amount of thirst threshold [millisols]. */
	public static final int ENERGY_THRESHOLD = 2525;// + RandomUtil.getRandomInt(20);
	/** The amount of fatigue threshold [millisols]. */
	public static final int FATIGUE_THRESHOLD = 500;
	/** The amount of stress threshold [millisols]. */
	public static final int STRESS_THRESHOLD = 50;
	
	/** Life support minimum value. */
	private static final int MIN_VALUE = 0;
	/** Life support maximum value. */
	private static final int MAX_VALUE = 1;
	
	/** The amount of fatigue for the mental breakdown to occur [millisols]. */
	public static final double MENTAL_BREAKDOWN = 100D;
	/** The amount of fatigue for the collapse to occur [millisols]. */
	private static final double COLLAPSE_IMMINENT = 3000D;
	/** Stress jump resulting from being in an accident. */
	public static final double ACCIDENT_STRESS = 10D;
	/** The food reserve factor. */
	public static final double FOOD_RESERVE_FACTOR = 1.5D;
	/** Performance modifier for thirst. */
	private static final double THIRST_PERFORMANCE_MODIFIER = .00015D;
	/** Performance modifier for hunger. */
	private static final double HUNGER_PERFORMANCE_MODIFIER = .0001D;
	/** Performance modifier for fatigue. */
	private static final double FATIGUE_PERFORMANCE_MODIFIER = .0005D;
	/** Performance modifier for stress. */
	private static final double STRESS_PERFORMANCE_MODIFIER = .005D;
	/** Performance modifier for energy. */
	private static final double ENERGY_PERFORMANCE_MODIFIER = .0001D;
	/** The average maximum daily energy intake */
	private static final double MAX_DAILY_ENERGY_INTAKE = 10100D;
	/** The average kJ of a 1kg food. Assume each meal has 0.1550 kg and has 2525 kJ. */
	public static final double FOOD_COMPOSITION_ENERGY_RATIO = 16290.323;
	// public static int MAX_KJ = 16290; // 1kg of food has ~16290 kJ (see notes on
	// people.xml under <food-consumption-rate value="0.62" />)
	public static final double ENERGY_FACTOR = 0.8D;
	/** The maximum air pressure a person can live without harm in kPa. (somewhat arbitrary). */
	public static final double MAXIMUM_AIR_PRESSURE = 68D; // Assume 68 kPa time dependent
	/** Period of time (millisols) over which random ailments may happen. */
	private static final double RANDOM_AILMENT_PROBABILITY_TIME = 100_000D;
	/** A decimal number a little bigger than zero for comparing doubles. */
	private static final double SMALL_AMOUNT = 0.001;
	
	private static final double h2o_consumption;
	private static final double minimum_air_pressure;
	private static final double min_temperature;
	private static final double max_temperature;
	private static final double food_consumption;
	private static final double dessert_consumption;
	private static final double highFatigueCollapseChance;
	private static final double stressBreakdownChance;

	public static final String WELL = "Well";
	public static final String DEAD = "Dead";
	public static final String DEAD_COLON = "Dead : ";
	public static final String SICK_COLON = "Sick : ";
	public static final String TBD = "(To be determined)";
	public static final String SUICIDE = "Suicide";
	public static final String INSTRUCTED = " committed suicide as instructed.";
	
	/** The standard pre-breathing time in the EVA suit. */
	private static final double STANDARD_PREBREATHING_TIME = 40;
	
	private static double o2_consumption;
	
	/** The time it takes to prebreathe the air mixture in the EVA suit. */
	private double remainingPrebreathingTime = STANDARD_PREBREATHING_TIME + RandomUtil.getRandomInt(-5, 5);
	/**  The amount of water this person would consume each time (assuming drinking water 8 times a day). */
	private double waterConsumedPerServing;
	/** True if person is starving. */
	private boolean isStarving;
	/** True if person is stressed out. */
	private boolean isStressedOut;
//	/** True if person is collapsed under fatigue. */
//	private boolean isCollapsed;
	/** True if person is dehydrated. */
	private boolean isDehydrated;
	/** True if person is alive. */
	private boolean alive;
	/** True if person is radiation Poisoned. */
	private boolean isRadiationPoisoned;
	/** True if person is doing a task that's considered resting. */
	private boolean restingTask;

	private int endurance;
	private int strength;
	private int resilience;
	private int emotStability;
	private int agility;

	/**
	 * Person's Musculoskeletal system from 0 to 100 (muscle pain tolerance, muscle
	 * health, muscle soreness).
	 */
	private double[] musculoskeletal = new double[] { 0, 0, 0 };
	/** Person's thirst level. [in millisols]. */
	private double thirst;
	/** Person's fatigue level from 0 to infinity. */
	private double fatigue;
	/** Person's hunger level [in millisols]. */
	private double hunger;
	/** Person's stress level (0.0 % - 100.0 %). */
	private double stress;
	/** Performance factor 0.0 to 1.0. */
	private double performance;
	/** Person's hygiene factor (0.0 - 100.0) */
	// private double hygiene;
	/** Person's energy level [in kJ] */
	private double kJoules;
	/** Person's food appetite (0.0 to 1.0) */
	private double appetite;

	private double inclination_factor;

	private double starvationStartTime;

	private double dehydrationStartTime;

	private double personalMaxEnergy;

	private double foodDryMassPerServing;

	private double bodyMassDeviation;

	private String name;

	/** Person owning this physical. */
	private Person person;
	/** Details of persons death. */
	private DeathInfo deathDetails;

	/** Radiation Exposure. */
	private RadiationExposure radiation;

	/** List of medications affecting the person. */
	private List<Medication> medicationList;
	/** Injury/Illness effecting person. */
	private List<HealthProblem> problems;
	/** Record of Illness frequency. */
	private Map<ComplaintType, Integer> healthLog;
	/** Record of illness start time. */
	private Map<ComplaintType, List<String>> healthHistory;

	/** The CircadianClock instance. */
	private transient CircadianClock circadian;
	/** The TaskManager instance. */
//	private transient TaskManager taskMgr;
	/** The NaturalAttributeManager instance. */
	private transient NaturalAttributeManager naturalAttributeManager;
	
	/** The HealthProblem instance. */
	private HealthProblem starved;
	/** The HealthProblem instance. */
	private HealthProblem dehydrated;
	/** The HealthProblem instance. */
	private HealthProblem radiationPoisoned;
	/** Most serious problem. */
	private HealthProblem serious;
	
	
	/** A static list of all available medical complaints. */
//	private static List<Complaint> allMedicalComplaints;
	
	private static Simulation sim = Simulation.instance();
	private static MarsClock marsClock;
	private static MasterClock masterClock;
	
	private static EatDrinkMeta eatMealMeta = new EatDrinkMeta();
	private static MedicalManager medicalManager;
//	private static Complaint depression;
//	private static Complaint panicAttack;
//	private static Complaint highFatigue;
	private static Complaint radiationPoisoning;

	private static Complaint dehydration;
	private static Complaint starvation;
	private static Complaint freezing;
	private static Complaint heatStroke;
	private static Complaint decompression;
	private static Complaint suffocation;

	private static PersonConfig personConfig;

	/**
	 * Loads the values
	 */
	static {
		masterClock = sim.getMasterClock();
		if (masterClock != null)  // check for null in order to pass maven test
			marsClock = masterClock.getMarsClock();
			
		personConfig = SimulationConfig.instance().getPersonConfig();
		
		h2o_consumption = personConfig.getWaterConsumptionRate(); // 3 kg per sol
		o2_consumption = personConfig.getNominalO2ConsumptionRate();

		minimum_air_pressure = personConfig.getMinAirPressure();
		min_temperature = personConfig.getMinTemperature();
		max_temperature = personConfig.getMaxTemperature();
		food_consumption = personConfig.getFoodConsumptionRate();
		dessert_consumption = personConfig.getDessertConsumptionRate();

		stressBreakdownChance = personConfig.getStressBreakdownChance();
		highFatigueCollapseChance = personConfig.getHighFatigueCollapseChance();
		
		medicalManager = sim.getMedicalManager();
		
		// Set health instances
		if (medicalManager != null) {
			// Note that this 'if' above is for maven test, or else NullPointerException
//			allMedicalComplaints = medicalManager.getAllMedicalComplaints();
			
//			panicAttack = medicalManager.getComplaintByName(ComplaintType.PANIC_ATTACK);
//			depression = medicalManager.getComplaintByName(ComplaintType.DEPRESSION);
//			highFatigue = medicalManager.getComplaintByName(ComplaintType.HIGH_FATIGUE_COLLAPSE);
			radiationPoisoning = medicalManager.getComplaintByName(ComplaintType.RADIATION_SICKNESS);
			dehydration = medicalManager.getDehydration();
			starvation = medicalManager.getStarvation();
			
			freezing = medicalManager.getFreezing();
			heatStroke = medicalManager.getHeatStroke();
			decompression = medicalManager.getDecompression();
			suffocation = medicalManager.getSuffocation();
		}
	}
	
	/**
	 * Constructor 1.
	 * 
	 * @param newPerson The person requiring a physical presence.
	 */
	public PhysicalCondition(Person newPerson) {
		person = newPerson;
		name = newPerson.getName();
		
		circadian = person.getCircadianClock();
//		taskMgr = person.getMind().getTaskManager();
		naturalAttributeManager = person.getNaturalAttributeManager();
		
		alive = true;

		radiation = new RadiationExposure(this);
		radiation.initializeWithRandomDose();

		deathDetails = null;

		problems = new CopyOnWriteArrayList<HealthProblem>();
		healthLog = new ConcurrentHashMap<ComplaintType, Integer>();
		healthHistory = new ConcurrentHashMap<ComplaintType, List<String>>();
		medicationList = new CopyOnWriteArrayList<Medication>();

		endurance = naturalAttributeManager.getAttribute(NaturalAttributeType.ENDURANCE);
		strength = naturalAttributeManager.getAttribute(NaturalAttributeType.STRENGTH);
		resilience = naturalAttributeManager.getAttribute(NaturalAttributeType.STRESS_RESILIENCE);
		emotStability = naturalAttributeManager.getAttribute(NaturalAttributeType.EMOTIONAL_STABILITY);
		agility = naturalAttributeManager.getAttribute(NaturalAttributeType.AGILITY);

		// Computes the adjustment from a person's natural attributes
		double es = (endurance + strength + agility) / 300D;

		// TODO: may incorporate real world parameters such as areal density in g cm−2,
		// T-socre and Z-score (see https://en.wikipedia.org/wiki/Bone_density)
		musculoskeletal[0] = RandomUtil.getRandomInt(-10, 10) + (int) es; // pain tolerance
		musculoskeletal[1] = 50; // muscle health index; 50 being the average
		musculoskeletal[2] = RandomUtil.getRandomRegressionInteger(100); // muscle soreness
		
		personalMaxEnergy = MAX_DAILY_ENERGY_INTAKE;
		appetite = personalMaxEnergy / MAX_DAILY_ENERGY_INTAKE;
		
		bodyMassDeviation = Math.sqrt(person.getBaseMass() / Person.getAverageWeight() 
				* person.getHeight() / Person.getAverageHeight());
		// Note: p = mean + RandomUtil.getGaussianDouble() * standardDeviation
		bodyMassDeviation = bodyMassDeviation + RandomUtil.getGaussianDouble() * bodyMassDeviation / 7D;		
		// Assume a person drinks 10 times a day, each time ~375 mL
		waterConsumedPerServing = h2o_consumption * bodyMassDeviation / 10D; // about .3 kg per serving

		foodDryMassPerServing = food_consumption / (double) Cooking.NUMBER_OF_MEAL_PER_SOL;

		starvationStartTime = 1000D * (personConfig.getStarvationStartTime() - RandomUtil.getGaussianDouble() * bodyMassDeviation / 3);

		dehydrationStartTime = 1000D * (personConfig.getDehydrationStartTime() - RandomUtil.getGaussianDouble() * bodyMassDeviation);
	
		isStarving = false;
		isStressedOut = false;
//		isCollapsed = false;
		isDehydrated = false;
		// Initially set performance to 1.0 (=100%) to avoid issues at startup
		performance = 1.0D;
		
		initialize();
	}

	private void initializeHealthIndices() {
		// Set up random physical healt index
		thirst = RandomUtil.getRandomRegressionInteger(50);
		
//		fatigue = RandomUtil.getRandomRegressionInteger(50);
		stress = RandomUtil.getRandomRegressionInteger(30);
		
		hunger = RandomUtil.getRandomRegressionInteger(200);
		// kJoules somewhat co-relates with hunger
		kJoules = 10000 + (200 - hunger) * 100;

		performance = 1.0D - (50 - fatigue) * .002 - (50 - stress) * .002 - (200 - hunger) * .002;
		
	}
	
	/**
	 * Initialize values and instances at the beginning of sol 1
	 * (Note : Must skip this when running maven test or else having exceptions)
	 */
	private void initialize() {
		// Set up the initial values for each physical health index
		initializeHealthIndices();
		// Modify personalMaxEnergy at the start of the sim
		int d1 = 2 * (35 - person.getAge()); 
		// Assume that after age 35, metabolism slows down
		double d2 = person.getBaseMass() - Person.getAverageWeight();
		double preference = person.getPreference().getPreferenceScore(eatMealMeta) * 10D;

		// Update the personal max energy and appetite based on one's age and weight
		personalMaxEnergy = personalMaxEnergy + d1 + d2 + preference;
		appetite = personalMaxEnergy / MAX_DAILY_ENERGY_INTAKE;
		
//		logger.info(person + " : " + personalMaxEnergy + "   appetite : " + Math.round(appetite*1000.0)/1000.0);
	}

	public void recoverFromSoreness(double value) {
		// Reduce the muscle soreness by 1 point at the end of the day
		double soreness = musculoskeletal[2];
		soreness = soreness - value;
		if (soreness < 0)
			soreness = 0;
		musculoskeletal[2] = soreness;
	}

	/**
	 * The Physical condition should be updated to reflect a passing of time. This
	 * method has to check the recover or degradation of any current illness. The
	 * progression of this time period may result in the illness turning fatal. It
	 * also updated the hunger and fatigue status
	 *
	 * @param time    amount of time passing (in millisols)
	 * @param support life support system.
	 * @return True still alive.
	 */
	public void timePassing(ClockPulse pulse, LifeSupportInterface support) {
//		if (time > 10)
//			System.out.println("time : " + time);
		if (alive) {
			double time = pulse.getElapsed();
			
			// Check once a day only
			if (pulse.isNewSol()) {
				// reduce the muscle soreness
				recoverFromSoreness(1);
			}

			// Check if a person is performing low aerobic tasks 
			if (person.getTaskDescription().toLowerCase().contains("eat")
						|| person.getTaskDescription().toLowerCase().contains("drink")
//					person.getTaskDescription().toLowerCase().contains("assist")
//					|| person.getTaskDescription().toLowerCase().contains("compil")
						|| person.getTaskDescription().toLowerCase().contains("meet")
//					|| person.getTaskDescription().toLowerCase().contains("peer")
						|| person.getTaskDescription().toLowerCase().contains("relax")
						|| person.getTaskDescription().toLowerCase().contains("rest")
						|| person.getTaskDescription().toLowerCase().contains("sleep")
//					|| person.getTaskDescription().toLowerCase().contains("teach")
//					|| person.getTaskDescription().toLowerCase().contains("walk")		
//					|| person.getTaskDescription().toLowerCase().contains("yoga")	
				) {
					restingTask = true;
				} else
					restingTask = false;
			
			if (restingTask)
				o2_consumption = personConfig.getLowO2ConsumptionRate();
			else
				o2_consumption = personConfig.getNominalO2ConsumptionRate();
				
			// Check life support system
			checkLifeSupport(time, support);
			// Update radiation counter
			radiation.timePassing(pulse);
			// Update the existing health problems
			checkHealth(pulse);
			

			// Build up fatigue & hunger for given time passing.
			setThirst(thirst + time * bodyMassDeviation);
//			System.out.println(person + " fatigue : " + fatigue);
			setFatigue(fatigue + time);
//			System.out.println(person + " fatigue : " + fatigue);
			setHunger(hunger + time * bodyMassDeviation);

			// normal bodily function consume a minute amount of energy
			// even if a person does not perform any tasks
			// Note: removing this as reduce energy is already handled
			// in the TaskManager and people are always performing tasks
			// unless dead. - Scott
			// reduceEnergy(time);

			int msol = pulse.getMarsTime().getMillisolInt();
			if (msol % 7 == 0) {

//				if (!restingTask) {
					checkStarvation(hunger);
					checkDehydration(thirst);
//				}

//				// If person is at high stress, check for mental breakdown.
//				if (!isStressedOut)
//					if (stress > MENTAL_BREAKDOWN)
//						checkForStressBreakdown(time);
////					else if (stress < 33) {
////						isStressedOut = false;
////						checkHealth(time);// remove highFatigue
////					}
//				
//				// Check if person is at very high fatigue may collapse.
//				if (!isCollapsed)
//					if (fatigue > COLLAPSE_IMMINENT)
//						checkForHighFatigueCollapse(time);
////					else if (fatigue < 333) {
////						isCollapsed = false;
////						checkHealth(time);// remove highFatigue
////					}

				if (!isRadiationPoisoned)
					checkRadiationPoisoning(time);
			}

			// Calculate performance and most serious illness.
			recalculatePerformance();
		}
	}
		

	 /**
	  * Checks and updates existing health problems
	  * 
	  * @param time
	  */
	private void checkHealth(ClockPulse pulse) {
		boolean illnessEvent = false;
		
		if (!problems.isEmpty()) {
			// Throw illness event if any problems already exist.
			illnessEvent = true;
			// A list of complaints (Type of illnesses)
			List<Complaint> newComplaints = new CopyOnWriteArrayList<Complaint>();

			Iterator<HealthProblem> hp = problems.iterator();
			while (hp.hasNext()) {
				HealthProblem problem = hp.next();
				// Advance each problem, they may change into a worse problem.
				// If the current is completed or a new problem exists then
				// remove this one.
				Complaint nextComplaintPhase = problem.timePassing(pulse.getElapsed(), this);
	
				// After sleeping sufficiently, the high fatigue collapse should no longer exist.
//				if (problem.getIllness().getType() == ComplaintType.HIGH_FATIGUE_COLLAPSE
//						&& fatigue < 500) {
//					isCollapsed = false;
//					problems.remove(problem.getIllness());	
//				}
				
//				LogConsolidated.log(logger, Level.INFO, 20_000, sourceName,
//						 person + " had " + problem.getType() + " (state : " + problem.getStateString() + ").");
				
				if (problem.isCured() || (nextComplaintPhase != null)) {
//					Complaint c = problem.getIllness();
					ComplaintType type = problem.getType();
	
					logger.log(person, Level.INFO, 20_000, 
							"Cured from " + type + ".");
					
//					if (c.getType() == ComplaintType.HIGH_FATIGUE_COLLAPSE)
//						isCollapsed = false;
//	
//					else if (c.getType() == ComplaintType.PANIC_ATTACK || c.getType() == ComplaintType.DEPRESSION)
//						isStressedOut = false;
	
					if (type == ComplaintType.DEHYDRATION)
						isDehydrated = false;
	
					else if (type == ComplaintType.STARVATION)
						isStarving = false;
	
					else if (type == ComplaintType.RADIATION_SICKNESS)
						isRadiationPoisoned = false;
	
					// If nextPhase is not null, remove this problem so that it can 
					// properly be transitioned into the next.
					problems.remove(problem);
	
				}
	
				// If a new problem, check it doesn't exist already
				if (nextComplaintPhase != null) {
					newComplaints.add(nextComplaintPhase);
				}
				
			}
	
//			LogConsolidated.log(logger, Level.INFO, 20_000, sourceName,
//					 person + " had a list of " + problems + ".");
			
			// Add the new problems
			for (Complaint c : newComplaints) {
				addMedicalComplaint(c);
				illnessEvent = true;
			}
		}
	
		// Has the person died ?
		// if (isDead()) return false;
	
		// Generates any random illnesses.
		if (!restingTask) {
			List<Complaint> randomAilments = checkForRandomAilments(pulse);
			if (randomAilments.size() > 0) {
				illnessEvent = true;
			}
		}
	
		if (illnessEvent) {
			person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
		}
	
		// Add time to all medications affecting the person.
		Iterator<Medication> i = medicationList.iterator();
		while (i.hasNext()) {
			Medication med = i.next();
			med.timePassing(pulse);
			if (!med.isMedicated()) {
				i.remove();
			}
		}
	}
	
	/**
	 * Checks on the life support
	 * 
	 * @param time
	 * @param support
	 */
	public void checkLifeSupport(double time, LifeSupportInterface support) {
		if (time > 0) {
			try {
				if (lackOxygen(support, o2_consumption * (time / 1000D)))
					logger.log(person, Level.SEVERE, 60_000, "Reported lack of oxygen.");
				if (badAirPressure(support, minimum_air_pressure))
					logger.log(person, Level.SEVERE, 60_000, "Reported non-optimal air pressure.");
				if (badTemperature(support, min_temperature, max_temperature))
					logger.log(person, Level.SEVERE, 60_000, "Reported non-optimal temperature.");
				
			} catch (Exception e) {
				e.printStackTrace();
				logger.log(person, Level.SEVERE, 60_000, "Reported anomaly in the life support system.");
			}
		}
	}

	/**
	 * Gets the person's fatigue level 
	 * 
	 * @return the value from 0 to infinity.
	 */
	public double getFatigue() {
		return fatigue;
	}

	public double getThirst() {
		return thirst;
	}

	/**
	 * Gets the person's caloric energy.
	 * 
	 * @return person's caloric energy in kilojoules Note: one large calorie is
	 *         about 4.2 kilojoules
	 */
	public double getEnergy() {
		return kJoules;
	}

	/**
	 * Reduces the person's energy.
	 * 
	 * @param time the amount of time (millisols).
	 */
	public void reduceEnergy(double time) {
		double xdelta = time * MAX_DAILY_ENERGY_INTAKE / 1000D;

		// Changing this to a more linear reduction of energy.
		// We may want to change it back to exponential. - Scott

		// double xdelta = 4 * time / FOOD_COMPOSITION_ENERGY_RATIO;
		// kJoules = kJoules / exponential(xdelta);

		if (kJoules < 200D) {
			kJoules -= xdelta * .75;
		} else if (kJoules < 400D) {
			kJoules -= xdelta * .8;
		} else if (kJoules < 600D) {
			kJoules -= xdelta * .85;
		} else if (kJoules < 800D) {
			kJoules -= xdelta * .9;
		} else if (kJoules < 1000D) {
			kJoules -= xdelta * .95;
		} else
			kJoules -= xdelta;

		if (kJoules < 100D) {
			// 100 kJ is the lowest possible energy level
			kJoules = 100D;
		}
	}

	/**
	 * Adds to the person's energy intake by eating
	 * 
	 * @param person's energy level in kilojoules
	 */
	public void addEnergy(double foodAmount) {
		// 1 calorie = 4.1858 kJ
		// TODO: vary MAX_KJ according to the individual's physical profile strength,
		// endurance, etc..
		// double FOOD_COMPOSITION_ENERGY_RATIO = 16290; 1kg of food has ~16290 kJ (see
		// notes on people.xml under <food-consumption-rate value="0.62" />)
		// double FACTOR = 0.8D;
		// Each meal (.155 kg = .62/4) has an average of 2525 kJ

		// Note: changing this to a more linear addition of energy.
		// We may want to change it back to exponential. - Scott

		double xdelta = foodAmount * FOOD_COMPOSITION_ENERGY_RATIO / appetite;
		// kJoules += foodAmount * xdelta * Math.log(FOOD_COMPOSITION_ENERGY_RATIO /
		// kJoules) / ENERGY_FACTOR;

		if (kJoules > 10_000D) {
			kJoules += xdelta * .65;
		} else if (kJoules > 9_000D) {
			kJoules += xdelta * .6;
		} else if (kJoules > 8_000D) {
			kJoules += xdelta * .75;
		} else if (kJoules > 7_000D) {
			kJoules += xdelta * .8;
		} else if (kJoules > 6_000D) {
			kJoules += xdelta * .85;
		} else
			kJoules += xdelta * .9;

		circadian.eatFood(kJoules / 50D);

		if (kJoules > personalMaxEnergy * 2) {
			kJoules = personalMaxEnergy * 2;
		}

	}

	/**
	 * Get the performance factor that effect Person with the complaint.
	 * 
	 * @return The value is between 0 -> 1.
	 */
	public double getPerformanceFactor() {
		return performance;
	}

	/**
	 * Sets the performance factor.
	 * 
	 * @param newPerformance new performance (between 0 and 1).
	 */
	public void setPerformanceFactor(double p) {
		if (p > 1D)
			p = 1D;
		else if (p < 0)
			p = 0;
		if (performance != p)
			performance = p;
		// person.fireUnitUpdate(UnitEventType.PERFORMANCE_EVENT);
	}

	/**
	 * Define the fatigue setting for this person
	 * 
	 * @param newFatigue New fatigue.
	 */
	public void setFatigue(double f) {
		if (f > 3000)
			fatigue = 3000;
		else if (f < 0)
			f = 0;

		fatigue = f;
		// person.fireUnitUpdate(UnitEventType.FATIGUE_EVENT);
	}

	public void setThirst(double t) {
		if (t > 4000)
			t = 4000;
		thirst = t;
//		if (t > THIRST_THRESHOLD && !isThirsty)
//			isThirsty = true;
//		else if (isThirsty)
//			isThirsty = false;
		// person.fireUnitUpdate(UnitEventType.THIRST_EVENT);
	}

	/**
	 * Gets the person's hunger level
	 * 
	 * @return person's hunger
	 */
	public double getHunger() {
		return hunger;
	}

	/**
	 * Checks if a person is starving or no longer starving
	 * 
	 * @param hunger
	 */
	private void checkStarvation(double hunger) {
		starved = getStarvationProblem();
		
//		LogConsolidated.log(logger, Level.INFO, 20_000, sourceName,
//				 person + " - Check starvation. "
//				 + "  Hunger: " + (int)hunger 
//				 + ";  kJ: " + Math.round(kJoules*10.0)/10.0 
//				 + ";  Complaint: " + starvation
//				 + ";  isStarving: " + isStarving
//				 + ";  Status: " + starved);

		if (!isStarving && hunger > starvationStartTime) {
			
			// if problems doesn't have starvation, execute the following
			if (!problems.contains(starved)) {
				
				addMedicalComplaint(starvation);
				
				isStarving = true;
				
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
				
				logger.log(person, Level.INFO, 20_000, "Starting starving.");
			}

			else if (starved != null) {
				
				starved.setCured();
				// Set isStarving to false
				isStarving = false;
				
				logger.log(person, Level.INFO, 20_000, "Cured of starving (case 1).");
			}
			
			// TODO : how to tell a person to walk back to the settlement ?
			// TODO : should check if a person is on a critical mission,
		}

		else if (isStarving) {
		
			if (hunger < HUNGER_THRESHOLD / 2 || kJoules > ENERGY_THRESHOLD) {
				if (starved != null) {
					starved.setCured();
					// Set isStarving to false
					isStarving = false;
					
					logger.log(person, Level.INFO, 20_000, "Cured of starving (case 2).");
				}
			}

			// If this person's hunger has reached the buffer zone
			else if (hunger < HUNGER_THRESHOLD * 2 || kJoules > ENERGY_THRESHOLD / 4) {
			 
				if (starved == null)
					starved = getStarvationProblem();
				
				if (starved != null) {
					starved.startRecovery();
														
					// Set to not starving
					isStarving = false;
				}
				
				logger.log(person, Level.INFO, 20_000, "Recovering from hunger. "
						 + "  Hunger: " + (int)hunger 
						 + ";  kJ: " + Math.round(kJoules*10.0)/10.0 
						 + ";  Complaint: " + starvation
						 + ";  isStarving: " + isStarving
						 + ";  Status: " + starved.getStateString());	 
			}
		}
		
		else if (starved != null) {
			
			starved.setCured();
			// Set isStarving to false
			isStarving = false;
			
			logger.log(person, Level.INFO, 20_000, "Cured of starving (case 3).");
		}
		
//		else {
//			LogConsolidated.log(logger, Level.INFO, 20_000, sourceName,
//					 person + " was not starving.");
//		}
	}
	
	private HealthProblem getStarvationProblem() {
		for (HealthProblem p: problems) {
			if (p.getType() == starvation.getType()) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Checks if a person is dehydrated
	 * 
	 * @param hunger
	 */
	private void checkDehydration(double thirst) {
		dehydrated = getDehydrationProblem();
	
		// If the person's thirst is greater than dehydrationStartTime
		if (!isDehydrated && thirst > dehydrationStartTime) {
			
			if (!isDehydrated && problems.contains(dehydrated)) {
				addMedicalComplaint(dehydration);
				isDehydrated = true;
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
			}

			else if (dehydrated != null) {
				dehydrated.setCured();
				// Set dehydrated to false
				isDehydrated = false;
				
				logger.log(person, Level.INFO, 0, "Cured of dehydrated (case 1).");
			}
		}

		else if (isDehydrated) {
			
			if (thirst < THIRST_THRESHOLD / 2) {
				
				if (dehydrated != null) {
					
					dehydrated.setCured();
					// Set dehydrated to false
					isDehydrated = false;
					
					logger.log(person, Level.INFO, 0, "Cured of dehydrated (case 2).");
				}
			}
				
			// If this person's thirst has reached the buffer zone 
			else if (thirst < THIRST_THRESHOLD * 2) {
				 
				if (dehydrated == null)
					dehydrated = getDehydrationProblem();
				
				if (dehydrated != null) {
					dehydrated.startRecovery();
					// Set dehydrated to false
					isDehydrated = false;
				}			
				
				logger.log(person, Level.INFO, 20_000, "Recovering from dehydration. "
						 + "  Thirst: " + (int)thirst
						 + ";  Complaint: " + dehydration
						 + ";  isDehydrated: " + isDehydrated
						 + ";  Status: " + dehydrated.getStateString());
			}
		}
		
		else if (dehydrated != null) {
			dehydrated.setCured();
			// Set dehydrated to false
			isDehydrated = false;
			
			logger.log(person, Level.INFO, 0, "Cured of dehydrated (case 3).");
		}
		
//		else {
//			LogConsolidated.log(logger, Level.INFO, 20_000, sourceName,
//					 person + " was not dehydrated.");
//		}
	}

	/**
	 * Gets the dehydrated health problem
	 * 
	 * @return dehydrated
	 */
	private HealthProblem getDehydrationProblem() {
		for (HealthProblem p: problems) {
			if (p.getType() == dehydration.getType()) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Gets the radiation poisoning health problem
	 * 
	 * @return radiationPoisoning
	 */
	private HealthProblem getRadiationProblem() {
		for (HealthProblem p: problems) {
			if (p.getType() == radiationPoisoning.getType()) {
				return p;
			}
		}
		return null;
	}
	
	/**
	 * Sets the person's stress level.
	 * 
	 * @param newStress the new stress level (0.0 to 100.0)
	 */
	public void setStress(double newStress) {
		if (stress != newStress) {
			stress = newStress;
			if (stress > 100D)
				stress = 100D;
			else if (stress < 0D)
				stress = 0D;
			else if (Double.isNaN(stress))
				stress = 0D;
			// person.fireUnitUpdate(UnitEventType.STRESS_EVENT);
		}
	}

//	/**
//	 * Checks if person has an anxiety attack due to too much stress.
//	 * 
//	 * @param time the time passing (millisols)
//	 */
//	private void checkForStressBreakdown(double time) {
//		// Expanded Anxiety Attack into either Panic Attack or Depression
//
//		// a person is limited to have only one of them at a time
//		if (!problems.containsKey(panicAttack) && !problems.containsKey(depression)) {
//
//			// Determine stress resilience modifier (0D - 2D).
//			// 0 (strong) to 1 (weak)
//			double resilienceModifier = (double) (100.0 - resilience * .6 - emotStability * .4) / 100D;
//			double value = stressBreakdownChance / 10D * resilienceModifier;
//
//			if (RandomUtil.lessThanRandPercent(value)) {
//
//				isStressedOut = true;
//
//				double rand = RandomUtil.getRandomDouble(1.0) + inclination_factor;
//
//				if (rand < 0.5) {
//
//					if (panicAttack != null) {
//						if (inclination_factor > -.5)
//							inclination_factor = inclination_factor - .05;
//						addMedicalComplaint(panicAttack);
//						person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
//						LogConsolidated.log(Level.INFO, 0, sourceName,
//								"[" + person.getLocationTag().getLocale() + "] " + name
//										+ " had a panic attack.");
//
//						// the person should be carried to the sickbay at this point
////						person.getMind().getTaskManager().addTask(new RequestMedicalTreatment(person));
//
//					} else
//						logger.log(Level.SEVERE,
//								"Could not find 'Panic Attack' medical complaint in 'conf/medical.xml'");
//
//				} else {
//
//					if (depression != null) {
//						if (inclination_factor < .5)
//							inclination_factor = inclination_factor + .05;
//						addMedicalComplaint(depression);
//						person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
//						LogConsolidated.log(Level.INFO, 0, sourceName,
//								"[" + person.getLocationTag().getLocale() + "] " + name
//										+ " had an episode of depression.");
////						person.getMind().getTaskManager().addTask(new RequestMedicalTreatment(person));
//					} else
//						logger.log(Level.SEVERE, "Could not find 'Depression' medical complaint in 'conf/medical.xml'");
//				}
//			}
//		}
//	}
//
//	/**
//	 * Checks if person has very high fatigue.
//	 * 
//	 * @param time the time passing (millisols)
//	 */
//	private void checkForHighFatigueCollapse(double time) {
//
//		if (!problems.containsKey(highFatigue)) {
//			// Calculate the modifier (from 10D to 0D) Note that the base
//			// high-fatigue-collapse-chance is 5%
//
//			// a person with high endurance will be less likely to be collapse
//			double modifier = (double) (100 - endurance * .6 - strength * .4) / 100D;
//
//			double value = highFatigueCollapseChance / 15D * modifier;
//
//			if (RandomUtil.lessThanRandPercent(value)) {
//				isCollapsed = true;
//
//				if (highFatigue != null) {
//					addMedicalComplaint(highFatigue);
//					person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
////					LogConsolidated.log(Level.INFO, 500, sourceName,
////							"[" + person.getLocationTag().getLocale() + "] " + name
////									+ " collapsed because of high fatigue exhaustion.");
////					person.getMind().getTaskManager().addTask(new RequestMedicalTreatment(person));
//				} else
//					logger.log(Level.SEVERE,
//							"Could not find 'High Fatigue Collapse' medical complaint in 'conf/medical.xml'");
//			}
//		}
//	}

	/**
	 * Checks if person has very high fatigue.
	 * 
	 * @param time the time passing (millisols)
	 */
	private void checkRadiationPoisoning(double time) {
		radiationPoisoned = getRadiationProblem();
		
		if (!isRadiationPoisoned && radiation.isSick()) {
			
			if (!isRadiationPoisoned && !problems.contains(radiationPoisoned)) {
				addMedicalComplaint(radiationPoisoning);
				isRadiationPoisoned = true;
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
				logger.log(person, Level.INFO, 3000, "Collapsed because of radiation poisoning.");
			} 

			else if (radiationPoisoned != null) {
				
				radiationPoisoned.setCured();
				// Set isStarving to false
				isRadiationPoisoned = false;
				
				logger.log(person, Level.INFO, 20_000, "Cured of radiation poisoning (case 1).");
			}		
		}
		
		else if (isRadiationPoisoned) {

			if (!radiation.isSick()) {
				if (radiationPoisoned != null) {
					radiationPoisoned.setCured();
					// Set isRadiationPoisoned to false
					isRadiationPoisoned = false;
					
					logger.log(person, Level.INFO, 20_000, "Cured of radiation poisoning (case 2).");
				}
			}

			// If this person is taking anti-rad meds
			else if (hasMedication(RadioProtectiveAgent.NAME)) {
			 
				if (radiationPoisoned == null)
					radiationPoisoned = getStarvationProblem();
				
				if (radiationPoisoned != null) {
					radiationPoisoned.startRecovery();
														
					// Set to not starving
					isRadiationPoisoned = false;
				}
				
				logger.log(person, Level.INFO, 20_000, "Taking anti-rad meds and recovering from radiation poisoning. "
						 + ";  Complaint: " + radiationPoisoning
						 + ";  isRadiationPoisoned: " + radiationPoisoned
						 + ";  Status: " + radiationPoisoned.getStateString());	 
			}
		}
		
		else if (radiationPoisoned != null) {
			
			radiationPoisoned.setCured();
			// Set isRadiationPoisoned to false
			isRadiationPoisoned = false;
			
			logger.log(person, Level.INFO, 20_000, "Cured of radiationPoisoning (case 3).");
		}
		
//		else {
//			LogConsolidated.log(logger, Level.INFO, 20_000, sourceName,
//					 person + " was not starving.");
//		}
	}

	
	/**
	 * Check for any random ailments that a person comes down with over a period of
	 * time.
	 * 
	 * @param time the time period (millisols).
	 * @return list of ailments occurring. May be empty.
	 */
	private List<Complaint> checkForRandomAilments(ClockPulse pulse) {
		double time  = pulse.getElapsed();
		List<Complaint> result = new CopyOnWriteArrayList<Complaint>();
		List<Complaint> list = medicalManager.getAllMedicalComplaints();
		for (Complaint complaint : list) {
			// Check each possible medical complaint.
			ComplaintType ct = complaint.getType();

			boolean noGo = false;

			if (hasComplaint(complaint)) {

				if (ct == ComplaintType.LACERATION || ct == ComplaintType.BROKEN_BONE
						|| ct == ComplaintType.PULL_MUSCLE_TENDON || ct == ComplaintType.RUPTURED_APPENDIX) {
					if (person.getTaskDescription().toLowerCase().contains("assist")
							|| person.getTaskDescription().toLowerCase().contains("compil")
							|| person.getTaskDescription().toLowerCase().contains("peer")
							|| person.getTaskDescription().toLowerCase().contains("teach")
						|| restingTask
					) {
						// If a person is performing a resting task, then it is impossible to suffer
						// from laceration.
						noGo = true;
					}
				}

				// Check that person does not already have a health problem with this complaint.

				// Note : the following complaints are being initiated in their own methods
				else if (ct == ComplaintType.HIGH_FATIGUE_COLLAPSE || ct == ComplaintType.PANIC_ATTACK
						|| ct == ComplaintType.DEPRESSION
						// Exclude the following 6 environmentally induced complaints
						|| ct == ComplaintType.DEHYDRATION || ct == ComplaintType.STARVATION
						|| ct == ComplaintType.SUFFOCATION || ct == ComplaintType.FREEZING
						|| ct == ComplaintType.HEAT_STROKE || ct == ComplaintType.DECOMPRESSION
						//
						|| ct == ComplaintType.RADIATION_SICKNESS
						// not meaningful to implement suicide until emotional/mood state is in place
						|| ct == ComplaintType.SUICIDE) {
					noGo = true;
				}

				if (!noGo) {
					double probability = complaint.getProbability();
					// Check that medical complaint has a probability > zero
					// since some complaints are secondary complaints and cannot be started
					// by itself
					if (probability > 0D) {
						double taskModifier = 1;
						double tendency = 1;
											
						int msol = pulse.getMarsTime().getMissionSol();

						if (healthLog.get(ct) != null && msol > 3)
							tendency = 0.5 + healthLog.get(ct) / msol;
						else
							tendency = 1.0;
						double immunity = endurance + strength;

						if (immunity > 100)
							tendency = .75 * tendency - .25 * immunity / 100.0;
						else
							tendency = .75 * tendency + .25 * (100 - immunity) / 100.0;

						if (tendency < 0)
							tendency = 0.0001;

						if (tendency > 2)
							tendency = 2;

						if (ct == ComplaintType.PULL_MUSCLE_TENDON 
								|| ct == ComplaintType.BROKEN_BONE) {
							// Note: at the time of workout, pulled muscle can happen
							// TODO: but make a person less prone to pulled muscle while doing other tasks
							// if having consistent workout.
							String taskDes = person.getTaskDescription().toLowerCase();
							String taskPhase = person.getTaskPhase().toLowerCase();
							if (taskPhase.contains("exercising") || taskDes.contains("yoga"))
								taskModifier = 1.1;

							else if (taskPhase.contains("loading") || taskPhase.contains("unloading")) {
								// Doing outdoor field work increases the risk of having pulled muscle.
								taskModifier = 1.2;

								if (agility > 50)
									taskModifier = .75 * taskModifier - .25 * agility / 100.0;
								else
									taskModifier = .75 * taskModifier + .25 * (50 - agility) / 50.0;
							}
							else if (person.getTaskDescription().contains("EVA"))
								// match the uppercase EVA
								taskModifier = 1.3;

							else if (taskDes.contains("digging") || taskDes.contains("mining")
									|| taskDes.contains("excavating")) {
								taskModifier = 1.4;

								int avoidAccident = strength + agility;
								if (avoidAccident > 50)
									taskModifier = .75 * taskModifier - .25 * avoidAccident / 100.0;
								else
									taskModifier = .75 * taskModifier + .25 * (100 - avoidAccident) / 100.0;
							}

						} else if (ct == ComplaintType.MINOR_BURNS// || ct == ComplaintType.MAJOR_BURNS
								|| ct == ComplaintType.BURNS 
								|| ct == ComplaintType.LACERATION) {
							if (agility > 50)
								taskModifier = .75 * taskModifier - .25 * agility / 100.0;
							else
								taskModifier = .75 * taskModifier + .25 * (50 - agility) / 50.0;
						}

						if (taskModifier < 0)
							taskModifier = 0.0001;
						if (taskModifier > 2)
							taskModifier = 2;

						// Randomly determine if person suffers from ailment.
						double rand = RandomUtil.getRandomDouble(100D);
						double timeModifier = time / RANDOM_AILMENT_PROBABILITY_TIME;

						if (rand <= probability * taskModifier * tendency * timeModifier) {
							addMedicalComplaint(complaint);
							result.add(complaint);
						}
					}
				}
			}
		}
		return result;
	}

	private boolean hasComplaint(Complaint c) {
		for (HealthProblem problem : problems) {
			if (problem.getType() == c.getType()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Adds a new medical complaint to the person.
	 * 
	 * @param complaint the new medical complaint
	 */
	public void addMedicalComplaint(Complaint c) {
		if (!hasComplaint(c)) {
			ComplaintType type = c.getType();
			// Create a new health problem
			HealthProblem newProblem = new HealthProblem(type, person);
			problems.add(newProblem);

			// Record this complaint type
			int freq = 0;
			if (healthLog.get(type) != null)
				freq = healthLog.get(type);
			healthLog.put(type, freq + 1);

			// Register this complaint type with a time-stamp
			List<String> clocks = null;
			if (healthHistory.get(type) != null) {
				clocks = healthHistory.get(type);
			} else {
				clocks = new CopyOnWriteArrayList<>();
			}
					
			clocks.add(marsClock.getDateTimeStamp());
			healthHistory.put(type, clocks);

			String phrase = "";

			if (type == ComplaintType.STARVATION)// .equalsIgnoreCase("starvation"))
				phrase = "Starving.";
			else if (type == ComplaintType.DEHYDRATION)
				phrase = "Dehydrated.";
			else if (type == ComplaintType.COLD)
				phrase = "Caught a cold.";
			else if (type == ComplaintType.FLU)
				phrase = "Caught the flu.";
			else if (type == ComplaintType.FEVER)
				phrase = "Having a fever.";
			else if (type == ComplaintType.DECOMPRESSION)
				phrase = "Suffering from decompression.";
			else if (type == ComplaintType.FREEZING)
				phrase = "Freezing.";
			else if (type == ComplaintType.HEAT_STROKE)
				phrase = "Suffering from heat stroke.";
			else if (type == ComplaintType.SUFFOCATION)
				phrase = "Suffocating.";
			else if (type == ComplaintType.LACERATION)
				phrase = "Had a laceration.";
			else if (type == ComplaintType.PULL_MUSCLE_TENDON)
				phrase = "Pulled a muscle.";
			else if (type == ComplaintType.HIGH_FATIGUE_COLLAPSE)
				phrase = "Collapsed under high fatigue.";
//			else
//				phrase = " was complaining about " + n;

			logger.log(person, Level.INFO, 60_000, phrase);

			recalculatePerformance();
			
			// Stop any on-going tasks
//			taskMgr.clearTask();
		}
	}

	/**
	 * Robot consumes given amount of power
	 * 
	 * @param amount    amount of power to consume (in kJ).
	 * @param container unit to get power from
	 * @throws Exception if error consuming power.
	 */
	public void consumePower(double amount, Unit container) {
		if (container == null)
			throw new IllegalArgumentException("container is null");
	}

	/**
	 * Person consumes given amount of packed food
	 * 
	 * @param amount    amount of food to consume (in kg).
	 * @param container unit to get food from
	 * @throws Exception if error consuming food.
	 */
	public void consumePackedFood(double amount, Unit container) {
		Inventory inv = container.getInventory();

		double foodEaten = amount;
		double foodAvailable = inv.getAmountResourceStored(ResourceUtil.foodID, false);

		inv.addAmountDemandTotalRequest(ResourceUtil.foodID, foodEaten);

		if (foodAvailable < 0.01D) {

			logger.log(person, Level.WARNING, 10_000, "Found only " + foodAvailable
							+ " kg preserved food remaining.");
			}

		// if container has less than enough food, finish up all food in the container
		else {

			if (foodEaten > foodAvailable)
				foodEaten = foodAvailable;

			foodEaten = Math.round(foodEaten * 1_000_000.0) / 1_000_000.0;
			// subtract food from container
			inv.retrieveAmountResource(ResourceUtil.foodID, foodEaten);

			inv.addAmountDemand(ResourceUtil.foodID, foodEaten);
		}
	}

	/**
	 * Person consumes given amount of oxygen
	 * 
	 * @param support Life support system providing oxygen.
	 * @param amount  amount of oxygen to consume (in kg)
	 * @return new problem added.
	 * @throws Exception if error consuming oxygen.
	 */
	private boolean lackOxygen(LifeSupportInterface support, double amount) {
		if (amount > 0) {
			if (support == null)
				logger.log(person, Level.SEVERE, 1000, "Had no life support.");
			
			double amountRecieved = support.provideOxygen(amount);

			// Track the amount consumed
			person.addConsumptionTime(ResourceUtil.oxygenID, amountRecieved);
			// TODO: how to model how much oxygen we need properly ?			
			double required = amount / 2D; 

			return checkResourceConsumption(amountRecieved, required, MIN_VALUE, suffocation);
		}
		
		return false;
	}

	/**
	 * This method checks the consume values of a resource. If the actual is less
	 * than the required then a HealthProblem is generated. If the required amount
	 * is satisfied, then any problem is recovered.
	 *
	 * @param actual    The amount of resource provided.
	 * @param require   The amount of resource required.
	 * @param complaint Problem associated to this resource.
	 * @return Has a new problem been added.
	 */
	private boolean checkResourceConsumption(double actual, double required, int bounds, Complaint complaint) {

		boolean newProblem = false;
		if (actual - required > 0.000_1 || required - actual > 0.000_1)
			newProblem = false;
		else if ((bounds == MIN_VALUE) && (actual < required))
			newProblem = true;
		else if ((bounds == MAX_VALUE) && (actual > required))
			newProblem = true;

		if (newProblem) {
			String reading = "";
			String unit = "";
			double decimals = 10.0;
			ComplaintType ct = complaint.getType();
			if (ct == ComplaintType.SUFFOCATION) {
				reading = "Oxygen sensor";
				unit = " kg";
				decimals = 10000.0;
			}
			else if (ct == ComplaintType.DECOMPRESSION) {
				reading = "Pressure sensor";
				unit = " kPa";
			}
			if (ct == ComplaintType.FREEZING) {
				reading = "Low Temperature sensor";
				unit = " C";
			}
			if (ct == ComplaintType.HEAT_STROKE) {
				reading = "High Temperature sensor";
				unit = " C";
			}
			String s = reading + " triggered.   Affected : " + name
					+ "   Actual : " + Math.round(actual*decimals)/decimals + unit
					+ "   Required : " + Math.round(required*decimals)/decimals + unit;
			logger.log(person, Level.SEVERE, 60_000, s);
			
			addMedicalComplaint(complaint);
			person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
		}
		
		else {
			// Is the person suffering from the illness, if so recovery
			// as the amount has been provided
			HealthProblem illness = getProblem(complaint);
			if (illness != null) {
				illness.startRecovery();
				person.fireUnitUpdate(UnitEventType.ILLNESS_EVENT);
			}
		}
		return newProblem;
	}

	private HealthProblem getProblem(Complaint complaint) {
		for (HealthProblem hp: problems) {
			if (hp.getType() == complaint.getType())
				return hp;
		}
		return null;	
	}
	
	/**
	 * Person requires minimum air pressure.
	 * 
	 * @param support  Life support system providing air pressure.
	 * @param pressure minimum air pressure person requires (in Pa)
	 * @return new problem added.
	 */
	private boolean badAirPressure(LifeSupportInterface support, double pressure) {
		return checkResourceConsumption(support.getAirPressure(), pressure, MIN_VALUE, decompression);
	}

	/**
	 * Person requires minimum temperature.
	 * 
	 * @param support     Life support system providing temperature.
	 * @param temperature minimum temperature person requires (in degrees Celsius)
	 * @return new problem added.
	 */
	private boolean badTemperature(LifeSupportInterface support, double minTemperature, double maxTemperature) {
		boolean freeze = checkResourceConsumption(support.getTemperature(), minTemperature, MIN_VALUE, freezing);
		boolean hot = checkResourceConsumption(support.getTemperature(), maxTemperature, MAX_VALUE, heatStroke);
		return freeze || hot;
	}

	/**
	 * Get the details of this Person's death.
	 * 
	 * @return Detail of the death, will be null if person is still alive.
	 */
	public DeathInfo getDeathDetails() {
		return deathDetails;
	}

	/**
	 * Renders this Person dead, creates DeathInfo, and process the changes
	 * 
	 * @param problem      The health problem that contributes to his death.
	 * @param causedByUser True if it's caused by users
	 */
	public void recordDead(HealthProblem problem, Boolean causedByUser, String lastWord) {
		alive = false;
		String reason = TBD;
		if (causedByUser) {
			logger.log(person, Level.WARNING, 0, INSTRUCTED);
			reason = SUICIDE;
		}
		else {
			this.serious = problem;
		}
		
		deathDetails = new DeathInfo(person, problem, reason, lastWord);
		// Declare the person dead
		person.setDeclaredDead();
		// Set unit description to "Dead"
		person.setDescription("Dead");
		// Set work shift to OFF
		person.setShiftType(ShiftType.OFF);
		// Backup the role type
		deathDetails.setRoleType(person.getRole().getType());
		// Relinquish his role
		person.getRole().relinquishOldRoleType();
		// Re-elect any vacated top leaders or chiefs role
		person.getAssociatedSettlement().getChainOfCommand().reelectLeadership(deathDetails.getRoleType());
		// Set the state of the health problem to DEAD
		problem.setState(HealthProblem.DEAD);
		// Remove the person from the airlock's record
		person.getAssociatedSettlement().removeAirlockRecord(person);
		// Set the mind of the person to inactive
		person.getMind().setInactive();
		// Add the person's death info to the postmorten exam waiting list
		medicalManager.addPostmortemExams(person.getAssociatedSettlement(), deathDetails);
		// Remove this person from being a citizen 
		// Note: this affects the num of indoor people as well
		person.getAssociatedSettlement().removeACitizen(person);
	}



	/**
	 * Define the hunger setting for this person
	 * 
	 * @param newHunger New hunger.
	 */
	public void setHunger(double newHunger) {
		if (hunger != newHunger) {
			hunger = newHunger;
		}
	}

	/**
	 * Gets the person's stress level
	 * 
	 * @return stress (0.0 to 100.0)
	 */
	public double getStress() {
		return stress;
	}

	public double getMassPerServing() {
		return foodDryMassPerServing;
	}

	/**
	 * Checks if the person is dead.
	 *
	 * @return true if dead
	 */
	public boolean isDead() {
		return !alive;
	}

	/**
	 * Checks if the person is starving.
	 *
	 * @return true if starving
	 */
	public boolean isStarving() {
		return isStarving;
	}

	public boolean isDeydrated() {
		return isDehydrated;
	}

	/**
	 * Get a string description of the most serious health situation.
	 * 
	 * @return A string containing the current illness if any.
	 */
	public String getHealthSituation() {
		String situation = WELL;
		if (serious != null) {
			if (isDead()) {
				situation = DEAD_COLON + serious.getIllness().getType().toString();
			} else {
				situation = SICK_COLON + serious.toString();
			}
			// else situation = "Not Well";
		}
		return situation;
	}

	/**
	 * Gets the most serious illness.
	 * 
	 * @return most serious illness
	 */
	public Complaint getMostSerious() {
		return serious.getIllness();
	}

	/**
	 * The collection of known Medical Problems.
	 */
	public Collection<HealthProblem> getProblems() {
		return problems;
	}

	/**
	 * Calculate how the most serious problem and other metrics would affect a
	 * person's performance.
	 */
	private void recalculatePerformance() {

		double tempPerformance = 1.0D;

		serious = null;

		// Check the existing problems. find most serious problem and how it
		// affects performance
		Iterator<HealthProblem> iter = problems.iterator();
		while (iter.hasNext()) {
			HealthProblem problem = iter.next();
			double factor = problem.getPerformanceFactor();
			if (factor < tempPerformance) {
				tempPerformance = factor;
			}

			if ((serious == null) || (serious.getIllness().getSeriousness() < problem.getIllness().getSeriousness())) {
				serious = problem;
			}
		}

		// High thirst reduces performance.
		if (thirst > 800D) {
			tempPerformance -= (thirst - 800D) * THIRST_PERFORMANCE_MODIFIER / 2;
		} else if (thirst > 400D) {
			tempPerformance -= (thirst - 400D) * THIRST_PERFORMANCE_MODIFIER / 4;
		}

		// High hunger reduces performance.
		if (hunger > 1600D) {
			tempPerformance -= (hunger - 1600D) * HUNGER_PERFORMANCE_MODIFIER / 2;
		} else if (hunger > 800D) {
			tempPerformance -= (hunger - 800D) * HUNGER_PERFORMANCE_MODIFIER / 4;
		}

		// High fatigue reduces performance.
		if (fatigue > 1500D) {
			tempPerformance -= (fatigue - 1500D) * FATIGUE_PERFORMANCE_MODIFIER / 2;
		} else if (fatigue > 700D) {
			tempPerformance -= (fatigue - 700D) * FATIGUE_PERFORMANCE_MODIFIER / 4;
			// e.g. f = 1000, p = 1.0 - 500 * .0001/4 = 1.0 - 0.05/4 = 1.0 - .0125 ->
			// reduces by 1.25% on each frame
		}

		// High stress reduces performance.
		if (stress > 90D) {
			tempPerformance -= (stress - 90D) * STRESS_PERFORMANCE_MODIFIER / 2;
		} else if (stress > 50D) {
			tempPerformance -= (stress - 50D) * STRESS_PERFORMANCE_MODIFIER / 4;
			// e.g. p = 100 - 10 * .005 /3 = 1 - .05/4 -> reduces by .0125 or 1.25% on each
			// frame
		}

		// High kJoules improves performance and low kJoules hurts performance.
		if (kJoules > 7500) {
			// double old = tempPerformance;
			tempPerformance += (kJoules - 7500) * ENERGY_PERFORMANCE_MODIFIER / 8;
			// LogConsolidated.log(logger, Level.INFO, 200, sourceName,
			// "kJ > 2000 " + old + " --> " + tempPerformance, null);
		} else if (kJoules < 400) {
			// double old = tempPerformance;
			tempPerformance -= 400_000 / kJoules * ENERGY_PERFORMANCE_MODIFIER / 4;
			// LogConsolidated.log(logger, Level.INFO, 200, sourceName,
			// "kJ < 400 " + old + " --> " + tempPerformance, null);
		}

		setPerformanceFactor(tempPerformance);

	}

	/**
	 * Give the status of a person's hunger level
	 *
	 * @param hunger
	 * @return status
	 */
	public static String getHungerStatus(double hunger, double energy) {
		String status = "N/A";
		if (hunger < 50 && energy > 19000) // Full
			status = Msg.getString("PersonTableModel.column.energy.level1");
		else if (hunger < 250 && energy > 14000) // Satisfied
			status = Msg.getString("PersonTableModel.column.energy.level2");
		else if (hunger < 500 && energy > 9000) // Comfy
			status = Msg.getString("PersonTableModel.column.energy.level3");
		else if (hunger < 750 && energy > 4000) // Adequate
			status = Msg.getString("PersonTableModel.column.energy.level4");
		else if (hunger < 900 && energy > 1500) // Rumbling
			status = Msg.getString("PersonTableModel.column.energy.level5");
		else if (hunger < 1200 && energy > 750) // Ravenous
			status = Msg.getString("PersonTableModel.column.energy.level6");		
		else // Famished
			status = Msg.getString("PersonTableModel.column.energy.level7");
		return status;
	}

	/**
	 * Give the status of a person's water level
	 *
	 * @param water
	 * @return status
	 */
	public static String getThirstyStatus(double thirst) {
		String status = "N/A";
		if (thirst < 150)
			status = Msg.getString("PersonTableModel.column.water.level1");
		else if (thirst < 500)
			status = Msg.getString("PersonTableModel.column.water.level2");
		else if (thirst < 1000)
			status = Msg.getString("PersonTableModel.column.water.level3");
		else if (thirst < 1600) 
			// Note : Use getDehydrationStartTime()
			status = Msg.getString("PersonTableModel.column.water.level4");
		else
			status = Msg.getString("PersonTableModel.column.water.level5");
		return status;
	}
	
	/**
	 * Give the status of a person's fatigue level
	 *
	 * @param fatigue
	 * @return status
	 */
	public static String getFatigueStatus(double value) {
		String status = "N/A";
		if (value < 500)
			status = Msg.getString("PersonTableModel.column.fatigue.level1");
		else if (value < 800)
			status = Msg.getString("PersonTableModel.column.fatigue.level2");
		else if (value < 1200)
			status = Msg.getString("PersonTableModel.column.fatigue.level3");
		else if (value < 1600)
			status = Msg.getString("PersonTableModel.column.fatigue.level4");
		else
			status = Msg.getString("PersonTableModel.column.fatigue.level5");
		return status;
	}

	/**
	 * Give the status of a person's stress level
	 *
	 * @param hunger
	 * @return status
	 */
	public static String getStressStatus(double value) {
		String status = "N/A";
		if (value < 10)
			status = Msg.getString("PersonTableModel.column.stress.level1");
		else if (value < 40)
			status = Msg.getString("PersonTableModel.column.stress.level2");
		else if (value < 75)
			status = Msg.getString("PersonTableModel.column.stress.level3");
		else if (value < 95)
			status = Msg.getString("PersonTableModel.column.stress.level4");
		else
			status = Msg.getString("PersonTableModel.column.stress.level5");
		return status;
	}

	/**
	 * Give the status of a person's hunger level
	 *
	 * @param hunger
	 * @return status
	 */
	public static String getPerformanceStatus(double value) {
		String status = "N/A";
		if (value > .95)
			status = Msg.getString("PersonTableModel.column.performance.level1");
		else if (value > .75)
			status = Msg.getString("PersonTableModel.column.performance.level2");
		else if (value > .50)
			status = Msg.getString("PersonTableModel.column.performance.level3");
		else if (value > .25)
			status = Msg.getString("PersonTableModel.column.performance.level4");
		else 
			status = Msg.getString("PersonTableModel.column.performance.level5");
		// logger.info(" Perf : " + Math.round(value) + " ; status : " +
		// status);
		return status;
	}

	/**
	 * Checks if the person has any serious medical problems.
	 * 
	 * @return true if serious medical problems
	 */
	public boolean hasSeriousMedicalProblems() {
		boolean result = false;
		Iterator<HealthProblem> meds = getProblems().iterator();
		while (meds.hasNext()) {
			if (meds.next().getIllness().getSeriousness() >= 50)
				result = true;
		}
		return result;
	}

	public boolean isFit() {
        if (fatigue > 1000 || stress > 50 || hunger > 1000 || thirst > 750 || kJoules < 1000
	        	|| hasSeriousMedicalProblems())
        	return false;
        
        return true;
	}
	
	/**
	 * Compute the fitness level
	 * 
	 * @return
	 */
	public int computeFitnessLevel() {
		int level = 5;
		if (hasSeriousMedicalProblems()) {
			return 0;
		}
		
        if (fatigue > 300 || stress > 5 || hunger > 200 || thirst > 150 || kJoules < 15000)
        	level = 4;
        else if (fatigue > 500 || stress > 20 || hunger > 400 || thirst > 300 || kJoules < 10000)
        	level = 3;
        else if (fatigue > 800 || stress > 40 || hunger > 600 || thirst > 500 || kJoules < 5000)
        	level = 2;
        else if (fatigue > 1200 || stress > 65 || hunger > 1000 || thirst > 750 || kJoules < 2500)
        	level = 1;
        else if (fatigue > 1500 || stress > 90 || hunger > 1400 || thirst > 1100 || kJoules < 500)
        	level = 0;
        
        return level;
	}
	
	/**
	 * Gets a list of medication affecting the person.
	 * 
	 * @return list of medication.
	 */
	public List<Medication> getMedicationList() {
		return new CopyOnWriteArrayList<Medication>(medicationList);
	}

	/**
	 * Checks if the person is affected by the given medication.
	 * 
	 * @param medicationName the name of the medication.
	 * @return true if person is affected by it.
	 */
	public boolean hasMedication(String medicationName) {
		if (medicationName == null)
			throw new IllegalArgumentException("medicationName is null");

		boolean result = false;

		Iterator<Medication> i = medicationList.iterator();
		while (i.hasNext()) {
			if (medicationName.equals(i.next().getName()))
				result = true;
		}

		return result;
	}

	/**
	 * Adds a medication that affects the person.
	 * 
	 * @param medication the medication to add.
	 */
	public void addMedication(Medication medication) {
		if (medication == null)
			throw new IllegalArgumentException("medication is null");
		medicationList.add(medication);
	}

	/**
	 * Gets the oxygen consumption rate per Sol.
	 * 
	 * @return oxygen consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getOxygenConsumptionRate() {
		return o2_consumption;
	}

	/**
	 * Gets the water consumption rate per Sol.
	 * 
	 * @return water consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getWaterConsumptionRate() {
		return h2o_consumption;
	}

	public double getWaterConsumedPerServing() {
		return waterConsumedPerServing;
	}

	/**
	 * Gets the food consumption rate per Sol.
	 * 
	 * @return food consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getFoodConsumptionRate() {
		return food_consumption;
	}

	/**
	 * Gets the dessert consumption rate per Sol.
	 * 
	 * @return dessert consumed (kg/Sol)
	 * @throws Exception if error in configuration.
	 */
	public static double getDessertConsumptionRate() {
		return dessert_consumption;
	}

	/**
	 * Gets the person with this physical condition
	 * 
	 * @return
	 */
	public Person getPerson() {
		return person;
	}

	public RadiationExposure getRadiationExposure() {
		return radiation;
	}


	public double getBodyMassDeviation() {
		return bodyMassDeviation;
	}

	public boolean isStressedOut() {
		return isStressedOut;
	}

	public boolean isRadiationPoisoned() {
		return isRadiationPoisoned;
	}

	public double[] getMusculoskeletal() {
		return musculoskeletal;
	}

	public void setMuscularSystem(double[] value) {
		musculoskeletal = value;
	}

	public void workOut() {
		musculoskeletal[0] = musculoskeletal[0] + .01; // pain tolerance
		musculoskeletal[2] = musculoskeletal[0] + .1; // muscle soreness
	}

	public double getStarvationStartTime() {
		return starvationStartTime;
	}

	
	public double getDehydrationStartTime() {
		return dehydrationStartTime;
	}
	
	/**
	 * Checks if it passes the hunger threshold
	 * 
	 * @return
	 */
	public boolean isHungry() {
		return hunger > HUNGER_THRESHOLD || kJoules < ENERGY_THRESHOLD / 2.5;
	}
	
	/**
	 * Checks if it passes the thirst threshold
	 * 
	 * @return
	 */
	public boolean isThirsty() {
		return thirst > THIRST_THRESHOLD * 2;
	}
	
	/**
	 * Checks if it passes the fatigue threshold
	 * 
	 * @return
	 */
	public boolean isSleepy() {
		return fatigue > FATIGUE_THRESHOLD;
	}
	
	/**
	 * Checks if it passes the stress threshold
	 * 
	 * @return
	 */
	public boolean isStressed() {
		return stress > STRESS_THRESHOLD;
	}
	
	public double getStrengthMod() {
		return (endurance * .6 - strength * .4) / 100D;
	}
	
	public double getRemainingPrebreathingTime() {
		return remainingPrebreathingTime;
	}
	
	public void reduceRemainingPrebreathingTime(double time) {
		remainingPrebreathingTime -= time;
	}
	
	public boolean isDonePrebreathing() {
		if (remainingPrebreathingTime <= 0)
			return true;
		return false;
	}
	
	public boolean isThreeQuarterDonePrebreathing() {
		if (remainingPrebreathingTime > 0 && remainingPrebreathingTime <= .25 * STANDARD_PREBREATHING_TIME)
			return true;
		return false;
	}
	
	public void resetRemainingPrebreathingTime() {
		remainingPrebreathingTime = STANDARD_PREBREATHING_TIME + RandomUtil.getRandomInt(-5, 5);
	}
	
	public static void initializeInstances(Simulation s, MasterClock c0, MarsClock c1, MedicalManager m) {
		sim = s;
		masterClock = c0;
		marsClock = c1;
		medicalManager = m;
	}
	
	public void reinit() {
		circadian = person.getCircadianClock();
//		taskMgr = person.getMind().getTaskManager();
		naturalAttributeManager = person.getNaturalAttributeManager();
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {

		deathDetails = null;
		// problems.clear();
		problems = null;
		serious = null;
		person = null;

		radiation = null;
		circadian = null;
//		taskMgr = null;
		starved = null;
		dehydrated = null;
		medicalManager = null;
		marsClock = null;
		dehydration = null;
		starvation = null;
		freezing = null;
		heatStroke = null;
		decompression = null;
		suffocation = null;
//		depression = null;
//		panicAttack = null;
//		highFatigue = null;
		radiationPoisoning = null;

		// if (medicationList != null) medicationList.clear();
		medicationList = null;
//		allMedicalComplaints = null;

	}
}
