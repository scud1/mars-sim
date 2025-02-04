/*
 * Mars Simulation Project
 * MapPanel.java
 * @date 2023-06-19
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.tool.map;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.mars_sim.mapdata.MapData;
import org.mars_sim.mapdata.MapDataUtil;
import org.mars_sim.mapdata.MapMetaData;
import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.mission.MissionWindow;
import org.mars_sim.msp.ui.swing.tool.mission.NavpointPanel;
import org.mars_sim.msp.ui.swing.tool.navigator.NavigatorWindow;

@SuppressWarnings("serial")
public class MapPanel extends JPanel implements MouseWheelListener {

	public static final String DEFAULT_MAPTYPE = "surface";

	private static final Logger logger = Logger.getLogger(MapPanel.class.getName());
	
	private static final double HALF_PI = Math.PI / 2d;

	public static final int MAP_BOX_HEIGHT = NavigatorWindow.MAP_BOX_WIDTH;
	public static final int MAP_BOX_WIDTH = MAP_BOX_HEIGHT;
	private static int dragx, dragy;

	private transient ExecutorService executor;

	// Data members
	private boolean mapError;
	private boolean wait;
//	private boolean update;
	private boolean recreateMap = false;
	
	public double RHO_DEFAULT;
	
	private final double ZOOM_STEP = 16;
	private double multiplier;
	private double magnification;
	
	private String mapErrorMessage;
	private String mapStringType;
	
	private Coordinates centerCoords;

	private Image mapImage;

	private transient Map marsMap;
	
	private MainDesktopPane desktop;

	private NavigatorWindow navwin;
	
	private NavpointPanel navPanel;
	
	private Image starfield;
	
	private List<MapLayer> mapLayers;

	private static final MapDataUtil mapUtil = MapDataUtil.instance();
	
	/**
	 * Constructor 1.
	 * 
	 * @param desktop
	 * @param navwin
	 */
	public MapPanel(MainDesktopPane desktop, NavigatorWindow navwin) {
		this(desktop);
		this.navwin = navwin;
	}
	
	/**
	 * Constructor 2.
	 * 
	 * @param desktop
	 * @param navPanel
	 */
	public MapPanel(MainDesktopPane desktop, NavpointPanel navPanel) {
		this(desktop);
		this.navPanel = navPanel;
	}
	
	/**
	 * Constructor 3.
	 * 
	 * @param desktop
	 * @param refreshRate
	 */
	public MapPanel(MainDesktopPane desktop, long refreshRate) {
		this(desktop);
		this.desktop = desktop;
	}
	
	/**
	 * Constructor 4.
	 * 
	 * @param desktop
	 */
	public MapPanel(MainDesktopPane desktop) {
		super();
		this.desktop = desktop;
		
		init();
	}
	
	public void init() {
		
		starfield = ImageLoader.getImage("map/starfield");
		
		executor = Executors.newSingleThreadExecutor();
		
		// Initializes map instance as surf map
		setMapType(DEFAULT_MAPTYPE);
		
		mapError = false;
		wait = false;
		mapLayers = new CopyOnWriteArrayList<>();
//		update = true;
		centerCoords = new Coordinates(HALF_PI, 0D);
	
		setPreferredSize(new Dimension(MAP_BOX_WIDTH, MAP_BOX_HEIGHT));
		setMaximumSize(getPreferredSize());
		setSize(getPreferredSize());
		setBackground(Color.BLACK);
		setOpaque(true);
		
		RHO_DEFAULT = getScale();
		multiplier = RHO_DEFAULT / ZOOM_STEP;
		
		magnification = RHO_DEFAULT/RHO_DEFAULT;
		
		logger.info("scale: " + Math.round(RHO_DEFAULT * 10.0)/10.0 + "  multiplier: " + Math.round(multiplier * 10.0)/10.0);
	}

	
	public void mouseWheelMoved(MouseWheelEvent e) {
		
		// Gets the latest scale
		double oldRho = getScale();

		// May use this if (e.isControlDown()) {} to add ctrl key
        // May use if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {} to combine with other keys 
		
		double delta = e.getWheelRotation();
    	double rhoDelta = - multiplier * delta;
    	double newRho = oldRho + rhoDelta;
    	
//      Note 1:: Scroll up to zoom in or magnify map
//    	Note 2:Scroll down to zoom out or shrink the map

		if (newRho > RHO_DEFAULT * 4) {
			newRho = RHO_DEFAULT * 4;
		}
		else if (newRho < RHO_DEFAULT / 4) {
			newRho = RHO_DEFAULT / 4;
		}
 
		if (newRho != oldRho) {
			
			magnification = newRho/RHO_DEFAULT;
			
	    	// Update the map scale
//	    	setMapScale(newRho);

			// Call showMap
//			showMap(centerCoords, newRho);
	    	// which in turns calls updateDisplay()
	    	// which in turns calls MapTask thread
	    	// which in turns calls marsMap.drawMap(centerCoords, getScale());

			marsMap.drawMap(centerCoords, newRho);
			repaint();
		}
    }
	
	/*
	 * Sets up the mouse dragging capability.
	 */
	public void setMouseDragger(boolean isNavigator) {

		// List to the mouse scroll
		addMouseWheelListener(this);
		
		// Note: need navWin prior to calling addMouseMotionListener()
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int dx, dy, x = e.getX(), y = e.getY();

				dx = dragx - x;
				dy = dragy - y;

				if ((dx != 0 || dy != 0) 
					 && x > 0 && x < MAP_BOX_WIDTH 
					 && y > 0 && y < MAP_BOX_HEIGHT) {
					
					centerCoords = centerCoords.convertRectToSpherical(dx, dy, marsMap.getScale());
					marsMap.drawMap(centerCoords, getScale());
					repaint();
				}

				dragx = x;
				dragy = y;
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dragx = e.getX();
				dragy = e.getY();
				setCursor(new Cursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragx = 0;
				dragy = 0;
				if (isNavigator) {
					navwin.updateCoordsMaps(centerCoords);
				}
				else {
					navPanel.updateCoords(centerCoords);
				}

				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}

	/**
	 * Adds a new map layer.
	 * 
	 * @param newLayer the new map layer.
	 * @param index    the index order of the map layer.
	 */
	public void addMapLayer(MapLayer newLayer, int index) {
		if (newLayer != null) {
			if (!mapLayers.contains(newLayer)) {
				if (index < mapLayers.size()) {
					mapLayers.add(index, newLayer);
				} else {
					mapLayers.add(newLayer);
				}
			}
		} else
			throw new IllegalArgumentException("newLayer is null");
	}

	/**
	 * Removes a map layer.
	 * 
	 * @param oldLayer the old map layer.
	 */
	public void removeMapLayer(MapLayer oldLayer) {
		if (oldLayer != null) {
			if (mapLayers.contains(oldLayer)) {
				mapLayers.remove(oldLayer);
			}
		} else
			throw new IllegalArgumentException("oldLayer is null");
	}

	/**
	 * Checks if map has a map layer.
	 * 
	 * @param layer the map layer.
	 * @return true if map has the map layer.
	 */
	public boolean hasMapLayer(MapLayer layer) {
		return mapLayers.contains(layer);
	}

	/**
	 * Gets the map type.
	 * 
	 * @return map type.
	 */
	public MapMetaData getMapType() {
		return marsMap.getType();
	}

	public Map getMap() {
		return marsMap;
	}

	public MapData getMapData() {
		return mapUtil.getMapData(mapStringType);
	}
	
	/**
	 * Sets the map type.
	 * 
	 * @return map type set successfully
	 */
	public boolean setMapType(String mapStringType) {
		
		if ((marsMap == null) || !mapStringType.equals(marsMap.getType().getId())) {
			MapData data = mapUtil.getMapData(mapStringType);
			this.mapStringType = mapStringType;
			if (data == null) {
				logger.warning("Map type cannot be loaded " + mapStringType);
				return false;
			}
			marsMap = new CannedMarsMap(this, mapUtil.getMapData(mapStringType));
			recreateMap = true;
		}
			
		showMap(centerCoords, getScale());
		return true;
	}

	public Coordinates getCenterLocation() {
		return centerCoords;
	}
	
	public void showMap(Coordinates newCenter) {
		showMap(newCenter, getScale());
	}
	
	/**
	 * Displays map at given center, regenerating if necessary.
	 *
	 * @param newCenter the center location for the globe
	 */
	public void showMap(Coordinates newCenter, double scale) {
		if (newCenter == null) 
			return;
		
		if (centerCoords == null
			|| !centerCoords.equals(newCenter)
			|| scale != getScale()) {
				recreateMap = true;
				centerCoords = newCenter;
		}
		else {
			recreateMap = false;
		}
			
		if (recreateMap) {
			wait = true;
			updateDisplay(scale);
			recreateMap = false;
		}
	}

	public void updateDisplay() {
		updateDisplay(getScale());
	}

	public void updateDisplay(double scale) {
		if ((desktop.isToolWindowOpen(NavigatorWindow.NAME) 
			|| desktop.isToolWindowOpen(MissionWindow.NAME))
//			&& update 
			&& (!executor.isTerminated() || !executor.isShutdown())) {
				executor.execute(new MapTask(scale));
		}
	}

	class MapTask implements Runnable {

		private double scale;
		
		private MapTask(double scale) {
			this.scale = scale;
		}

		@Override
		public void run() {
			try {
				mapError = false;

				if (centerCoords == null) {
					logger.severe("centerCoords is null.");
					centerCoords = new Coordinates(HALF_PI, 0);
				}

				marsMap.drawMap(centerCoords, scale);
				wait = false;
				repaint();
				
			} catch (Exception e) {
				mapError = true;
				mapErrorMessage = e.getMessage();
				logger.severe("Can't draw surface map: " + e);
			}
		}
	}
	
	public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (desktop != null && isShowing() && 
        		(desktop.isToolWindowOpen(NavigatorWindow.NAME)
        		|| desktop.isToolWindowOpen(MissionWindow.NAME))) {
	        
        	Graphics2D g2d = (Graphics2D) g;
	        
        	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			
	        if (wait) {
	        	if (mapImage != null) 
	        		g2d.drawImage(mapImage, 0, 0, this);
	        	String message = "Generating Map";
	        	drawCenteredMessage(message, g2d);
	        }
	        else {
	        	if (mapError) {
	            	logger.log(Level.SEVERE,"mapError: " + mapErrorMessage);
	                // Display previous map image
//	                if (mapImage != null) g2d.drawImage(mapImage, 0, 0, this);
	
	                // Draw error message
	                if (mapErrorMessage == null) mapErrorMessage = "Null Map";
	                drawCenteredMessage(mapErrorMessage, g2d);
	            }
	        	else {

	        		g2d.setComposite(AlphaComposite.SrcOver); 
	        		
//	        		g2d.setBackground(Color.BLACK);
	        		
	        		// Clear the background with white
//	        		g2d.clearRect(0, 0, Map.DISPLAY_WIDTH, Map.DISPLAY_HEIGHT);

	        		// Paint black background
//	        		g2d.setPaint(Color.BLACK); 
	        		
//	        		g2d.setColor(Color.BLACK);
//	                
//	        		g2d.fillRect(0, 0, Map.DISPLAY_WIDTH, Map.DISPLAY_HEIGHT);
//	
	        		g2d.drawImage(starfield, 0, 0, Color.BLACK, this);
	
	                if (centerCoords != null) {
	                	if (marsMap != null && marsMap.isImageDone()) {
	                		mapImage = marsMap.getMapImage();
	                		if (mapImage != null) {
	                			g2d.drawImage(mapImage, 0, 0, this);
	                		}
	                	}
	                	else
	                		return;
	
	                	// Display map layers.
	                	
	                	// Put the map layers here.
	                	// Say if the location of a vehicle is updated
	                	// it doesn't have to redraw the marsMap.
	                	// It only have to redraw its map layer below
	                	Iterator<MapLayer> i = mapLayers.iterator();
	                	while (i.hasNext()) i.next().displayLayer(centerCoords, marsMap, g);
	                	
                		g2d.dispose();
	                }
	        	}
	        }
        }
    }

	/**
	 * Draws a message string in the center of the map panel.
	 * 
	 * @param message the message string
	 * @param g       the graphics context
	 */
	private void drawCenteredMessage(String message, Graphics g) {

		// Set message color
		g.setColor(Color.green);

		// Set up font
		Font messageFont = new Font("SansSerif", Font.BOLD, 25);
		g.setFont(messageFont);
		FontMetrics messageMetrics = getFontMetrics(messageFont);

		// Determine message dimensions
		int msgHeight = messageMetrics.getHeight();
		int msgWidth = messageMetrics.stringWidth(message);

		// Determine message draw position
		int x = (Map.DISPLAY_WIDTH - msgWidth) / 2;
		int y = (Map.DISPLAY_HEIGHT + msgHeight) / 2;

		// Draw message
		g.drawString(message, x, y);
	}

	public void update(ClockPulse pulse) {
		updateDisplay();
	}

    public Coordinates getMouseCoordinates(int x, int y) {
		double xMap = x - (Map.DISPLAY_WIDTH) - 1;
		double yMap = y - (Map.DISPLAY_HEIGHT) - 1;
		
		return centerCoords.convertRectToSpherical(xMap, yMap, marsMap.getScale());
    }

    /**
     * Gets the scale of the Mars surface map.
     * 
     * @return
     */
    public double getScale() {
    	return marsMap.getScale();
    }
    
    /**
     * Gets the magnification of the Mars surface map.
     * 
     * @return
     */
    public double getMagnification() {
    	return magnification;
    }
    
	/**
	 * Prepares map panel for deletion.
	 */
	public void destroy() {
		// Remove clock listener.
		mapLayers = null;
		if (executor != null) {
			// Stop anything running
			executor.shutdownNow();
		}
		executor = null;
		marsMap = null;
//		update = false;
		mapImage = null;
	}

}
