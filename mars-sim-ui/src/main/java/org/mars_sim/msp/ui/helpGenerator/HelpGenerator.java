/*
 * Mars Simulation Project
 * HelpGenerator.java
 * @date 2023-05-17
 * @author stpa
 */

package org.mars_sim.msp.ui.helpGenerator;

import java.io.File;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.food.FoodProductionProcessInfo;
import org.mars_sim.msp.core.food.FoodProductionProcessItem;
import org.mars_sim.msp.core.food.FoodProductionUtil;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.manufacture.ManufactureProcessInfo;
import org.mars_sim.msp.core.manufacture.ManufactureProcessItem;
import org.mars_sim.msp.core.manufacture.ManufactureUtil;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.ItemType;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.vehicle.VehicleConfig;
import org.mars_sim.msp.core.vehicle.VehicleSpec;
import org.mars_sim.msp.ui.swing.tool.resupply.SupplyTableModel;

/*
 * Help for finding the absolute path of the html help files 
 */

// First, use the command line argument -html for generating new html help files (Or run the eclipse launcher
// "MarsProject -html.launch" in Eclipse IDE to do the same).
//	 
// In Windows OS, the html help files are kept inside the src folder hierarchy as follows : 
//
// At \mars-sim\mars-sim-ui\src\main\resources\docs\help\$[DIR]
// e.g. D:\Data\git\mars-sim\mars-sim-ui\src\main\resources\docs\help\$[DIR]
//
// However, after running "MarsProject -html.launch" in mars-sim-main submodule in Eclipse, 
// the newly generated htmls will be saved inside the 'target' folder hierarchy,
// instead of the 'src' folder as follows :
//
// At \mars-sim-ui\target\classes\docs\help\$[DIR]
// e.g. D:\Data\git\mars-sim\mars-sim-ui\target\classes\docs\help\$[DIR]
//
// Note : the newly generated html files are located within the mars-sim-ui's 'target' folder
// and NOT the mars-sim-ui's 'src' folder.
//
// You will need to "manually" copy the newly generated htmls from the \target\$[DIR] to the \src\$[DIR] 
// and then commit the changes.
//   
// A total of 652 newly generated htmls using this HelpGenerator class can be broken into the followings :
// 1. food_production*.html
// 2. process*.html       
// 3. part*.html
// 4. resource*.html
// 5. $[vehicle type].html
//
// They are stored inside their respective directory $[DIR] as follows : 
// 1. food
// 2. processes
// 3. parts
// 4. resources
// 5. vehicles
//
// The base directory \help contains other important html pages such as the tutorial pages. They do not
// need to be overwritten in this case.
//
// In order to include a clean set of htmls, it is recommended that one should delete the old set of 
// htmls (1-5 above) in the \src\$[DIR] right before copying the new set of htmls from the \target\$[DIR]. 
// 
// There may be a time delay and refresh issue with Eclipse, if you use the Windows Explorer outside of 
// Eclipse IDE to manually delete the old html pages and then copy over the new html files.
//
// Afterward, make sure you "refresh" the Project Explorer in Eclipse so that it will intelligently compare which 
// particular html files have been changed/updated and which ones stay the same. Eclipse will compare the new
// ones against the local copies. In this manner, during "Git Staging", Eclipse will properly tag and post only 
// the html files that need to be updated in the mars-sim codebase. 
//
// For if you delete all 652 old html files in $[DIR] and copy over the new 652 html files, Eclipse would 
// intelligently detect the 20 html files that need to be updated in the codebase during "Git Staging".
//



/**
 * Generates html files for the in-game help. 
 * Note : run "MarsProject -html.launch" in Eclipse 
 */
public class HelpGenerator {

	/** initialized logger for this class. */
	private static SimLogger logger = SimLogger.getLogger(HelpGenerator.class.getName());

//	private static final String ABSOLUTE_DIR = "X:/path/to/your/workspace/code/mars-sim/mars-sim-ui/src/main/resources/docs/help";

//	private static final String DIR = "\\docs\\help\\";
	private static final String VEHICLE_DIR = "\\docs\\help\\vehicles\\";
	private static final String RESOURCE_DIR = "\\docs\\help\\resources\\";
	private static final String PART_DIR = "\\docs\\help\\parts\\";
	private static final String PROCESS_DIR = "\\docs\\help\\processes\\";
	private static final String FOOD_DIR = "\\docs\\help\\food\\";
//	private static final String EQUIPMENT_DIR = "\\docs\\help\\equipment";
		
	private static final String SUFFIX = ".html";

	private static final String VEHICLES = "../vehicles/vehicles";
	private static final String VEHICLE = "../vehicles/vehicle_";

	private static final String RESOURCES = "../resources/resources";
	private static final String RESOURCE = "../resources/resource_";

	private static final String PARTS = "../parts/parts";
	private static final String PART = "../parts/part_";

	private static final String PROCESSES = "../processes/processes";
	private static final String PROCESS = "../processes/process_";

	private static final String FOOD_PRODUCTIONS = "../food/food_productions";
	private static final String FOOD_PRODUCTION = "../food/food_production_";

	private static final String EQUIPMENTS = "../equipment/equipment";
	private static final String EQUIPMENT = "../equipment/equipment_";

	/** used to count how many files are generated. */
	private static int filesGenerated = 0;
	/** used to count how many files could not be generated. */
	private static int filesNotGenerated = 0;

	/**
	 * Inserts html header with given page title into given string buffer.
	 * 
	 * @param s {@link StringBuffer}
	 * @param title {@link String}
	 */
	private static final void helpFileHeader(final StringBuffer s, final String title) {
		StringBuffer header = new StringBuffer()
		.append("<!DOCTYPE html>\n")
		.append("<!-- Generated for mars-sim by st.pa. -->\n")
		.append("<html lang=\"en\">\n")
		.append("\t<head>\n")
		//.append("\t\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n")
		.append("\t\t<meta charset=\"UTF-8\">\n")
		.append("\t\t<title>")
		.append("Mars Simulation Project - Generated help file - ")
		.append(title)
		.append("</title>\n")
		.append("\t\t<link rel=\"stylesheet\" href=\"../msp.css\">\n")
		.append("\t</head>\n")
		.append("\t<body>\n\n")
		.append("\t<TT>\n\n");

		s.insert(0,header.toString());
	}

	private static final void helpFileFooter(final StringBuffer s) {
		s.append("\n\t</TT>\n\n");
		s.append("\n\t</body>\n");
		s.append("</html>\n");
	}

	private static final void helpFileTableRow(
		final StringBuffer s,
		final String[] columnContents
	) {
		s.append("\t<tr>\n");
		for (String columnContent : columnContents) {
			s.append("\t<td>");
			s.append(columnContent.replace(" ","&nbsp;").replace("a&nbsp;href", "a href"));
			s.append("</td>\n");
		}
		s.append("\t</tr>\n");
	}

	private static final void helpFileNoSuchProcess(final StringBuffer content) {
		content.append("<p><ul><li>No Such Manufacturing Processes Known</li></ul></p>");
	}

	private static final void helpFileNoSuchFoodProductionProcess(final StringBuffer content) {
		content.append("<p><ul><li>No Such Food Production Processes Known</li></ul></p>");
	}

	/**
	 * Generates a html file.
	 * 
	 * @param path the path of the html file.
	 * @param content the String content of the html file.
	 */
	private static final void generateFile(String dir, final String path, final String content) {
		PrintWriter pw = null;
		try {
			String absPath = getAbsPath(dir);
			
			if (absPath == null)
				return;
			
			File file = new File(absPath + '/' + path.toString());
			
		      // if the autosave/default save directory does not exist, create one now
	        if (!file.getParentFile().exists()) {
	            file.getParentFile().mkdirs();
	        }
	   
	        pw = new PrintWriter(file);
			pw.write(content.toString());
			pw.close();
			filesGenerated++;
		} catch (Exception e) {
			logger.log(Level.WARNING,"failed to generate file " + path.toString());
			filesNotGenerated++;
		}
		finally {	
			if (pw != null) {
				pw.close();
			}
		}
	}

	private static String escape(String s) {
		return s.replace(" ","_").replace("/","--");
	}

	private static final String getPathVehicle(final String vehicle) {
		return new StringBuffer()
		.append(VEHICLE)
		.append(escape(vehicle))
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathVehicles() {
		return new StringBuffer()
		.append(VEHICLES)
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathResource(final String resource) {
		return new StringBuffer()
		.append(RESOURCE)
		.append(escape(resource))
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathResources() {
		return new StringBuffer()
		.append(RESOURCES)
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathPart(final String part) {
		return new StringBuffer()
		.append(PART)
		.append(escape(part))
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathParts() {
		return new StringBuffer()
		.append(PARTS)
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathProcess(final String process) {
		return new StringBuffer()
		.append(PROCESS)
		.append(escape(process))
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathProcesses() {
		return new StringBuffer()
		.append(PROCESSES)
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathFoodProductionProcess(final String process) {
		return new StringBuffer()
		.append(FOOD_PRODUCTION)
		.append(escape(process))
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathFoodProductionProcesses() {
		return new StringBuffer()
		.append(FOOD_PRODUCTIONS)
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathEquipment(final String equipment) {
		return new StringBuffer()
		.append(EQUIPMENT)
		.append(escape(equipment))
		.append(SUFFIX)
		.toString();
	}

	private static final String getPathEquipments() {
		return new StringBuffer()
		.append(EQUIPMENTS)
		.append(SUFFIX)
		.toString();
	}

	
	/**
	 * Produces a link <code> a href = getPathResource(name) </code>
	 * with the name as caption.
	 * 
	 * @param resourceName {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkResource(final int resourceId) {
		String resourceName = ResourceUtil.findAmountResourceName(resourceId);
		return link(getPathResource(resourceName), resourceName);
	}

	/**
	 * Produces a link <code> a href = getPathResource(name) </code>
	 * with the name as caption.
	 * 
	 * @param resourceName {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkResource(final String resourceName) {
		return link(getPathResource(resourceName), resourceName);
	}

	/**
	 * Produces a link to the resources page with the given caption.
	 * 
	 * @param caption {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkResources(final String caption) {
		return link(getPathResources(),caption);
	}

	/**
	 * Produces a link <code> a href = getPathVehicle(name) </code>
	 * with the name as caption.
	 * 
	 * @param vehicleName {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkVehicle(final String vehicleName) {
		return link(getPathVehicle(vehicleName),vehicleName);
	}

	/**
	 * Produces a link to the vehicles page with the given caption.
	 * 
	 * @param caption {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkVehicles(final String caption) {
		return link(getPathVehicles(),caption);
	}

	/**
	 * Produces a link <code> a href = getPathProcess(name) </code>
	 * with the name as caption.
	 * 
	 * @param processName {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkProcess(final String processName) {
		return link(getPathProcess(processName),processName);
	}

	/**
	 * Produces a link to the processes page with the given caption.
	 * 
	 * @param caption {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkProcesses(final String caption) {
		return link(getPathProcesses(),caption);
	}

	/**
	 * Produces a link <code> a href = getPathFoodProductionProcess(name) </code>
	 * with the name as caption.
	 * 
	 * @param processName {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkFoodProductionProcess(final String processName) {
		return link(getPathFoodProductionProcess(processName),processName);
	}

	/**
	 * Produces a link to the Food Production processes page with the given caption.
	 * 
	 * @param caption {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkFoodProductionProcesses(final String caption) {
		return link(getPathFoodProductionProcesses(),caption);
	}

	private static final String getLinkEquipment(final String equipmentName) {
		return link(getPathEquipment(equipmentName),equipmentName);
	}

	private static final String getLinkEquipments(final String caption) {
		return link(getPathEquipments(),caption);
	}

	/**
	 * Produces a link <code> a href = getPathPart(name) </code>
	 * with the name as caption.
	 * 
	 * @param partName {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkPart(final String partName) {
		return link(getPathPart(partName),partName);
	}

	/**
	 * Produces a link to the parts page with the given caption.
	 * 
	 * @param caption {@link String}
	 * @return {@link String}
	 */
	private static final String getLinkParts(final String caption) {
		return link(getPathParts(),caption);
	}

	private static final String link(final String target, final String caption) {
		return new StringBuffer()
		.append("<a href=\"")
		.append(target)
		.append("\">")
		.append(caption)
		.append("</a>")
		.toString();
/*
		return
		"<a href=\""
		+ target
		+ "\">"
		+ Conversion.capitalize(caption)
		+ "</a>";
*/
	}

	/**
	 * Generates help files with vehicle descriptions.
	 */
	private static final void generateVehicleDescriptions() {
		List<String> vehicles = SupplyTableModel.getSortedVehicleTypes();

		// first generate "vehicles.html" with a list of defined vehicles
		StringBuffer content = new StringBuffer()
		.append("<h2>Vehicles</h2>\n")
		.append("<p>Types of Vehicles Featured for Mars Surface Operations:</p>")
		.append("<ol>\n");

		for (String vehicle : vehicles) {
			content.append("<li>")
			.append(getLinkVehicle(vehicle))
			.append("</li>\n");
		}
		content.append("</ol>");
		helpFileHeader(content,"vehicles");
		helpFileFooter(content);
		generateFile(VEHICLE_DIR, getPathVehicles(),content.toString());

		// second loop over vehicle types to generate a help file for each one
		int[] cargoArray = new int[] {
			ResourceUtil.hydrogenID,
			ResourceUtil.methanolID,
			ResourceUtil.oxygenID,
			ResourceUtil.waterID,
			ResourceUtil.foodID,
			ResourceUtil.rockSamplesID,
			ResourceUtil.iceID
		};
		VehicleConfig config = SimulationConfig.instance().getVehicleConfiguration();
		for (String vehicle : vehicles) {
			VehicleSpec v = config.getVehicleSpec(vehicle);
			String description = v.getDescription();
			if (description == null)
				description = "No Description is Available";
			content = new StringBuffer()
			.append("<h2>\"")
			.append(vehicle)
			.append("\" Vehicle</h2>\n")
			.append("</p></p>\n")

			.append(description)
			.append("</p>")
			.append("<table>\n");

			if (v.hasPartAttachments()) {
				StringBuffer parts = new StringBuffer().append("[");
				Collection<Part> partsCollection = v.getAttachableParts();
				if (partsCollection != null) {
					Iterator<Part> iterator = partsCollection.iterator();
					while (iterator.hasNext()) {
						Part part = iterator.next();
						parts.append(getLinkPart(part.getName()));
						if (iterator.hasNext()) {
							parts.append(", ");
						}
					}
					helpFileTableRow(content,new String[] {"Attachable Parts",parts.append("]").toString()});
					helpFileTableRow(content,new String[] {"Attachment Slots",Integer.toString(v.getAttachmentSlots())});
				}
			}
			helpFileTableRow(content,new String[] {"Base Speed",Double.toString(v.getBaseSpeed())});
			helpFileTableRow(content,new String[] {"Total Cargo Capacity",Double.toString(v.getTotalCapacity())});
			for (int cargo : cargoArray) {
				Double capacity = v.getCargoCapacity(cargo);
				if (capacity > 0.0) {
					StringBuffer caption = new StringBuffer()
					.append("Cargo Capacity for ")
					.append(getLinkResource(cargo))
					.append("  ");
					helpFileTableRow(
						content,
						new String[] {
							caption.toString(),
							Double.toString(capacity)
						}
					);
				}
			}
			helpFileTableRow(content,new String[] {"Crew Size",Integer.toString(v.getCrewSize())});
			helpFileTableRow(content,new String[] {"Empty Mass",Double.toString(v.getEmptyMass())});
			helpFileTableRow(content,new String[] {"Fuel Efficiency",Double.toString(v.getDrivetrainEfficiency())});
			if (v.hasLab()) {
				helpFileTableRow(content,new String[] {"Lab Tech Level",Integer.toString(v.getLabTechLevel())});
				helpFileTableRow(content,new String[] {"Lab Specialties",v.getLabTechSpecialties().toString()});
			}
			if (v.hasSickbay()) {
				helpFileTableRow(content,new String[] {"Sickbay Tech Level",Integer.toString(v.getSickbayTechLevel())});
				helpFileTableRow(content,new String[] {"Sickbay Beds",Integer.toString(v.getSickbayBeds())});
			}
			helpFileTableRow(content,new String[] {"Width",Double.toString(v.getWidth())});
			helpFileTableRow(content,new String[] {"Length",Double.toString(v.getLength())});

			content.append("</table>\n")
			.append("<hr><p>")
			.append(getLinkVehicles("Back to Vehicles Overview"))
			.append("</p><br/>\n")
			.append("<p>");

			helpFileHeader(content, "Vehicle \"" + vehicle + "\"");
			helpFileFooter(content);
			
			generateFile(VEHICLE_DIR, getPathVehicle(vehicle),content.toString());
		}
	}

	/**
	 * Generates help files with resources descriptions.
	 */
	private static final void generateResourceDescriptions() {
		ResourceUtil.createMaps();
		List<AmountResource> resources = ResourceUtil.getSortedAmountResources();
		
		// first: generate "resources.html" with a list of defined resources
		StringBuffer content = new StringBuffer()
		.append("<h2>Amount Resources</h2>\n")
		.append("<p>Available Types of Resources :</p>")
		.append("<table>\n");

		for (AmountResource resource : resources) {
			String name = resource.getName();	
			String life = resource.isLifeSupport() ? "   (Life Support)" : "";
			String edible = resource.isEdible() ? "   (Edible)" : "";

			helpFileTableRow(
				content,
				new String[] {
					getLinkResource(name),
					"   ",
					resource.getPhase().getName(),
					life,
					edible
				}
			);
		}

		content.append("</table>\n");

		helpFileHeader(content,"resources");
		helpFileFooter(content);
		generateFile(RESOURCE_DIR, getPathResources(),content.toString());

		// STEP 2 :
	
		// loop over resource types to generate a help file for each one
		//for (Map.Entry<String,AmountResource> entry : resources.entrySet()) {
		//	AmountResource resource = entry.getValue();
		//	String name = entry.getKey();
		for (AmountResource resource : resources) {
			String name = resource.getName();	
			String description = resource.getDescription();
			if (description == null)
				description = "No Description is Available";
			content = new StringBuffer()
			.append("<h2>Resource : \"")
			.append(name)
			.append("\" (")
			.append(resource.getPhase().getName())
			.append(")\t</h2>\n")
			.append("<br/>")
			.append("1. Description :<br/>\n")
			.append("<p><ul><li>")
			.append(description)
			.append("</li></ul></p><br/>");
			if (resource.isLifeSupport()) {
				content.append("<p>this resource is needed for life support.</p>\n");
			}

			content.append("\n2. Manufacturing Processes : \n");

			// list of manufacturing processes with the current resource as output
			List<ManufactureProcessInfo> output = ManufactureUtil
			.getManufactureProcessesWithGivenOutput(name);
			content.append("<p><u>How to make ")
			.append(name)
			.append(":</u></p>\n");
			if (output.size() > 0) {
				content.append("<ul>\n");
				for (ManufactureProcessInfo info : output) {
					content.append("\t<li>")
					.append(getLinkProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchProcess(content);

			// list of manufacturing processes with the current resource as input
			List<ManufactureProcessInfo> input = ManufactureUtil
			.getManufactureProcessesWithGivenInput(name);
			content.append("<p><u>What to do with ")
			.append(name)
			.append(":</u></p>\n");
			if (input.size() > 0) {
				content.append("<ul>\n");
				for (ManufactureProcessInfo info : input) {
					content.append("\t<li>")
					.append(getLinkProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchProcess(content);

			content.append("\n3. Food Production Processes : \n");

			// list of food production processes with the current resource as output
			List<FoodProductionProcessInfo> output_fp = FoodProductionUtil
			.getFoodProductionProcessesWithGivenOutput(name);
			content.append("<p><u>How to make ")
			.append(name)
			.append(":</u></p>\n");
			if (output_fp.size() > 0) {
				content.append("<ul>\n");
				for (FoodProductionProcessInfo info : output_fp) {
					content.append("\t<li>")
					.append(getLinkFoodProductionProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchFoodProductionProcess(content);

			// list of food production processes with the current resource as input
			List<FoodProductionProcessInfo> input_fp = FoodProductionUtil
			.getFoodProductionProcessesWithGivenInput(name);
			content.append("<p><u>What to do with ")
			.append(name)
			.append(":</u></p>\n");
			if (input_fp.size() > 0) {
				content.append("<ul>\n");
				for (FoodProductionProcessInfo info : input_fp) {
					content.append("\t<li>")
					.append(getLinkFoodProductionProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchFoodProductionProcess(content);

			content.append("<hr><p>")
			.append(getLinkResources("Back to Resources Overview"))
			.append("</p>")
			.append("<br/>");
			
			// finalize and generate help file
			helpFileHeader(content,"Resource \"" + resource + "\"");
			helpFileFooter(content);
			generateFile(RESOURCE_DIR, getPathResource(name),content.toString());

		}
	}

	/**
	 * Generates help files with parts descriptions.
	 */
	private static final void generatePartsDescriptions() {
		//Map<String, ItemResource> parts = ItemResource.getItemResourcesMap();
		//Map<String, Part> parts = ItemResource.getItemResourcesMap();
		List<Part> parts = ItemResourceUtil.getSortedParts();
		
		// first: generate "parts.html" with a list of defined equipment parts
		StringBuffer content = new StringBuffer()
		.append("<h2>Parts</h2>\n")
		.append("<p>Available Types of Parts and Equipments :</p>")
		.append("<ul>\n");
		
		//for (String part : parts.keySet()) {
		for (Part part : parts) {		
			String name = part.getName();
			content.append("\t<li>")
			.append(getLinkPart(name))
			.append("</li>\n");
		}
		content.append("</ul>\n");
		helpFileHeader(content,"parts");
		helpFileFooter(content);
		generateFile(PART_DIR, getPathParts(),content.toString());

		// second: loop over part types to generate a help file for each one
		//for (Entry<String, ItemResource> entry : parts.entrySet()) {
		//	Part part = (Part) entry.getValue();
		//for (Entry<String, Part> entry : parts.entrySet()) {
		//	Part part = entry.getValue();
		//	String name = entry.getKey();
		for (Part part : parts) {
			String name = part.getName();
			String description = part.getDescription();
			if (description == null)
				description = "No Description is Available";
			content = new StringBuffer()
			.append("<h2>Part : \"")
			.append(name)
			.append("\"</h2>\n")
			.append("</p><br/>")

			.append("1. Mass Per Unit : ")
			.append(Double.toString(part.getMassPerItem()))
			.append("kg<br/><br/>\n")
			.append("2. Description :<p><ul><li>\n")
			.append(description)
			.append("</li></ul></p><br/>");

			content.append("\n3. Manufacturing Processes : \n");

			// list of manufacturing processes with the current part as output
			List<ManufactureProcessInfo> output = ManufactureUtil
			.getManufactureProcessesWithGivenOutput(name);
			content.append("<p><u>a. How to make ")
			.append(name)
			.append(":</u></p>\n");
			if (output.size() > 0) {
				content.append("<ul>\n");
				for (ManufactureProcessInfo info : output) {
					content.append("\t<li>")
					.append(getLinkProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchProcess(content);
			content.append("</p>\n");
			// list of manufacturing processes with the current part as input
			List<ManufactureProcessInfo> input = ManufactureUtil
			.getManufactureProcessesWithGivenInput(name);
			content.append("<p><u>b. What to do with ")
			.append(name)
			.append(":</u></p>\n");
			if (input.size() > 0) {
				content.append("<ul>\n");
				for (ManufactureProcessInfo info : input) {
					content.append("\t<li>")
					.append(getLinkProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchProcess(content);

			content.append("\n4. Food Production Processes : \n");

			// list of food production processes with the current resource as output
			List<FoodProductionProcessInfo> output_fp = FoodProductionUtil
			.getFoodProductionProcessesWithGivenOutput(name);
			content.append("<p><u>How to make ")
			.append(name)
			.append(":</u></p>\n");
			if (output_fp.size() > 0) {
				content.append("<ul>\n");
				for (FoodProductionProcessInfo info : output_fp) {
					content.append("\t<li>")
					.append(getLinkFoodProductionProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchFoodProductionProcess(content);

			// list of food production processes with the current resource as input
			List<FoodProductionProcessInfo> input_fp = FoodProductionUtil
			.getFoodProductionProcessesWithGivenInput(name);
			content.append("<p><u>What to do with ")
			.append(name)
			.append(":</u></p>\n");
			if (input_fp.size() > 0) {
				content.append("<ul>\n");
				for (FoodProductionProcessInfo info : input_fp) {
					content.append("\t<li>")
					.append(getLinkFoodProductionProcess(info.getName()))
					.append("</li>\n");
				}
				content.append("</ul>\n");
			} else helpFileNoSuchFoodProductionProcess(content);

			content.append("<hr><p>")
			.append(getLinkParts("Back to Parts Overview"))
			.append("</p><br/>");
			
			// finalize and generate help file
			helpFileHeader(content,"Part \"" + part + "\"");
			helpFileFooter(content);
			generateFile(PART_DIR, getPathPart(name),content.toString());

		}
	}

	/**
	 * Generates help files with manufacturing process descriptions.
	 */
	private static final void generateProcessDescriptions() {
		TreeMap<String,ManufactureProcessInfo> processes = ManufactureUtil.getAllManufactureProcessesMap();

		// first: generate "processes.html" with a list of defined processes
		String[] header = new String[] {
			"<b>Tech  </b>",
			"<b>Skill  </b>",
			"<b>Work  </b>",
			"<b>Time  </b>",
			"<b>Power  </b>",
			"<b>Name</b>"
		};
		StringBuffer content = new StringBuffer()
		.append("<h2>Processes</h2>\n")
		.append("<p>Available Types of Manufacturing Processes:</p>\n")
		.append("<table>\n");
		helpFileTableRow(content,header);
		for (Entry<String,ManufactureProcessInfo> process : processes.entrySet()) {
			String name = process.getKey();
			ManufactureProcessInfo info = process.getValue();
			helpFileTableRow(
				content,
				new String[] {
					Integer.toString(info.getTechLevelRequired()),
					Integer.toString(info.getSkillLevelRequired()),
					Double.toString(info.getWorkTimeRequired()),
					Double.toString(info.getProcessTimeRequired()),
					Double.toString(info.getPowerRequired()),
					getLinkProcess(name)
				}
			);
		}
		helpFileTableRow(content,header);
		content.append("</table>\n");
		helpFileHeader(content,"processes");
		helpFileFooter(content);
		generateFile(PROCESS_DIR, getPathProcesses(),content.toString());

		// second: loop over processes to generate a help file for each one
		for (Entry<String,ManufactureProcessInfo> process : processes.entrySet()) {
			String name = process.getKey();
			ManufactureProcessInfo info = process.getValue();
			String description = info.getDescription();
			if (description == null)
				description = "No Description is Available";
			content = new StringBuffer()
			.append("<h2>Process : \"")
			.append(name)
			.append("\"</h2>\n")
			.append("1. Description :\n")
			.append("<p><ul><li>")
			.append(description)
			.append("</li></ul></p><br/>")
			.append("2. Characteristics :\n")
			.append("<table>\n");
			helpFileTableRow(content,new String [] {"Required Building Tech Level   ",Integer.toString(info.getTechLevelRequired())});
			helpFileTableRow(content,new String [] {"Required Skill Level",Integer.toString(info.getSkillLevelRequired())});
			helpFileTableRow(content,new String [] {"Work Time in millisols",Double.toString(info.getWorkTimeRequired())});
			helpFileTableRow(content,new String [] {"Time in millisols",Double.toString(info.getProcessTimeRequired())});
			helpFileTableRow(content,new String [] {"Power Requirement",Double.toString(info.getPowerRequired())});
			content.append("</table>\n")
			.append("<br/>\n")
			.append("3. Process Inputs :\n")
			.append("<p><table><ul>\n");
			for (ManufactureProcessItem input : info.getInputList()) {
				String inputName = input.getName();
				ItemType inputType = input.getType();
				String link = getLink_ResourceType(inputType,inputName);
				String type = getLink_ResourceLink(inputType);
				helpFileTableRow(
					content,
					new String[] {
						"<li>", type, "   ",
						Double.toString(input.getAmount()), "   ",
						link, "</li>"
					}
				);
			}
			content.append("</table></p>\n")
			.append("<br/>\n")
			.append("4. Process Outputs :\n")
			.append("<p><table><ul>\n");
			for (ManufactureProcessItem output : info.getOutputList()) {
				String outputName = output.getName();
				ItemType outputType = output.getType();
				String link = getLink_ResourceType(outputType,outputName);
				String type = getLink_ResourceLink(outputType);
				helpFileTableRow(
					content,
					new String[] {
						"<li>", type, "   ",
						Double.toString(output.getAmount()), "   ",
						link, "</li>"
					}
				);
			}
			content.append("</ul></table></p>\n")
			.append("<br/><hr>")
			.append(getLinkProcesses("Back to Processes Overview"))
			.append("</br></br>\n");
			
			// finalize and generate help file
			helpFileHeader(content,"Process \"" + name + "\"");
			helpFileFooter(content);
			generateFile(PROCESS_DIR, getPathProcess(name),content.toString());
		}
	}


	/**
	 * Generates help files with food production process descriptions.
	 */
	private static final void generateFoodProductionDescriptions() {
		TreeMap<String,FoodProductionProcessInfo> processes = FoodProductionUtil.getAllFoodProductionProcessesMap();

		// first: generate "foodProduction.html" with a list of defined processes
		String[] header = new String[] {
			"<b>Tech  </b>",
			"<b>Skill  </b>",
			"<b>Work  </b>",
			"<b>Time  </b>",
			"<b>Power  </b>",
			"<b>Name</b>"
		};
		StringBuffer content = new StringBuffer()
		.append("<h2>Food Production</h2>\n")
		.append("<p>Available Types of Food Production Processes:</p>\n")
		.append("<table><ol>\n");
		helpFileTableRow(content,header);
		for (Entry<String,FoodProductionProcessInfo> process : processes.entrySet()) {
			String name = process.getKey();
			FoodProductionProcessInfo info = process.getValue();
			helpFileTableRow(
				content,
				new String[] {
					Integer.toString(info.getTechLevelRequired()),
					Integer.toString(info.getSkillLevelRequired()),
					Double.toString(info.getWorkTimeRequired()),
					Double.toString(info.getProcessTimeRequired()),
					Double.toString(info.getPowerRequired()),
					getLinkFoodProductionProcess(name)
				}
			);
		}
		helpFileTableRow(content,header);
		content.append("</ol></table>\n");
		helpFileHeader(content,"Food Production");
		helpFileFooter(content);
		generateFile(FOOD_DIR, getPathFoodProductionProcesses(),content.toString());

		// second: loop over processes to generate a help file for each one
		for (Entry<String,FoodProductionProcessInfo> process : processes.entrySet()) {
			String name = process.getKey();
			FoodProductionProcessInfo info = process.getValue();
			String description = info.getDescription();
			if (description == null)
				description = "No Description is Available";
			content = new StringBuffer()
			.append("<h2>Food Production \"")
			.append(name)
			.append("\"</h2>\n")

			.append("1. Description :\n")
			.append("<p><ul><li>")
			.append(description)
			.append("</li></ul></p><br/>")
			.append("2. Characteristics :\n")
			.append("<p><table>\n");
			helpFileTableRow(content,new String [] {"Required Building Tech Level   ",Integer.toString(info.getTechLevelRequired())});
			helpFileTableRow(content,new String [] {"Required Skill Level",Integer.toString(info.getSkillLevelRequired())});
			helpFileTableRow(content,new String [] {"Work Time in millisols",Double.toString(info.getWorkTimeRequired())});
			helpFileTableRow(content,new String [] {"Time in millisols",Double.toString(info.getProcessTimeRequired())});
			helpFileTableRow(content,new String [] {"Power Requirement",Double.toString(info.getPowerRequired())});
			content.append("</table></p>\n")
			.append("<br/>\n")
			.append("3. Process Inputs :\n")
			.append("<table><ul>\n");
			for (FoodProductionProcessItem input : info.getInputList()) {
				String inputName = input.getName();
				ItemType inputType = input.getType();
				String link = getLink_ResourceType(inputType,inputName);
				String type = getLink_ResourceLink(inputType);
				helpFileTableRow(
					content,
					new String[] {
						"<li>", type, "   ",
						Double.toString(input.getAmount()), "   ",
						link, "</li>"
					}
				);
			}
			content.append("</ul></table>\n")
			.append("<br/>\n")

			.append("4. Process Outputs :\n")
			.append("<table><ul>\n");
			for (FoodProductionProcessItem output : info.getOutputList()) {
				String outputName = output.getName();
				ItemType outputType = output.getType();
				String link = getLink_ResourceType(outputType,outputName);
				String type = getLink_ResourceLink(outputType);
				helpFileTableRow(
					content,
					new String[] {
						"<li>", type, "   ",
						Double.toString(output.getAmount()), "   ",
						link, "</li>"
					}
				);
			}
			content.append("</ul></table>\n")
			.append("<br/><hr>")
			.append(getLinkFoodProductionProcesses("Back to Food Production Overview"))
			.append("</br></br>\n");

			// finalize and generate help file
			helpFileHeader(content,"Food Production \"" + name + "\"");
			helpFileFooter(content);
			generateFile(FOOD_DIR, getPathFoodProductionProcess(name),content.toString());
		}
	}

	private static String getLink_ResourceType(ItemType type, String name) {
		String link;
		switch (type) {
			case AMOUNT_RESOURCE : {
				link = getLinkResource(name);
				break;
			}
			case EQUIPMENT : {
				link = getLinkEquipment(name);
				break;
			}
			case PART : {
				link = getLinkPart(name);
				break;
			}
			case VEHICLE : {
				link = getLinkVehicle(name);
				break;
			}
			default : link = "";
		}
		return link;
	}

	private static String getLink_ResourceLink(ItemType type) {
		String link;
		switch (type) {
			case AMOUNT_RESOURCE : {
				link = getLinkResources("resource");
				break;
			}
			case EQUIPMENT : {
				link = getLinkEquipments("equipment");
				break;
			}
			case PART : {
				link = getLinkParts("part");
				break;
			}
			case VEHICLE : {
				link = getLinkVehicles("vehicle");
				break;
			}
			default : link = "";
		}
		return link;
	}

	private static String getAbsPath(String dir) {
		String absPath = "Wrong path";
		try {
			absPath = new File(
					HelpGenerator
					.class
					.getClassLoader()
					.getResource(dir)
					.toURI()
				).getAbsolutePath();
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE, "Error in getAbsPath", e);
		}
		
		return absPath;
	}
	
	
	/**
	 * Generates html help files for use in the in-game help and tutorial browser.
	 */
	public static final void generateHtmlHelpFiles() {
		logger.log(Level.INFO,"Starting to generate new help files");

		logger.log(Level.INFO,"Vehicles htmls are at " + getAbsPath(VEHICLE_DIR).replaceAll("\\\\", "/"));
		HelpGenerator.generateVehicleDescriptions();
		logger.log(Level.INFO,"Resources htmls are at " + getAbsPath(RESOURCE_DIR).replaceAll("\\\\", "/"));
		HelpGenerator.generateResourceDescriptions();
		logger.log(Level.INFO,"Parts htmls are at " + getAbsPath(PART_DIR).replaceAll("\\\\", "/"));
		HelpGenerator.generatePartsDescriptions();
		logger.log(Level.INFO,"Processes htmls are at " + getAbsPath(PROCESS_DIR).replaceAll("\\\\", "/"));
		HelpGenerator.generateProcessDescriptions();
		logger.log(Level.INFO,"Food htmls are at " + getAbsPath(FOOD_DIR).replaceAll("\\\\", "/"));
		HelpGenerator.generateFoodProductionDescriptions();
		logger.log(Level.INFO,"All done !");

		//FUTURE: will create HelpGenerator.generateMealsDescriptions();

		logger.log(
			Level.INFO,
			new StringBuffer()
				.append("Files Generated: ")
				.append(Integer.toString(filesGenerated))
				.append("  Failed: ")
				.append(Integer.toString(filesNotGenerated))
			.toString()
		);

		logger.log(
				Level.INFO,
				new StringBuffer()
					.append("All of the old html files are located at mars-sim-ui's src folder in /git/mars-sim/mars-sim-ui/src/main/resources/docs/help/")
				.toString()
			);
		
		logger.log(
				Level.INFO,
				new StringBuffer()
					.append("1. All of the newly generated html files are located at mars-sim-ui's target folder in /git/mars-sim/mars-sim-ui/target/classes/docs/help/")
				.toString()
			);
		
		logger.log(
				Level.INFO,
				new StringBuffer()
					.append("2. Now open the old src folder and go to /main/resources/docs/help/")
					.toString()
			);

		logger.log(
				Level.INFO,
				new StringBuffer()
					.append("3. Manually delete 5 folders (food, parts, processes, resources, and vehicles)"
							+ ", thus removing all old html files within")
					.toString()
			);
		
		logger.log(
				Level.INFO,
				new StringBuffer()
					.append("4. Manually copy all of the newly generated html files in those 5 folders from the target folder "
							+ "to the src folder, essentially replacing those deleted htmls in the src folder")
					.toString()
			);
		System.exit(0);
	}
}
