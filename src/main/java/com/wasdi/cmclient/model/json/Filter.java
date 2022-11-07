package com.wasdi.cmclient.model.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
public class Filter {

	public String indexname;
	public String indexlabel;
	public String indexvalues;
	public String regex;

	@JsonInclude(Include.NON_NULL)
	public String visibilityConditions;

	public String indexvalue;

}
