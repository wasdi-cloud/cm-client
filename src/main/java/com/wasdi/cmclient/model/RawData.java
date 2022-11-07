package com.wasdi.cmclient.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class RawData {

	List<String> serviceNamesList = new ArrayList<>();
	List<String> productNamesList = new ArrayList<>();

	Map<String, List<String>> productsByServiceMap = new HashMap<>();
	Map<String, List<String>> productsByVariablesMap = new HashMap<>();
	Map<String, List<String>> productsByAvailableDepthsMap = new HashMap<>();

}
