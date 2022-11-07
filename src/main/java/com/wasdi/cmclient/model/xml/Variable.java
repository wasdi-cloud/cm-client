package com.wasdi.cmclient.model.xml;

import lombok.Data;

@Data
public class Variable {

	private String msg;
	private String standardName;
	private Integer code;
	private String name;
	private String description;
	private String units;
	private String longName;

}
