/*
 * Mars Simulation Project
 * CommerceMission.java
 * @date 2022-07-19
 * @author Barry Evans
 */
package org.mars_sim.msp.core.goods;

import java.util.Map;

import org.mars_sim.msp.core.person.ai.mission.MissionStatus;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * A mission that is undertaking commerce between 2 settlements
 */
public interface CommerceMission {

    /**
     * Common Mission Status for commerce
     */
    public static final MissionStatus NO_TRADING_SETTLEMENT = new MissionStatus("Mission.status.noTradeSettlement");

    /**
     * Settlement starting the commerce action.
     */
    Settlement getStartingSettlement();

    /**
     * Settlement trading with
     */
    Settlement getTradingSettlement();

    Map<Good, Integer> getDesiredBuyLoad();

    Map<Good, Integer> getBuyLoad();

    Map<Good, Integer> getSellLoad();

}
