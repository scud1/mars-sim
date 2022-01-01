/*
 * Mars Simulation Project
 * MasterClock.java
 * @date 2021-12-09
 * @author Scott Davis
 */
package org.mars_sim.msp.core.time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.logging.SimLogger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The MasterClock represents the simulated time clock on virtual Mars and
 * delivers a clock pulse for each frame.
 */
public class MasterClock implements Serializable {

	/** default serial id. */
	static final long serialVersionUID = 1L;

	/** Initialized logger. */
	private static final SimLogger logger = SimLogger.getLogger(MasterClock.class.getName());

	public static final int MAX_SPEED = 11;

	/** The number of milliseconds for each millisol.  */
	private static final double MILLISECONDS_PER_MILLISOL = MarsClock.SECONDS_PER_MILLISOL * 1000.0;
	// Maximum number of pulses in the log
	private static final int MAX_PULSE_LOG = 10;
	// What is a reasonable jump in the observed real time
	// Allow for long simulation steps. 15 seconds
	// Note if debugging this triggers but the next pulse will reactivate
	private static final long MAX_ELAPSED = 30000;
	/** The time interval between each pulse for updating resource processes and crop growth. */
	private static final double TIME_INTERVAL = 50.0;

	// Data members
	/** Runnable flag. */
	private transient boolean keepRunning = false;
	/** Pausing clock. */
	private transient boolean isPaused = false;
	/** Flag for ending the simulation program. */
	private transient boolean exitProgram;
	/** The last uptime in terms of number of pulses. */
	private transient long tLast;

	/** The scale factor for updating process and crop update calls. */
	private double scaleFactor;
	/** The current simulation time ratio. */
	private double actualTR = 0;
	/** The time taken to execute one frame in the game loop */
	private long executionTime;
	/** The user's preferred simulation time ratio. */
	private int desiredTR = 0;

	/** The thread for running the clock listeners. */
	private transient ExecutorService listenerExecutor;
	/** Thread for main clock */
	private transient ExecutorService clockExecutor;

	/** A list of clock listener tasks. */
	private transient Collection<ClockListenerTask> clockListenerTasks;

	/** Is pausing millisol in use. */
	public boolean canPauseTime = false;
	/** Sol day on the last fireEvent. */
	private int lastSol = -1;
	/** The last millisol integer on the last fireEvent. */
	private int lastMSol = 0;
	/** The maximum wait time between pulses in terms of milli-seconds. */
	private int maxWaitTimeBetweenPulses;
	/** Number of millisols covered in the last pulse. */
	private double lastPulseTime;
	/** The minimum time span covered by each simulation pulse in millisols. */
	private double minMilliSolPerPulse;
	/** The maximum time span covered by each simulation pulse in millisols. */
	private double maxMilliSolPerPulse;
	/** The optimal time span covered by each simulation pulse in millisols. */
	private double optMilliSolPerPulse;

	/** Next Clock Pulse ID. Start on 1 as all Unit are primed as 0 for the last **/
	private long nextPulseId = 1;
	// Duration of last sleep
	public long sleepTime;

	// Records the real milli time when a pulse is execited
	private long[] pulseLog = new long[MAX_PULSE_LOG];

	// A list of recent TPS for computing average value of TPS
	private List<Double> aveTPSList;

	/** The Martian Clock. */
	private MarsClock marsClock;
	/** A copy of the initial martian clock at the start of the sim. */
	private MarsClock initialMarsTime;
	/** The Earth Clock. */
	private EarthClock earthClock;
	/** The Uptime Timer. */
	private UpTimer uptimer;
	/** The thread for running the game loop. */
	private ClockThreadTask clockThreadTask;
	/** The clock pulse. */
	private transient ClockPulse currentPulse;

	private SimulationConfig simulationConfig = SimulationConfig.instance();

	/**
	 * Constructor
	 *
	 * @param userTimeRatio the time ratio defined by user
	 * @throws Exception if clock could not be constructed.
	 */
	public MasterClock(int userTimeRatio) {
		// logger.config("MasterClock's constructor is on " + Thread.currentThread().getName() + " Thread");

		// Create a martian clock
		marsClock = MarsClockFormat.fromDateString(simulationConfig.getMarsStartDateTime());

		// Save a copy of the initial mars time
		initialMarsTime = (MarsClock) marsClock.clone();

		// Create an Earth clock
		earthClock = new EarthClock(simulationConfig.getEarthStartDateTime());

		// Create an Uptime Timer
		uptimer = new UpTimer();

		// Calculate elapsedLast
		timestampPulseStart();

		// Create a dedicated thread for the Clock
		clockThreadTask = new ClockThreadTask();

		logger.config("-----------------------------------------------------");

		minMilliSolPerPulse = simulationConfig.getMinSimulatedPulse();
		maxMilliSolPerPulse = simulationConfig.getMaxSimulatedPulse();
		// Optimal rate is bais towards the minimum whihc is more accurate
		optMilliSolPerPulse = minMilliSolPerPulse + ((maxMilliSolPerPulse - minMilliSolPerPulse) * 0.25);
		maxWaitTimeBetweenPulses = simulationConfig.getDefaultPulsePeriod();
		desiredTR = (int)simulationConfig.getTimeRatio();

		// Safety check
		if (minMilliSolPerPulse > maxMilliSolPerPulse) {
			logger.severe("The min pulse millisol is higher than the max pule.");
			throw new IllegalStateException("The min millisol per pulse cannot be higher than the max.");
		}

		logger.config("                 Base time-ratio     : " + desiredTR + "x");
		logger.config("          Min millisol per pulse     : " + minMilliSolPerPulse);
		logger.config("          Optimal millisol per pulse : " + optMilliSolPerPulse);
		logger.config("          Max millisol per pulse     : " + maxMilliSolPerPulse);
		logger.config(" Max elapsed time between pulses     : " + maxWaitTimeBetweenPulses + " ms");
		logger.config("-----------------------------------------------------");

		// Set the new scale factor
		setScaleFactor();
	}

	/**
	 * Returns the Martian clock
	 *
	 * @return Martian clock instance
	 */
	public MarsClock getMarsClock() {
		return marsClock;
	}

	/**
	 * Gets the initial Mars time at the start of the simulation.
	 *
	 * @return initial Mars time.
	 */
	public MarsClock getInitialMarsTime() {
		return initialMarsTime;
	}

	/**
	 * Returns the Earth clock
	 *
	 * @return Earth clock instance
	 */
	public EarthClock getEarthClock() {
		return earthClock;
	}

	/**
	 * Returns uptime timer
	 *
	 * @return uptimer instance
	 */
	public UpTimer getUpTimer() {
		return uptimer;
	}

	/**
	 * Adds a clock listener. A minumum duratino can be specified which throttles how many
	 * pulses the listener receives. If the duration is set to zero then all Pulses are distributed.
	 *
	 * If the duration is positive then pulses will be skipped to ensure a pulse is not delivered any
	 * quicker than the min duration. The delivered Pulse will have the full elapsed times including
	 * the skipped Pulses.
	 *
	 *
	 * @param newListener the listener to add.
	 * @Param minDuration The minimum duration in milliseconds between pulses.
	 */
	public final void addClockListener(ClockListener newListener, long minDuration) {
		// Check if clockListenerTaskList already contain the newListener's task,
		// if it doesn't, create one
		if (clockListenerTasks == null)
			clockListenerTasks = Collections.synchronizedSet(new HashSet<>());
		if (!hasClockListenerTask(newListener)) {
			clockListenerTasks.add(new ClockListenerTask(newListener, minDuration));
		}
	}

	/**
	 * Removes a clock listener
	 *
	 * @param oldListener the listener to remove.
	 */
	public final void removeClockListener(ClockListener oldListener) {
		ClockListenerTask task = retrieveClockListenerTask(oldListener);
		if (task != null) {
			clockListenerTasks.remove(task);
		}
	}

	/**
	 * Does it has the clock listener
	 *
	 * @param listener
	 * @return
	 */
	private boolean hasClockListenerTask(ClockListener listener) {
		Iterator<ClockListenerTask> i = clockListenerTasks.iterator();
		while (i.hasNext()) {
			ClockListenerTask c = i.next();
			if (c.getClockListener().equals(listener))
				return true;
		}
		return false;
	}

	/**
	 * Retrieve the clock listener task instance, given its clock listener
	 *
	 * @param listener the clock listener
	 */
	private ClockListenerTask retrieveClockListenerTask(ClockListener listener) {
		if (clockListenerTasks != null) {
			Iterator<ClockListenerTask> i = clockListenerTasks.iterator();
			while (i.hasNext()) {
				ClockListenerTask c = i.next();
				if (c.getClockListener().equals(listener))
					return c;
			}
		}
		return null;
	}

	/**
	 * Sets the exit program flag.
	 */
	public void exitProgram() {
		this.setPaused(true, false);
		exitProgram = true;
	}

	/*
	 * Gets the total number of pulses since the start of the sim
	 */
	public long getTotalPulses() {
		return nextPulseId;
	}

	/**
	 * Resets the clock listener thread
	 */
	private void resetClockListeners() {
		// If the clockListenerExecutor is not working, need to restart it
		logger.warning("The Clock Thread has died. Restarting...");

		// Re-instantiate clockListenerExecutor
		if (listenerExecutor != null) {
			listenerExecutor.shutdown();
			listenerExecutor = null;
		}

		// Restart executor, listener tasks are still in place
		startClockListenerExecutor();
	}


	/**
	 * Sets the simulation target time ratio and adjust the value of time between update
	 * (TBU)
	 *
	 * @param ratio
	 */
	public void setDesiredTR(int ratio) {
		if (ratio > 0D && desiredTR != ratio) {
			desiredTR = ratio;
			logger.config("Time-ratio x" + desiredTR);

			// Set the new scale factor
			setScaleFactor();
		}
	}

	/**
	 * Gets the target time ratio.
	 *
	 * @return ratio
	 */
	public int getDesiredTR() {
		return desiredTR;
	}
	
	/**
	 * Gets the actual time ratio
	 *
	 * @return
	 */
	public double getActualTR() {
		return actualTR;
	}

	/**
	 * Set the new scale factor
	 */
	private void setScaleFactor() {
		double ratio = TIME_INTERVAL / MAX_SPEED;
		scaleFactor = Math.round(TIME_INTERVAL *10.0)/10.0;
		logger.config("The scale factor becomes " + scaleFactor);
	}

	/**
	 * Gets the scale factor
	 */
	public double getScaleFactor() {
		return scaleFactor;
	}

	/**
	 * Runs master clock's thread using ThreadPoolExecutor
	 */
	private class ClockThreadTask implements Runnable, Serializable {

		private static final long serialVersionUID = 1L;

		private ClockThreadTask() {
		}

		@Override
		public void run() {
			// Keep running until told not to by calling stop()
			keepRunning = true;

			if (!isPaused) {

				while (keepRunning) {
					long startTime = System.currentTimeMillis();

					// Call addTime() to increment time in EarthClock and MarsClock
					if (addTime()) {
						// If a can was applied then potentially adjust the sleep
						executionTime = System.currentTimeMillis() - startTime;

						calculateSleepTime();
					}
					else {
						// If on pause or acceptablePulse is false
						sleepTime = maxWaitTimeBetweenPulses;
					}

					// If still going then wait
					if (keepRunning) {
						if (sleepTime > MAX_ELAPSED) {
							// This should not happen
							logger.warning("Sleep too long: clipped to " + maxWaitTimeBetweenPulses);
							sleepTime = maxWaitTimeBetweenPulses;
						}
						if (sleepTime > 0) {
							// Pause simulation to allow other threads to complete.
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
						}
					}

					// Exit program if exitProgram flag is true.
					if (exitProgram) {
						System.exit(0);
					}

				} // end of while
			} // if fxgl is not used

			logger.warning("Clock Thread stopping");

		} // end of run

		private void calculateSleepTime() {
			// Desired Millisols per seconds
			double desiredMsolPerSecond = desiredTR / MarsClock.SECONDS_PER_MILLISOL;
			
			// How many pulses are needed to fufill this desire
			double desiredPulses = desiredMsolPerSecond / optMilliSolPerPulse;
			desiredPulses = Math.max(desiredPulses, 1D);
			
			// Pulse periodicy
			double milliSecondsPerPulse = 1000D / desiredPulses;

			// Sleep time allows for the execution time
			sleepTime = (long)(milliSecondsPerPulse - executionTime);

			// What has happened?
			String msg = String.format("Sleep calcs desiredTR=%d, actualTR=%.2f, msol/sec=%.2f, pulse/sec=%.2f, ms/Pulse=%.2f, exection=%d ms, sleep=%d ms",
					desiredTR, actualTR, desiredMsolPerSecond, desiredPulses, milliSecondsPerPulse, executionTime, sleepTime);
		    logger.info(msg);
		}
	}

	/**
	 * Sets the pause time for the Command Mode
	 *
	 * @param value0
	 * @param value1
	 */
	public void setCommandPause(boolean value0, double value1) {
		// Check GameManager.mode == GameMode.COMMAND ?
		canPauseTime = value0;
		// Note: will need to re-implement the auto pause time for command mode
		logger.info("Auto pause time: " + value1);
	}

	/*
	 * Adds earth time and mars time.
	 *
	 * @return true if the pulse was accepted
	 */
	private boolean addTime() {
		boolean acceptablePulse = false;

		if (!isPaused) {
			// Find the new up time
			long tnow = System.currentTimeMillis();

			// Calculate the elapsed time in milli-seconds
			long realElaspedMilliSec = tnow - tLast;

			// Make sure there is not a big jump; suggest power save so skip it
			if (realElaspedMilliSec > MAX_ELAPSED) {
				// Reset the elapsed clock to ignore this pulse
				logger.warning("Elapsed real time is " + realElaspedMilliSec + " ms, longer than the max time "
			                   + MAX_ELAPSED + " ms.");
				timestampPulseStart();
			}
			else {
				// Get the time pulse length in millisols.
				lastPulseTime = (realElaspedMilliSec * desiredTR) / MILLISECONDS_PER_MILLISOL;

				// Pulse must be less than the max and positive
				if (lastPulseTime > 0) {

					acceptablePulse = true;

					if (lastPulseTime > maxMilliSolPerPulse) {
						logger.config(20_000, "Pulse width " + Math.round(lastPulseTime*100_000.0)/100_000.0
								+ " clipped to a max of " + maxMilliSolPerPulse + ".");
						lastPulseTime = maxMilliSolPerPulse;
					
					}
					else if (lastPulseTime < minMilliSolPerPulse) {
						logger.config(20_000, "Pulse width " + Math.round(lastPulseTime*100_000.0)/100_000.0
								+ " increased to a minimum of " + minMilliSolPerPulse + ".");
						lastPulseTime = minMilliSolPerPulse;
					}
				}
			}

			// Can we do something ?
			if (acceptablePulse && keepRunning) {
				// Elapsed time is acceptable
				// The time elapsed for the EarthClock aligned to adjusted Mars time
				long earthMillisec = (long)(lastPulseTime * MILLISECONDS_PER_MILLISOL);

				// Calculate the actual rate for feedback
				actualTR = earthMillisec / realElaspedMilliSec;

				if (!listenerExecutor.isTerminated()
					&& !listenerExecutor.isShutdown()) {
					// Do the pulse
					timestampPulseStart();

					// Update the uptimer
					uptimer.updateTime(realElaspedMilliSec);

					// Add time to the Earth clock.
					earthClock.addTime(earthMillisec);

					// Add time pulse to Mars clock.
					marsClock.addTime(lastPulseTime);

					// Run the clock listener tasks that are in other package
					fireClockPulse(lastPulseTime);
				}
				else {
					// NOTE: when resuming from power saving, timePulse becomes zero
					logger.config("The clockListenerExecutor has died. Restarting...");
					resetClockListeners();
				}
			}
		}
		return acceptablePulse;
	}

	/**
	 * Prepares clock listener tasks for setting up threads.
	 */
	public class ClockListenerTask implements Callable<String>{
		private double msolsSkipped = 0;
		private long lastPulseDelivered = 0;
		private ClockListener listener;
		private long minDuration;

		public ClockListener getClockListener() {
			return listener;
		}

		private ClockListenerTask(ClockListener listener, long minDuration) {
			this.listener = listener;
			this.minDuration = minDuration;
			this.lastPulseDelivered = System.currentTimeMillis();
		}

		@Override
		public String call() throws Exception {
			if (!isPaused) {
				try {
					// The most important job for ClockListener is to send a clock pulse to listener
					// gets updated.
					ClockPulse activePulse = currentPulse;

					// Handler is collapsing pulses so check the passed time
					if (minDuration > 0) {
						// Compare elapsed real time to the minimum
						long timeNow = System.currentTimeMillis();
						if ((timeNow - lastPulseDelivered) < minDuration) {
							// Less than the minimum so record elapse and skip
							msolsSkipped += currentPulse.getElapsed();
							return "skip";
						}

						// Build new pulse to include skipped time
						activePulse = currentPulse.addElapsed(msolsSkipped);

						// Reset count
						lastPulseDelivered = timeNow;
						msolsSkipped = 0;
					}

					// Call handler
					listener.clockPulse(activePulse);
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "Can't send out clock pulse: ", e);
				}
			}
			return "done";
		}
	}

	public long getNextPulse() {
		return nextPulseId;
	}

	/**
	 * Fires the clock pulse to each clock listener
	 *
	 * @param time
	 */
	private void fireClockPulse(double time) {
		// Identify if it's a new Millisol integer
		int currentMSol = marsClock.getMillisolInt();
		boolean isNewMSol = false;
		if (lastMSol != currentMSol) {
			lastMSol = currentMSol;
			isNewMSol = true;
		}
		// Identify if it's a new Sol
		int currentSol = marsClock.getMissionSol();
		boolean isNewSol = ((lastSol >= 0) && (lastSol != currentSol));
		lastSol = currentSol;

		// Log the pulse
		long newPulseId = nextPulseId++;
		int logIndex = (int)(newPulseId % MAX_PULSE_LOG);
		pulseLog[logIndex] = System.currentTimeMillis();

		currentPulse = new ClockPulse(newPulseId, time, marsClock, earthClock, this, isNewSol, isNewMSol);
		// Note: for-loop may handle checked exceptions better than forEach()
		// See https://stackoverflow.com/questions/16635398/java-8-iterable-foreach-vs-foreach-loop?rq=1

		// May do it using for loop

		// Note: Using .parallelStream().forEach() in a quad cpu machine would reduce TPS and unable to increase it beyond 512x
		// Not using clockListenerTasks.forEach(s -> { }) for now

		// Execute all listener concurrently and wait for all to complete before advancing
		// Ensure that Settlements stay synch'ed and some don't get ahead of others as tasks queue
		// May use parallelStream() after it's proven to be safe
		new HashSet<>(clockListenerTasks).stream().forEach(this::executeClockListenerTask);
	}

	/**
	 * Execute the clock listener task
	 *
	 * @param task
	 */
	public void executeClockListenerTask(ClockListenerTask task) {
		Future<String> result = listenerExecutor.submit(task);

		try {
			// Wait for it to complete so the listeners doesn't get queued up if the MasterClock races ahead
			result.get();
		} catch (ExecutionException ee) {
			logger.log(Level.SEVERE, "ExecutionException. Problem with clock listener tasks: ", ee);
		} catch (RejectedExecutionException ree) {
			// Application shutting down
			Thread.currentThread().interrupt();
			// Executor is shutdown and cannot complete queued tasks
			logger.log(Level.SEVERE, "RejectedExecutionException. Problem with clock listener tasks: ", ree);
		} catch (InterruptedException ie) {
			// Program closing down
			Thread.currentThread().interrupt();
			logger.log(Level.SEVERE, "InterruptedException. Problem with clock listener tasks: ", ie);
		}
	}

	/**
	 * Stop the clock
	 */
	public void stop() {
		keepRunning = false;
	}

	/**
	 * Restarts the clock
	 */
	public void restart() {
		keepRunning = true;
		timestampPulseStart();
	}

	/**
	 * Timestamps the last pulse, used to calculate elapsed pulse time
	 */
	private void timestampPulseStart() {
		tLast = System.currentTimeMillis();
	}

	/**
	 * Starts the clock
	 */
	public void start() {
		keepRunning = true;

		startClockListenerExecutor();

		if (clockExecutor == null) {
			int num = 1; // Should only have 1 thread updating the time
			logger.config("Setting up " + num + " thread(s) for clock executor.");
			clockExecutor = Executors.newFixedThreadPool(num,
					new ThreadFactoryBuilder().setNameFormat("masterclock-%d").build());
		}
		clockExecutor.execute(clockThreadTask);

		timestampPulseStart();
	}

	/**
	 * Increases the speed / time ratio
	 */
	public void increaseSpeed() {
		desiredTR *= 2;
	}

	/**
	 * Decreases the speed / time ratio
	 */
	public void decreaseSpeed() {
		desiredTR /= 2;
		if (desiredTR < 1) {
			desiredTR = 1;
		}
	}

	/**
	 * Updates the average TPS
	 *
	 * @return
	 */
	public double updateAverageTPS() {
		return getAverageTPS(getPulsesPerSecond());
	}

	/**
	 * Gets the average TPS value
	 *
	 * @param tps the current TPS
	 * @return the average TPS
	 */
	private double getAverageTPS(double tps) {
		// Compute the average value of TPS
		if (aveTPSList == null)
			aveTPSList = new ArrayList<>();
		if (tps > 0.3125) {
			aveTPSList.add(tps);
			if (aveTPSList.size() > 20)
				aveTPSList.remove(0);
		}

		DoubleSummaryStatistics stats = aveTPSList.stream().collect(Collectors.summarizingDouble(Double::doubleValue));
		double ave = stats.getAverage();
		if (ave <= 0.3125) {
			aveTPSList.clear();
			ave = tps;
		}

		return ave;
	}

	/**
	 * Set if the simulation is paused or not.
	 *
	 * @param value the state to be set.
	 * @param showPane true if the pane should be shown.
	 */
	public void setPaused(boolean value, boolean showPane) {
		if (this.isPaused != value) {
			this.isPaused = value;

			if (!value) {
				// Reset the last pulse time
				timestampPulseStart();
			}

			// Fire pause change to all clock listeners.
			firePauseChange(value, showPane);
		}
	}

	/**
	 * Checks if the simulation is paused or not.
	 *
	 * @return true if paused.
	 */
	public boolean isPaused() {
		return isPaused;
	}

	/**
	 * Send a pulse change event to all clock listeners.
	 *
	 * @param isPaused
	 * @param showPane
	 */
	private void firePauseChange(boolean isPaused, boolean showPane) {
		clockListenerTasks.forEach(cl -> cl.listener.pauseChange(isPaused, showPane));
	}

	/**
	 * Starts clock listener thread pool executor
	 */
	private void startClockListenerExecutor() {
		if (listenerExecutor == null) {
			int num = Math.min(1, Simulation.NUM_THREADS - simulationConfig.getUnusedCores());
			if (num <= 0) num = 1;
			logger.config("Setting up " + num + " thread(s) for clock listener.");
			listenerExecutor = Executors.newFixedThreadPool(num,
					new ThreadFactoryBuilder().setNameFormat("clockListener-%d").build());
		}
	}

	/**
	 * Shuts down clock listener thread pool executor
	 */
	public void shutdown() {
		if (listenerExecutor != null)
			listenerExecutor.shutdownNow();
		if (clockExecutor != null)
			clockExecutor.shutdownNow();
	}


	/**
	 * Gets the Frame per second
	 *
	 * @return
	 */
	public double getFPS() {
		// How to check xFGL version ?
		return 0;
	}

	/**
	 * Gets the sleep time in milliseconds
	 *
	 * @return
	 */
	public long getSleepTime() {
		return sleepTime;
	}

	/**
	 * Gets the millisols covered in the last pulse
	 *
	 * @return
	 */
	public double getMarsPulseTime() {
		return lastPulseTime;
	}

	/**
	 * Gets the time [in microseconds] taken to execute one frame in the game loop
	 *
	 * @return
	 */
	public long getExecutionTime() {
		return executionTime;
	}

	/**
	 * How many pulses per second
	 *
	 * @return
	 */
	public double getPulsesPerSecond() {
		double ticksPerSecond = 0;

		// Make sure enough pulses have passed
		if (nextPulseId >= MAX_PULSE_LOG) {
			// Recent idx will be the previous pulse id but check it is not negative
			int recentIdx = (int)((nextPulseId-1) % MAX_PULSE_LOG);
			recentIdx = (recentIdx < 0 ? (MAX_PULSE_LOG-1) : recentIdx);

			// Oldest id will be the next pulse as it will be overwrite on next tick
			int oldestIdx = (int)(nextPulseId % MAX_PULSE_LOG);
			long elapsedMilli = (pulseLog[recentIdx] - pulseLog[oldestIdx]);
			ticksPerSecond = (MAX_PULSE_LOG * 1000D)/elapsedMilli;
		}

		return ticksPerSecond;
	}

	/**
	 * Gets the clock pulse
	 *
	 * @return
	 */
	public ClockPulse getClockPulse() {
		return currentPulse;
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		marsClock = null;
		initialMarsTime = null;
		earthClock.destroy();
		earthClock = null;
		uptimer = null;
		clockThreadTask = null;
		listenerExecutor = null;
	}
}
