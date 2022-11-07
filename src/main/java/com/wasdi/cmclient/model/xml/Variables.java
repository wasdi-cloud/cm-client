package com.wasdi.cmclient.model.xml;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import lombok.Data;

@Data
public class Variables {

	private String msg;
	private Integer code;

	@JacksonXmlElementWrapper(useWrapping = false)
	private List<Variable> variable = new ArrayList<>();

}
