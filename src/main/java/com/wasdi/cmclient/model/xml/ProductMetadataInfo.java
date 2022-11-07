package com.wasdi.cmclient.model.xml;

import lombok.Data;

@Data
public class ProductMetadataInfo {

	private String msg;
	private TimeCoverage timeCoverage;
	private Variables variables;
	private Integer code;
	private DataGeospatialCoverage dataGeospatialCoverage;
	private String title;
	private String url;
	private AvailableTimes availableTimes;
	private GeospatialCoverage geospatialCoverage;
	private VariablesVocabulary variablesVocabulary;
	private String lastUpdate;
	private String id;
	private AvailableDepths availableDepths;

}
