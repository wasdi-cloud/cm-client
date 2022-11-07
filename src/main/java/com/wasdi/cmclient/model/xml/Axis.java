package com.wasdi.cmclient.model.xml;

import lombok.Data;

@Data
public class Axis {

	private String msg;
	private String standardName;
	private Integer code;
	private Double lower;
	private Double upper;
	private String name;
	private String description;
	private Double step;
	private String units;
	private String axisType;
	private String longName;

}
