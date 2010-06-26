/**
 * 
 */
package com.nanosheep.bikeroute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

/**
 * @author jono@nanosheep.net
 * @version Jun 26, 2010
 */
public class OSMParser extends XMLParser {

	/**
	 * @param feedUrl
	 */
	public OSMParser(String feedUrl) {
		super(feedUrl);
		// TODO Auto-generated constructor stub
	}


	public List<Marker> parse() {
			final Marker currentMarker = new Marker();
			final RootElement root = new RootElement("osm");
			final List<Marker> marks = new ArrayList<Marker>();
			final Element node = root.getChild("node");
			// Listen for start of tag, get attributes and set them
			// on current marker.
			node.setStartElementListener(new StartElementListener() {
				public void start(final Attributes attributes) {
					currentMarker.setLat(attributes.getValue("lat"));
					currentMarker.setLng(attributes.getValue("lon"));
				}

			});
			node.setEndElementListener(new EndElementListener() {
				public void end() {
					marks.add(currentMarker.copy());
				}
			});
			try {
				Xml.parse(this.getInputStream(), Xml.Encoding.UTF_8, root
						.getContentHandler());
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (SAXException e) {
				throw new RuntimeException(e);
			}
			return marks;
	}

}
