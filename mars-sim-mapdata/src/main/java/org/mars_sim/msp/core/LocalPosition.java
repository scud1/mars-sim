/*
 * Mars Simulation Project
 * LocalPosition.java
 * @date 2021-11-26
 * @author Barry Evans
 */

package org.mars_sim.msp.core;

import java.io.Serializable;

/**
 * This represent a position within the local frame of reference of a Unit, e.g. Vehicle, Building
 */
public class LocalPosition implements Serializable {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Default position of the center.
	 */
    public static final LocalPosition DEFAULT_POSITION = new LocalPosition(0D, 0D);

	/** A very small distance (meters) for measuring how close two positions are. */
	private static final double VERY_SMALL_DISTANCE = .00001D;
	
	private double x;
	private double y;


	public LocalPosition(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

    /**
     * Returns the distance to another point.
     *
     * @param other Other location
     * @return the distance between the two sets of specified
     * coordinates.
     * @since 1.2
     */
    public double getDistanceTo(LocalPosition other)
    {
        double x1 = x - other.x;
        double y1 = y - other.y;
        return Math.sqrt(x1 * x1 + y1 * y1);
    }

    /**
     * Gets the position that is the mid point between two Positions.
     * 
     * @param other Other end of the line
     * @return
     */
	public LocalPosition getMidPosition(LocalPosition other) {
        return new LocalPosition((x + other.x) / 2D,
        						 (y + other.y) / 2D);
	}
	/**
	 * Gets the rotation direction to another Position.
	 * 
	 * @param other
	 * @return
	 */
	public double getDirectionTo(LocalPosition other) {
    	double result = Math.atan2(x - other.x,
                				   other.y - y);

		while (result > (Math.PI * 2D)) {
		    result -= (Math.PI * 2D);
		}
		
		while (result < 0D) {
		    result += (Math.PI * 2D);
		}
		return result;
	}
	
	/**
	 * Gets the Position a distance and rotation direction from this one.
	 * 
	 * @param distance
	 * @param direction
	 * @return New position
	 */
	public LocalPosition getPosition(double distance, double direction) {
		double newXLoc = (-1D * Math.sin(direction) * distance) + x;
		double newYLoc = (Math.cos(direction) * distance) + y;
        return new LocalPosition(newXLoc, newYLoc);
	}
	

	/**
	 * Gets this relative position as an absolute position relative to a basepoint.
	 * 
	 * @param bsaepoint Position as the base
	 * @return
	 */
	public LocalPosition getAbsolutePosition(LocalPosition basepoint) {
		return new LocalPosition(x + basepoint.x, y + basepoint.y);
	}
	
	/**
	 * Is another position close to this one?
	 * 
	 * @param other
	 * @return
	 */
	public boolean isClose(LocalPosition other) {
		return getDistanceTo(other) < VERY_SMALL_DISTANCE;
	}

	/**
	 * Is this position within the boundaries of an X & Y ?
	 * 
	 * @param maxX
	 * @param maxY
	 * @return
	 */
	public boolean isWithin(double maxX, double maxY) {
		return (Math.abs(x) < maxX && Math.abs(y) < maxY);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalPosition other = (LocalPosition) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

	/**
	 * Gets the position as a shortened string.
	 * 
	 * @return
	 */
	public String getShortFormat() {
		return String.format("(%.1f, %.1f)", x, y);
	}

	@Override
	public String toString() {
		return "(" + Math.round(x*10_000.0)/10_000.0 + ", " + Math.round(y*10_000.0)/10_000.0 + ")";
	}
}
