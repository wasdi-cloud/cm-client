package com.wasdi.cmclient.model.xml;

import lombok.Data;

@Data
public class GeospatialCoverage {

	private Double north;
	private Double south;
	private Double east;
	private Double west;
	private Double northSouthResolution;
	private String northSouthUnits;
	private Double eastWestResolution;
	private String eastWestUnits;
	private Integer code;
	private String msg;

}
