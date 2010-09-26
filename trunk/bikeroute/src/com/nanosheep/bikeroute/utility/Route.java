package com.nanosheep.bikeroute.utility;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.graphics.Color;

//import com.google.android.maps.GeoPoint;
import org.andnav.osm.util.GeoPoint;

/**
 * @author jono@nanosheep.net
 * @version Jun 22, 2010
 */
public class Route {
	private String name;
	private final List<GeoPoint> points;
	private List<Segment> segments;
	private String copyright;
	private String warning;
	private String country;
	private int length;
	private XYSeries elevations;
	private String polyline;
	
	public Route() {
		points = new ArrayList<GeoPoint>();
		segments = new ArrayList<Segment>();
		elevations = new XYSeries("Elevation");
	}
	
	public void addPoint(final GeoPoint p) {
		points.add(p);
	}
	
	public void addPoints(final List<GeoPoint> points) {
		this.points.addAll(points);
	}
	
	public List<GeoPoint> getPoints() {
		return points;
	}
	
	public void addSegment(final Segment s) {
		segments.add(s);
	}
	
	public List<Segment> getSegments() {
		return segments;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param copyright the copyright to set
	 */
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	/**
	 * @return the copyright
	 */
	public String getCopyright() {
		return copyright;
	}

	/**
	 * @param warning the warning to set
	 */
	public void setWarning(String warning) {
		this.warning = warning;
	}

	/**
	 * @return the warning
	 */
	public String getWarning() {
		return warning;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param length the length to set
	 */
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * @return the length
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * Get the elevations as a set of series' that can be displayed by the
	 * achart lib.
	 * @return an XYMultipleSeriesDataset that contains the elevation/distance series.
	 */
	
	public XYMultipleSeriesDataset getElevations() {
		XYMultipleSeriesDataset elevationSet = new XYMultipleSeriesDataset();
		elevationSet.addSeries(elevations);
	    return elevationSet;
	}
	
	/**
	 * An an elevation and distance (in metres) to the elevation series for
	 * this route.
	 * @param elevation in metres.
	 * @param dist in metres.
	 */
	
	public void addElevation(final double elevation, final double dist) {
		elevations.add(dist / 1000, elevation);
	}
	
	/**
	 * Get a renderer for drawing the elevation chart.
	 * @return an XYMultipleSeriesRenderer configured for metric.
	 */
	
	public XYMultipleSeriesRenderer getChartRenderer() {
	    XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
	    XYSeriesRenderer r = new XYSeriesRenderer();
	    r.setColor(Color.BLUE);
	    r.setPointStyle(PointStyle.POINT);
	    r.setFillBelowLine(true);
	    r.setFillBelowLineColor(Color.GREEN);
	    r.setFillPoints(true);
	    renderer.addSeriesRenderer(r);
	    renderer.setAxesColor(Color.DKGRAY);
	    renderer.setLabelsColor(Color.LTGRAY);
	    renderer.setYTitle("m");
	    renderer.setXTitle("km");
	    return renderer;
	  }

	/**
	 * @param polyline the polyline to set
	 */
	public void setPolyline(String polyline) {
		this.polyline = polyline;
	}

	/**
	 * @return the polyline
	 */
	public String getPolyline() {
		return polyline;
	}

}
