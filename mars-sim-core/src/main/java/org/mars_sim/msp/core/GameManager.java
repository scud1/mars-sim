/*
 * Mars Simulation Project
 * GameManager.java
 * @date 2022-11-02
 * @author Manny Kung
 */

package org.mars_sim.msp.core;

import java.util.Collection;
import java.util.logging.Logger;

import org.mars_sim.msp.core.person.Commander;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.structure.Settlement;

public class GameManager {

	private static final Logger logger = Logger.getLogger(GameManager.class.getName());

    /** The GameMode enums. */
    public enum GameMode {
        COMMAND, SANDBOX, SOCIETY;
    }

    /** An instance of GameMode. */
    private static GameMode gameMode = GameMode.SANDBOX;

    /** The menu variable captures the first menu choice in the console main menu. */
    public static String menu = "";

    /** The resolution variable captures the choice of the screen resolution in the console main menu. */
    public static String resolution = "";

    /** The input variable captures the game mode choice. */
    public static String input = "";

    /** The site variable captures the choice of site selection. */
    public static String site = "";

    /** The commandCfg variable captures the first choice in the Command Mode. */
    public static String commandCfg = "";

    /** The sandboxCfg variable captures the first choice in the Sandbox Mode. */
    public static String sandboxCfg = "";

    /** The Commander instance. */
    public static Person commanderPerson;

    public GameManager() {
    	// initialize here
    }

    /**
     * Change of the global commander.
     * @param cc
     */
    public static void setCommander(Person cc) {
    	commanderPerson = cc;
    }

    public static void initializeInstances(UnitManager u) {

		if (u.getCommanderID() > 0) {
			gameMode = GameMode.COMMAND;
	    	int id = u.getCommanderID();
	    	commanderPerson = u.getPersonByID(id);
		}
		else {
			gameMode = GameMode.SANDBOX;
		}

    }

	/**
	 * Find the settlement match for the user proposed commander's sponsor
	 */
	public static void placeInitialCommander(UnitManager unitMgr) {

		PersonConfig personConfig = SimulationConfig.instance().getPersonConfig();
		Commander commander = personConfig.getCommander();

		String sponsorCode = commander.getSponsorStr();

		Settlement selected = null;
		Collection<Settlement> settlements = unitMgr.getSettlements();
		for(Settlement s : settlements) {
			// If the sponsors are a match
			if (sponsorCode.equals(s.getReportingAuthority().getName()) ) {
				selected = s;
				logger.config("The Sponsor '" + sponsorCode + "' does have a settlement called '" + s + "'.");
				break;
			}
		}

		if (selected == null) {
			// Select a random settlement
			selected = settlements.iterator().next();
			logger.config("The Sponsor '" + sponsorCode + "' doesn't have any settlements. Choosen " + selected);
		}

		// Found the commander home
		commanderPerson = selected.setDesignatedCommander(commander);
	}

	/**
	 * Gets the game mode
	 *
	 * @return GameMode
	 */
    public static GameMode getGameMode() {
    	return gameMode;
    }

	/**
	 * Gets the game mode
	 *
	 * @return GameMode
	 */
    public static void setGameMode(GameMode mode) {
    	gameMode = mode;
    }

    @Override
    public String toString() {
        return System.lineSeparator() +"> ";
    }
}
