package com.wasdi.cmclient.model.json;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Mission {

	public String name;
	public String indexname;
	public String indexvalue;
	public List<Filter> filters = new ArrayList<>();

}
