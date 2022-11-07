package com.wasdi.cmclient.model.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import lombok.Data;

@Data
public class AvailableDepths {

	private String msg;
	private Integer code;

	@JacksonXmlText
	private String text;

}
