/*
 * Mars Simulation Project
 * Farming.java
 * @date 2022-08-10
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function.farming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.data.SolMetricDataLogger;
import org.mars_sim.msp.core.data.SolSingleMetricDataLogger;
import org.mars_sim.msp.core.food.FoodType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.TendGreenhouse;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.Worker;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.FunctionSpec;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.HouseKeeping;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.PowerMode;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The Farming class is a building function for greenhouse farming.
 */

public class Farming extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(Farming.class.getName());

	private static final int MAX_NUM_SOLS = 14;
	private static final int MAX_SAME_CROPTYPE = 3;

	private static final int CROP_WASTE_ID = ResourceUtil.cropWasteID;
	private static final int SOIL_ID = ResourceUtil.soilID;
	private static final int FERTILIZER_ID = ResourceUtil.fertilizerID;
	
	public static final double LOW_AMOUNT_TISSUE_CULTURE = 0.5;
	public static final double CROP_AMOUNT_FOR_TISSUE_EXTRACTION = 0.5;
	/** The average temperature tolerance of a crop [in C]. */
	private static final double T_TOLERANCE = 3D;
	/** The amount of crop tissue culture needed for each square meter of growing area. */
	private static final double TISSUE_PER_SQM = .0005; // 1/2 gram (arbitrary)
	public static final double STANDARD_AMOUNT_TISSUE_CULTURE = 0.05;
	private static final double MIN  = .00001D;// 0.0000000001;
	
	private static final String CROPS = "crops";
	private static final String POWER_GROWING_CROP = "power-growing-crop";
	private static final String POWER_SUSTAINING_CROP = "power-sustaining-crop";
	private static final String GROWING_AREA = "growing-area";

	public static final String TISSUE = " " + FoodType.TISSUE.getName();

	private static final String [] INSPECTION_LIST = {"Environmental Control System",
													  "HVAC System", "Waste Disposal System",
													  "Containment System", "Any Traces of Contamination",
													  "Foundation",	"Structural Element", "Thermal Budget",
													  "Water and Irrigation System"};
	private static final String [] CLEANING_LIST = {"Floor", "Curtains", "Canopy", "Equipment",
													"Pipings", "Trays", "Valves"};

	/** The mission sol. */
//	private int currentSol = 1;
	/** The default number of crops allowed by the building type. */
	private int defaultCropNum;
	/** The id of a crop in this greenhouse. */
	private int identifer;
	/** The number of crops to plant. */
	private int numCrops2Plant;

	private double powerGrowingCrop;
	private double powerSustainingCrop;
	private double maxGrowingArea;
	private double remainingGrowingArea;
	/** The cumulative time spent in this greenhouse. */
	private double cumulativeWorkTime;	
	
	public enum Aspect {
	    ATTRACTIVENESS,
	    CLEANINESS,
	    MANAGEMENT,
	    SCIENCE,
	    UTILIZATION
	  }
	
	/** List of crop types in queue */
	private List<String> cropListInQueue;
	/** List of crops the greenhouse is currently growing */
	private List<Crop> cropList;
	/** A map of all the crops ever planted in this greenhouse. */
	private Map<Integer, String> cropHistory;
	/** The attribute scores map for this greenhouse. */
	private Map<Aspect, Double> attributes;
	/** The crop usage on each crop in this facility [kg/sol]. */
	private Map<String, SolMetricDataLogger<Integer>> cropUsage;
	

	/** The daily water usage in this facility [kg/sol]. */
	private SolSingleMetricDataLogger dailyWaterUsage;

	/** The last crop the workers were tending. */
	private Crop needyCropCache;
	
	private Research lab;
	private HouseKeeping houseKeeping;

	/**
	 * Constructor.
	 *
	 * @param building the building the function is for.
	 * @param spec Spec of the farming function
	 * @throws BuildingException if error in constructing function.
	 */
	public Farming(Building building, FunctionSpec spec) {
		// Use Function constructor.
		super(FunctionType.FARMING, spec, building);
		
		// Initialize the attribute scores map
		initAttributeScores();

		identifer = 0;

		houseKeeping = new HouseKeeping(CLEANING_LIST, INSPECTION_LIST);

		cropListInQueue = new ArrayList<>();
		cropList = new ArrayList<>();
		cropHistory = new HashMap<>();
		dailyWaterUsage = new SolSingleMetricDataLogger(MAX_NUM_SOLS);
		cropUsage = new HashMap<>();
		
		defaultCropNum = spec.getIntegerProperty(CROPS);

		powerGrowingCrop = spec.getDoubleProperty(POWER_GROWING_CROP);
		powerSustainingCrop = spec.getDoubleProperty(POWER_SUSTAINING_CROP);
		maxGrowingArea = spec.getDoubleProperty(GROWING_AREA);
		remainingGrowingArea = maxGrowingArea;

		Map<CropSpec,Integer> alreadyPlanted = new HashMap<>();
		for (int x = 0; x < defaultCropNum; x++) {
			CropSpec cropType = pickACrop(alreadyPlanted);
			if (cropType == null) {
				break;// for avoiding NullPointerException during maven test
			}
			else {
				Crop crop = plantACrop(cropType, true, 0);
				cropList.add(crop);
				cropHistory.put(crop.getIdentifier(), cropType.getName());
				building.getSettlement().fireUnitUpdate(UnitEventType.CROP_EVENT, crop);

				alreadyPlanted.merge(cropType, 1, Integer::sum);
			}
		}
	}
	
	/**
	 * Initializes the attribute scores.
	 */
	private void initAttributeScores() {
		attributes = new HashMap<>(); 
		attributes.put(Aspect.ATTRACTIVENESS, .5);
		attributes.put(Aspect.CLEANINESS, .5);
		attributes.put(Aspect.MANAGEMENT, .5);
		attributes.put(Aspect.SCIENCE, .5);
		attributes.put(Aspect.UTILIZATION, .5);
	}

	/**
	 * Updates an attribute score.
	 * 
	 * @param aspect
	 * @param amount
	 */
	public void updateAttribute(Aspect aspect, double amount) {
		attributes.computeIfPresent(aspect, (k, v) -> v = adjust(v, amount));
	}
	
	/**
	 * Adjusts an attribute score.
	 * 
	 * @param score
	 * @param change
	 * @return
	 */
	private double adjust(double score, double change) {
		double result = score + change;
		if (result > 1.0)
			return 1.0;
		else if (result < 0.0)
			return 0.0;
		return result;
	}
	
	/**
	 * Picks a crop. Ensure this crop is not being grown more than MAX_SAME_CROPTYPE.
	 *
	 * @param isStartup - true if it is called at the start of the sim
	 * @return {@link CropSpec}
	 */
	private CropSpec pickACrop(Map<CropSpec, Integer> cropsPlanted) {
		CropSpec ct = null;
		boolean cropAlreadyPlanted = true;
		// TODO: at the start of the sim, choose only from a list of staple food crop
		int totalCropTypes = cropConfig.getNumCropTypes();

		// Attempt to find a unique crop but limit the number of attempts
		int attempts = 0;
		while ((attempts < totalCropTypes) && cropAlreadyPlanted) {
			ct = cropConfig.getRandomCropType();

			attempts++;
			cropAlreadyPlanted = cropsPlanted.getOrDefault(ct, 0) > MAX_SAME_CROPTYPE;
		}
		return ct;
	}

	/**
	 * Chooses a matured crop and extract samples for growing tissues.
	 * 
	 * @return
	 */
	public String chooseCrop2Extract(double amount) {
		List<CropSpec> list = new ArrayList<>(cropConfig.getCropTypes());
		Collections.shuffle(list);

		list = list.stream()
				.filter(c -> building.getSettlement()
				.getAllAmountResourceOwned(c.getCropID()) > amount)
				.collect(Collectors.toList());
			
//		logger.log(getBuilding(), Level.INFO, 0, "list: " + list);
		
		List<AmountResource> tissues = new ArrayList<>();
		
		for (CropSpec c: list) {
			String cropName = c.getName();
			String tissueName = cropName + Farming.TISSUE;
			AmountResource tissue = ResourceUtil.findAmountResource(tissueName);	
			double amountTissue = building.getSettlement().getAmountResourceStored(tissue.getID());
			if (amountTissue < LOW_AMOUNT_TISSUE_CULTURE)
				tissues.add(tissue);
		}
	
//		logger.log(getBuilding(), Level.INFO, 0, "tissues: " + tissues);
		
		String cropName = null;

		for (AmountResource ar: tissues) {
			if (cropName == null) {
				String tissueName = ar.getName();
				cropName = tissueName.replace(" tissue", "");
				int cropId = ResourceUtil.findIDbyAmountResourceName(cropName);
				double amountCrop = building.getSettlement().getAmountResourceStored(cropId);
				if (amountCrop > CROP_AMOUNT_FOR_TISSUE_EXTRACTION) {
					building.getSettlement().retrieveAmountResource(cropId, CROP_AMOUNT_FOR_TISSUE_EXTRACTION);
					break;
				}
				else
					cropName = null;
			}
		}
	
		if (cropName != null) {
			logger.log(getBuilding(), Level.INFO, 0, "cropName: " + cropName);
			return cropName;
		}
		else {	
			String selectedTissueName = null;
			int selectedTissueid = 0;
			double selectedTissueAmount = 0;
			
			for (AmountResource ar: tissues) {
				double tissueAmount = building.getSettlement().getAmountResourceStored(ar.getID());
				if (tissueAmount <= selectedTissueAmount) {
					selectedTissueAmount = tissueAmount;
					selectedTissueName = ar.getName();
					selectedTissueid = ar.getID();
				}
			}
			
			if (selectedTissueName != null) {
				building.getSettlement().retrieveAmountResource(selectedTissueid, STANDARD_AMOUNT_TISSUE_CULTURE);
				logger.log(getBuilding(), Level.INFO, 0, "selectedTissueName: " + selectedTissueName);
				return selectedTissueName.replace(" tissue", "");
			}
		}
		
		return null;
	}
	
	/**
	 * Selects a crop currently having the highest value point (VP).
	 *
	 * @return CropType
	 */
	public CropSpec selectVPCrop() {

		CropSpec no1Crop = null;
		CropSpec no2Crop = null;
		CropSpec chosen = null;
		double no1CropVP = 0;
		double no2CropVP = 0;

		for (CropSpec c : cropConfig.getCropTypes()) {
			double cropVP = getCropValue(ResourceUtil.findAmountResource(c.getName()));
			if (cropVP >= no1CropVP) {
				if (no1Crop != null) {
					no2CropVP = no1CropVP;
					no2Crop = no1Crop;
				}
				no1CropVP = cropVP;
				no1Crop = c;

			} else if (cropVP > no2CropVP) {
				no2CropVP = cropVP;
				no2Crop = c;
			}
		}

		String lastCT = null;
		String last2CT = null;
		boolean compareVP = false;

		int size = cropHistory.size();
		if (size > 1) {
			// get the last two planted crops
			last2CT = cropHistory.get(size - 2);
			lastCT = cropHistory.get(size - 1);

			if (no1Crop.getName().equalsIgnoreCase(last2CT) || no1Crop.getName().equalsIgnoreCase(lastCT)) {
				// if highestCrop has already been selected once

				if (last2CT != null && lastCT != null && last2CT.equals(lastCT)) {
					// Note : since the highestCrop has already been chosen previously,
					// should not choose the same crop type again
					// compareVP = false;
					chosen = no2Crop;
				}

				else
					compareVP = true;
			}

			else if (no2Crop != null && 
					(no2Crop.getName().equalsIgnoreCase(last2CT) 
							|| no2Crop.getName().equalsIgnoreCase(lastCT))) {
				// if secondCrop has already been selected once

				if (last2CT != null && lastCT != null && last2CT.equals(lastCT)) {
					// since the secondCrop has already been chosen twice,
					// should not choose the same crop type again
					// compareVP = false;
					chosen = no1Crop;
				}

				else
					compareVP = true;
			}

		}

		else if (size == 1) {
			lastCT = cropHistory.get(0);

			if (lastCT != null) {
				// if highestCrop has already been selected for planting last time,
				if (no1Crop.getName().equalsIgnoreCase(lastCT))
					compareVP = true;
			}
		}

		if (compareVP) {
			// compare secondVP with highestVP
			// if their VP are within 15%, toss a dice
			// if they are further apart, should pick highestCrop
			// if ((highestVP - secondVP) < .15 * secondVP)
			if ((no2CropVP / no1CropVP) > .85) {
				int rand = RandomUtil.getRandomInt(0, 1);
				if (rand == 0)
					chosen = no1Crop;
				else
					chosen = no2Crop;
			} else
				chosen = no2Crop;
		} else
			chosen = no1Crop;

		boolean flag = hasTooMany(chosen);

		while (flag) {
			chosen = cropConfig.getRandomCropType();
			flag = hasTooMany(chosen);
		}

		return chosen;
	}

	/**
	 * Checks if the greenhouse has too many crops of this type.
	 *
	 * @param name
	 * @return
	 */
	private boolean hasTooMany(CropSpec name) {
		int num = 0;
		for (Crop c : cropList) {
			if (c.getCropSpec().equals(name))
				num++;
		}

        return num > MAX_SAME_CROPTYPE;
    }

	private double getCropValue(AmountResource resource) {
		return building.getSettlement().getGoodsManager().getGoodValuePoint(resource.getID());
	}

	/**
	 * Plants a new crop.
	 *
	 * @param cropType
	 * @param isStartup             - true if it's at the start of the sim
	 * @param designatedGrowingArea
	 * @return Crop
	 */
	private Crop plantACrop(CropSpec cropType, boolean isStartup, double designatedGrowingArea) {
		// Implement new way of calculating amount of food in kg,
		// accounting for the Edible Biomass of a crop
		// edibleBiomass is in [ gram / m^2 / day ]
		double edibleBiomass = cropType.getEdibleBiomass();
		// growing-time is in millisol vs. growingDay is in sol
		// double growingDay = cropType.getGrowingTime() / 1000D ;
		double cropArea = 0;
		if (remainingGrowingArea <= 1D) {
			cropArea = remainingGrowingArea;
		} else if (designatedGrowingArea != 0) {
			cropArea = designatedGrowingArea;
		} else { // if (remainingGrowingArea > 1D)
			cropArea = maxGrowingArea / (double) defaultCropNum;
		}

		// Sets aside some areas
		remainingGrowingArea = remainingGrowingArea - cropArea;

		if (remainingGrowingArea < 0)
			remainingGrowingArea = 0;

		// Note edible-biomass is [ gram / m^2 / day ]
		double dailyMaxHarvest = edibleBiomass / 1000D * cropArea;
//		 logger.info("max possible daily harvest on " + cropType.getName() + " : " +
//		 Math.round(dailyMaxHarvest*100.0)/100.0 + " kg per sol");

		double percentAvailable = 0;

		if (!isStartup) {
			// Use tissue culture
			percentAvailable = useTissueCulture(cropType, cropArea);
			// Add fertilizer to the soil for the new crop
			provideFertilizer(cropArea);
			// Replace some amount of old soil with new soil
			provideNewSoil(cropArea);

		}

		Crop crop = new Crop(identifer++, cropType, cropArea, dailyMaxHarvest,
				this, isStartup, percentAvailable);

		return crop;
	}

	/**
	 * Retrieves new soil when planting new crop.
	 * 
	 * @param cropArea
	 */
	private void provideNewSoil(double cropArea) {
		double rand = RandomUtil.getRandomDouble(0.8, 1.2);

		double amount = Crop.NEW_SOIL_NEEDED_PER_SQM * cropArea * rand;

		if (amount > MIN) {
			// Collect some old crop and turn them into crop waste 
			store(amount, CROP_WASTE_ID, "Farming::provideNewSoil");
			// Note: adjust how much new soil is needed to replenish the soil bed
			retrieve(amount, SOIL_ID, true);
		}
	}

	/**
	 * Retrieves the fertilizer and add to the soil when planting the crop.
	 * 
	 * @param cropArea
	 */
	private void provideFertilizer(double cropArea) {
		double rand = RandomUtil.getRandomDouble(2);
		double amount = Crop.FERTILIZER_NEEDED_IN_SOIL_PER_SQM * cropArea * rand;
		if (amount > MIN)
			retrieve(amount, FERTILIZER_ID, true);
	}

	/**
	 * Uses available tissue culture to shorten Germinating Phase when planting the
	 * crop.
	 *
	 * @parama cropType
	 * @param cropArea
	 * @return percentAvailable
	 */
	private double useTissueCulture(CropSpec cropType, double cropArea) {
		double percent = 0;

		double requestedAmount = cropArea * cropType.getEdibleBiomass() * TISSUE_PER_SQM;

		String tissueName = cropType.getName() + TISSUE;
		// String name = Conversion.capitalize(cropType.getName()) + TISSUE_CULTURE;
		int tissueID = ResourceUtil.findIDbyAmountResourceName(tissueName);

		boolean available = false;

		try {
			double amountStored = building.getSettlement().getAmountResourceStored(tissueID);

			if (amountStored < MIN) {
				logger.log(building, Level.INFO, 1000, "Running out of " + tissueName + ".");
				percent = 0;
			}

			else if (amountStored < requestedAmount) {
				available = true;
				percent = amountStored / requestedAmount * 100D;
				requestedAmount = amountStored;
				logger.log(building, Level.INFO, 1000, Math.round(requestedAmount * 100.0) / 100.0
							+ " kg " + tissueName + " was partially available.");
			}

			else {
				available = true;
				percent = 100D;
				logger.log(building, Level.INFO, 1000,
						+ Math.round(requestedAmount * 100.0) / 100.0 + " kg "
				+ tissueName + " was fully available.");
			}

			if (available) {
				building.getSettlement().retrieveAmountResource(tissueID, requestedAmount);
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

		return percent;

	}

	/**
	 * Gets a collection of the CropType.
	 *
	 * @return Collection of CropType
	 */
	public List<String> getCropListInQueue() {
		return cropListInQueue;
	}

	/**
	 * Adds a cropType to the crop queue.
	 *
	 * @param cropType
	 */
	public void addCropListInQueue(String n) {
		if (n != null) {
			cropListInQueue.add(n);
		}
	}

	/**
	 * Deletes a cropType to cropListInQueue.
	 *
	 * @param cropType
	 */
	public void deleteACropFromQueue(int index, String n) {
		// cropListInQueue.remove(index);
		// Safer removal than cropListInQueue.remove(index)
		int size = cropListInQueue.size();
		if (size > 0) {
			Iterator<String> j = cropListInQueue.iterator();
			int i = 0;
			while (j.hasNext()) {
				String name = j.next();
				if (i == index) {
					if (!n.equals(name))
						logger.severe(building,
								"The crop queue encountered a problem removing a crop");
					else {
						j.remove();
						break; // remove the first entry only
					}
				}
				i++;
			}
		}
	}

	/**
	 * Gets the value of the function for a named building.
	 *
	 * @param type the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function. Called by BuildingManager.java
	 *         getBuildingValue()
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {

		double result = 0D;

		// Demand is farming area (m^2) needed to produce food for settlement
		// population.
		double requiredFarmingAreaPerPerson = cropConfig.getFarmingAreaNeededPerPerson();
		double demand = requiredFarmingAreaPerPerson * settlement.getNumCitizens();

		// Supply is total farming area (m^2) of all farming buildings at settlement.
		double supply = 0D;
		boolean removedBuilding = false;
		List<Building> buildings = settlement.getBuildingManager().getBuildings(FunctionType.FARMING);
		for (Building building : buildings) {
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(type) && !removedBuilding) {
				removedBuilding = true;
			} else {
				Farming farmingFunction = building.getFarming();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += farmingFunction.getGrowingArea() * wearModifier;
			}
		}

		// Modify result by value (VP) of food at the settlement.
		double foodValue = settlement.getGoodsManager().getGoodValuePoint(ResourceUtil.foodID);

		result = (demand / (supply + 1D)) * foodValue;

		// NOTE: investigating if other food group besides food should be added as well

		return result;
	}

	/**
	 * Checks if farm currently requires work.
	 *
	 * @return true if farm requires work
	 */
	public boolean requiresWork() {
		for (Crop c : cropList) {
			if (c.requiresWork()) {
				return true;
			}
		}
		return getNumCrops2Plant() > 0;
	}

	/**
	 * Adds work time to the crops current phase.
	 *
	 * @param workTime - Work time to be added (millisols)
	 * @param h        - an instance of TendGreenhouse
	 * @param worker     - a person or bot
	 * @return workTime remaining after working on crop (millisols)
	 * @throws Exception if error adding work.
	 */
	public double addWork(double workTime, Worker worker, Crop needyCrop) {
		return needyCrop.addWork(worker, workTime);
	}
	
	public boolean requiresWork(Crop needyCrop) {
		return needyCrop.requiresWork();
	}

	public Crop getNeedyCropCache() {
		return needyCropCache;
	}
	
	/**
	 * Gets a crop that needs work time.
	 *
	 * @param currentCrop
	 * @return crop or null if none found.
	 */
	public Crop getNeedyCrop() {
		Crop currentCrop = needyCropCache;
		
		if (cropList == null || cropList.isEmpty())
			return null;

		int rand = RandomUtil.getRandomInt(3);
			
		if (rand == 0) {
			Crop mostNeedyCrop = null;
			double mostWork = 0; 
			
			// Pick the crop that requires most work
			for (Crop c : cropList) {
				if (c.requiresWork() && c.getCurrentWorkRequired() > mostWork) {
					mostNeedyCrop = c;
					mostWork = c.getCurrentWorkRequired();
				}
			}
			
			return mostNeedyCrop;
		}
		// Half the chance it will pick the current crop to work on
		else if ((rand == 1 || rand == 2) && currentCrop != null
			&& currentCrop.requiresWork()) {
			// Pick the current crop again unless it no longer requires work
			return currentCrop;
		}
		
		else {
			Crop nextCrop = null;
			
			// Pick another crop that requires work
			List<Crop> needyCrops = new ArrayList<>();
			for (Crop c : cropList) {
				if (c.requiresWork()) {
					needyCrops.add(c);
				}
			}
	
			if (!needyCrops.isEmpty()) {
				nextCrop = needyCrops.get(RandomUtil.getRandomInt(0,
									needyCrops.size() - 1));
			}
	
			needyCropCache = nextCrop;
			return nextCrop;
		}
	}

	/**
	 * Gets the number of farmers currently working at the farm.
	 *
	 * @return number of farmers
	 */
	public int getFarmerNum() {
		int result = 0;

		if (building.hasFunction(FunctionType.LIFE_SUPPORT)) {
			LifeSupport lifeSupport = building.getLifeSupport();
			for(Person p : lifeSupport.getOccupants()) {
				Task task = p.getMind().getTaskManager().getTask();
				if (task instanceof TendGreenhouse)
					result++;
			}
		}

		return result;
	}

	/**
	 * Time passing for the building.
	 *
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
			// check for the passing of each day
			if (pulse.isNewSol()) {

//				currentSol = pulse.getMarsTime().getMissionSol();
				
				// Gradually reduce aspect score by default
				for (Aspect aspect: attributes.keySet()) {
					updateAttribute(aspect, -0.01);
				}
				
				// Reset the cleaning
				houseKeeping.resetCleaning();

				// Inspect every 2 days
				if ((pulse.getMarsTime().getMissionSol() % 2) == 0)
				{
					houseKeeping.resetInspected();
				}

				// Reset cumulativeDailyPAR
				for (Crop c : cropList)
					c.resetPAR();
				// Note: will need to limit the size of the other usage maps
			}

			// Determine the production level.
			double productionLevel = 0D;
			if (building.getPowerMode() == PowerMode.FULL_POWER)
				productionLevel = 1D;
			else if (building.getPowerMode() == PowerMode.POWER_DOWN)
				productionLevel = .5D;

			double solarIrradiance = surface.getSolarIrradiance(building.getSettlement().getCoordinates());
			double greyFilterRate = building.getSettlement().getGreyWaterFilteringRate();

			// Compute the effect of the temperature
			double temperatureModifier = 1D;
			double tempNow = building.getCurrentTemperature();
			double tempInitial = building.getInitialTemperature();
			if (tempNow > (tempInitial + T_TOLERANCE))
				temperatureModifier = tempInitial / tempNow;
			else if (tempNow < (tempInitial - T_TOLERANCE))
				temperatureModifier = tempNow / tempInitial;

			// Call timePassing on each crop.
			List<Crop> toRemove = new ArrayList<>();
			for(Crop crop : cropList) {

				try {
					crop.timePassing(pulse, productionLevel, solarIrradiance,
									 greyFilterRate, temperatureModifier);

				} catch (Exception e) {
					logger.severe(building, crop.getCropName() + " ran into issues ", e);
				}

				// Remove old crops.
				if (crop.getPhaseType() == PhaseType.FINISHED) {
					// Take back the growing area
					remainingGrowingArea = remainingGrowingArea + crop.getGrowingArea();
					toRemove.add(crop);
//					numCrops2Plant++;
				}
			}

			int size = cropList.size();
			numCrops2Plant = defaultCropNum - size;
	
//			logger.info(building, 10_000L,
//					"  defaultCropNum: " + defaultCropNum
//					+ "   size: " + size
//					+ "   numCrops2Plant: " + numCrops2Plant
//					+ "   remainingGrowingArea: " + remainingGrowingArea);
			
			// Remove finished crops
			cropList.removeAll(toRemove);
		}
		return valid;
	}

	/**
	 * Transfers the seedling.
	 * Note: Enable this task to take time to complete the work.
	 *
	 * @param time
	 * @param worker
	 * @return Crop
	 */
	public CropSpec selectSeedling() {
		CropSpec ct = null;
		// Add any new crops.
		for (int x = 0; x < numCrops2Plant; x++) {
			int size = cropListInQueue.size();
			if (size > 0) {
				String n = cropListInQueue.get(0);
				cropListInQueue.remove(0);
				ct = cropConfig.getCropTypeByName(n);
			}

			else {
				ct = selectVPCrop();
			}
		}
		return ct;
	}
	
	public void plantSeedling(CropSpec ct, double time, Worker worker) {
		Crop crop = plantACrop(ct, false, 0);
		cropList.add(crop);
		cropHistory.put(crop.getIdentifier(), crop.getCropName());
		building.fireUnitUpdate(UnitEventType.CROP_EVENT, crop);

		logger.log(building, worker, Level.INFO, 3_000, "Planted a new crop of " + crop.getCropName() + ".");
		numCrops2Plant--;
	}

	/**
	 * Gets the amount of power required when function is at full power.
	 *
	 * @return power (kW)
	 */
	@Override
	public double getFullPowerRequired() {
		// Power (kW) required for normal operations.
		double powerRequired = 0D;

		for (Crop crop : cropList) {
			// Tailor the lighting according to the phase type the crop is at
			if (crop.getPhaseType() == PhaseType.PLANTING 
					|| crop.getPhaseType() == PhaseType.INCUBATION)
				// More power is needed for illumination and crop monitoring
				powerRequired += powerGrowingCrop / 2D; 
			else if (crop.getPhaseType() == PhaseType.GERMINATION)
				powerRequired += powerGrowingCrop / 10D;
			else if (crop.getPhaseType() == PhaseType.HARVESTING 
					|| crop.getPhaseType() == PhaseType.FINISHED)
				// Winding down the growth
				powerRequired += powerGrowingCrop / 5D;
			else if (crop.getPhaseType() == PhaseType.FINISHED)
				;// do nothing
			else
				powerRequired += crop.getLightingPower();
		}

		// The normal lighting power during growing phase
//		powerRequired += getTotalLightingPower();

		// Note: add separate auxiliary power for subsystem, not just lighting power
		return powerRequired;
	}

	/**
	 * Gets the total amount of lighting power in this greenhouse.
	 *
	 * @return power (kW)
	 */
	public double getTotalLightingPower() {
		return cropList.stream().mapToDouble(Crop::getLightingPower).sum();
	}

	/**
	 * Gets the amount of power required when function is at power down level.
	 *
	 * @return power (kW)
	 */
	@Override
	public double getPoweredDownPowerRequired() {

		// Get power required for occupant life support.
		double powerRequired = 0D;

		// Add power required to sustain growing or harvest-ready crops.
		for (Crop crop : cropList) {
			if (crop.needsPower())
				powerRequired += powerSustainingCrop;
		}

		return powerRequired;
	}

	/**
	 * Gets the total growing area for all crops.
	 *
	 * @return growing area in square meters
	 */
	public double getGrowingArea() {
		return maxGrowingArea;
	}

	/**
	 * Gets the average number of growing cycles for a crop per orbit.
	 */
	public double getAverageGrowingCyclesPerOrbit() {

		double aveGrowingTime = cropConfig.getAverageCropGrowingTime();
		double solsInOrbit = MarsClock.AVERAGE_SOLS_PER_ORBIT_NON_LEAPYEAR;
		double aveGrowingCyclesPerOrbit = solsInOrbit * 1000D / aveGrowingTime; // e.g. 668 sols * 1000 / 50,000
																				// millisols

		return aveGrowingCyclesPerOrbit;
	}

	/**
	 * Checks to see if a botany lab with an open research slot is available and
	 * performs cell tissue extraction.
	 * 
	 * @return true if work has been done
	 */
	public boolean checkBotanyLab() {
		// Check to see if a botany lab is available
		boolean hasEmptySpace = false;
		
		// Check to see if the local greenhouse has a research slot
		if (lab == null)
			lab = building.getResearch();
		if (lab.hasSpecialty(ScienceType.BOTANY)) {
			hasEmptySpace = lab.checkAvailability();
		}

		if (hasEmptySpace) {
			// check to see if it can accommodate another researcher
			hasEmptySpace = lab.addResearcher();
		}

		else {
			// Check available research slot in another lab located in another greenhouse
			Set<Building> laboratoryBuildings = building.getSettlement().getBuildingManager().getBuildingSet(FunctionType.RESEARCH);
			Iterator<Building> i = laboratoryBuildings.iterator();
			while (i.hasNext() && !hasEmptySpace) {
				Building building = i.next();
				Research lab1 = building.getResearch();
				if (lab1.hasSpecialty(ScienceType.BOTANY)) {
					hasEmptySpace = lab1.checkAvailability();
					if (hasEmptySpace) {
						hasEmptySpace = lab1.addResearcher();
						// Note: compute research points to determine if it can be carried out.
						// int points += (double) (lab.getResearcherNum() * lab.getTechnologyLevel()) /
						// 2D;
					}
				}
			}
		}
		
		return hasEmptySpace;
	}

	
	/**
	 * Checks on a crop tissue.
	 * 
	 * @return
	 */
	public String checkOnCropTissue() {
		String name = null;
		List<String> unchecked = lab.getUncheckedTissues();
		int size = unchecked.size();
		if (size > 0) {
			int rand = RandomUtil.getRandomInt(size - 1);
			name = unchecked.get(rand);
			// mark this tissue culture. At max of 3 marks for each culture per sol
			lab.markChecked(name);
		}
		
		return name;
	}
		

	@Override
	public double getMaintenanceTime() {
		return maxGrowingArea * 5D;
	}

	/**
	 * Gets the farm's current crops.
	 *
	 * @return collection of crops
	 */
	public List<Crop> getCrops() {
		return cropList;
	}

	public List<String> getUninspected() {
		return houseKeeping.getUninspected();
	}

	public List<String> getUncleaned() {
		return houseKeeping.getUncleaned();
	}

	public void markInspected(String s) {
		houseKeeping.inspected(s);
	}

	public void markCleaned(String s) {
		houseKeeping.cleaned(s);
	}

	/**
	 * Records the average water usage on a particular crop.
	 *
	 * @param cropName
	 * @param usage    average water consumption in kg/sol
	 * @param type resource (0 for water, 1 for o2, 2 for co2, 3 for grey water)
	 */
	public void addCropUsage(String cropName, double usage, int type) {
		SolMetricDataLogger<Integer> crop = cropUsage.get(cropName);
		if (crop == null) {
			crop = new SolMetricDataLogger<>(MAX_NUM_SOLS);
			cropUsage.put(cropName, crop);
		}

		crop.increaseDataPoint(type, usage);
	}

	/**
	 * Computes the average water usage on a particular crop.
	 *
	 * @return average water consumption in kg/sol
	 */
	private double computeUsage(int type, String cropName) {
		double result = 0;
		SolMetricDataLogger<Integer> crop = cropUsage.get(cropName);
		if (crop != null) {
			result = crop.getDailyAverage(type);
		}
		return result;
	}

	/**
	 * Computes the resource usage on all crops.
	 *
	 * @type resource type (0 for water, 1 for o2, 2 for co2)
	 * @return water consumption in kg/sol
	 */
	public double computeUsage(int type) {
		// Note: the value is kg per square meter per sol
		double sum = 0;

		for (Crop c : cropList) {
			sum += computeUsage(type, c.getCropName());
		}
		
		return Math.round(sum * 100.0) / 100.0;
	}

	/**
	 * Gets the daily average water usage of the last 5 sols.
	 * Not: most weight on yesterday's usage. Least weight on usage from 5 sols ago.
	 *
	 * @return
	 */
	public double getDailyAverageWaterUsage() {
		return dailyWaterUsage.getDailyAverage();
	}

	/**
	 * Adds to the daily water usage.
	 *
	 * @param waterUssed
	 * @param solElapsed
	 */
	public void addDailyWaterUsage(double waterUsed) {
		dailyWaterUsage.increaseDataPoint(waterUsed);
	}

	public int getNumCrops2Plant() {
		return numCrops2Plant;
	}
	
	public Research getResearch() {
		if (lab == null)
			lab = building.getResearch();
		return lab;
	}

	/**
	 * Get the number of crops that needs tending in this Farm.
	 * 
	 * @return
	 */
	public int getNumNeedTending() {

		int cropsNeedingTending = 0;
		for (Crop c : cropList) {
			if (c.requiresWork()) {
				cropsNeedingTending++;
			}
			// if the health condition is below 50%,
			// need special care
			if (c.getHealthCondition() < .5)
				cropsNeedingTending++;
		}
		cropsNeedingTending += numCrops2Plant;

		return cropsNeedingTending;
	}

	/**
	 * Gets the tending score of all growing crops.
	 * 
	 * @return
	 */
	public int getTendingScore() {
		int score = 0;
		for (Crop c : cropList) {
			score += c.getTendingScore();
		}
		return score;
	}
	
	/**
	 * Gets the cumulative work time.
	 * 
	 * @return
	 */
	public double getCumulativeWorkTime() {
		return cumulativeWorkTime;
	}
	
	/**
	 * Gets the cumulative work time.
	 * 
	 * @return
	 */
	public void addCumulativeWorkTime(double value) {
		cumulativeWorkTime += value;
	}
	
	@Override
	public void destroy() {
		super.destroy();
	
		lab = null;
		cropListInQueue = null;
		cropList = null;
	}
}
