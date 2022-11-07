package com.wasdi.cmclient;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wasdi.cmclient.model.RawData;
import com.wasdi.cmclient.model.json.Filter;
import com.wasdi.cmclient.model.json.Mission;
import com.wasdi.cmclient.model.xml.AvailableDepths;
import com.wasdi.cmclient.model.xml.CmServiceProduct;
import com.wasdi.cmclient.model.xml.ProductMetadataInfo;
import com.wasdi.cmclient.model.xml.Variable;
import com.wasdi.cmclient.model.xml.Variables;
import com.wasdi.cmclient.service.CmService;

@SpringBootApplication
public class CmClientApplication implements CommandLineRunner {

	private static Logger LOGGER = LoggerFactory.getLogger(CmClientApplication.class);

	@Autowired
	private CmService cmService;

	@Value("${cm.filter.results}")
	private boolean cmFilterResults;

	@Value("${cm.accepted.product.types}")
	private String cmAcceptedProductTypes;


	public static void main(String[] args) {
		SpringApplication.run(CmClientApplication.class, args);
	}

	@Override
	public void run(String... args) {
		LOGGER.info("start calling CM");
		
		LocalDateTime startDateTimeGetCatalog = LocalDateTime.now();
		List<CmServiceProduct> cmServiceProductList = getCatalog();
		LocalDateTime endDateTimeGetCatalog = LocalDateTime.now();
		
		LocalDateTime startDateTimeExtractRawData = LocalDateTime.now();
		RawData rawData = extractRawData(cmServiceProductList);
		LocalDateTime endDateTimeExtractRawData = LocalDateTime.now();


		LocalDateTime startDateTimeCreateMissionData = LocalDateTime.now();
		Mission mission = createMissionData(rawData);
		LocalDateTime endDateTimeCreateMissionData = LocalDateTime.now();

		LocalDateTime startDateTimeWriteDataToFile = LocalDateTime.now();
		writeDataToFile(mission);
		LocalDateTime endDateTimeWriteDataToFile = LocalDateTime.now();

		long secondsGetCatalog = ChronoUnit.SECONDS.between(startDateTimeGetCatalog, endDateTimeGetCatalog);
		long secondsExtractRawData = ChronoUnit.SECONDS.between(startDateTimeExtractRawData, endDateTimeExtractRawData);
		long secondsCreateMissionData = ChronoUnit.SECONDS.between(startDateTimeCreateMissionData, endDateTimeCreateMissionData);
		long secondsWriteDataToFile = ChronoUnit.SECONDS.between(startDateTimeWriteDataToFile, endDateTimeWriteDataToFile);
		long secondsOverall = ChronoUnit.SECONDS.between(startDateTimeGetCatalog, endDateTimeWriteDataToFile);

		LOGGER.info("getProductMetadataInfo | secondsGetCatalog: " + secondsGetCatalog);
		LOGGER.info("getProductMetadataInfo | secondsExtractRawData: " + secondsExtractRawData);
		LOGGER.info("getProductMetadataInfo | secondsCreateMissionData: " + secondsCreateMissionData);
		LOGGER.info("getProductMetadataInfo | secondseWriteDataToFile: " + secondsWriteDataToFile);
		LOGGER.info("getProductMetadataInfo | secondsOverall: " + secondsOverall);

		LOGGER.info("end calling CM");
	}

	private List<CmServiceProduct> getCatalog() {
		List<CmServiceProduct> cmServiceProductList = Collections.emptyList();

		if (cmFilterResults) {
			String[] acceptedProductTypesArray = cmAcceptedProductTypes.split("\\|");
			List<String> acceptedProductTypesList = Arrays.asList(acceptedProductTypesArray);

			cmServiceProductList = cmService.getCatalog().stream()
				.filter(t -> acceptedProductTypesList.contains(t.getService()))
				.collect(Collectors.toList());
		} else {
			cmServiceProductList = cmService.getCatalog();
		}
		
		LOGGER.info("getCatalog | " + "filtered productList size: " + cmServiceProductList.size());

		return cmServiceProductList;
	}

	private RawData extractRawData(List<CmServiceProduct> cmServiceProductList) {
		List<String> serviceNamesList = new ArrayList<>();
		List<String> productNamesList = new ArrayList<>();

		Map<String, List<String>> productsByServiceMap = new HashMap<>();
		Map<String, List<String>> productsByVariablesMap = new HashMap<>();
		Map<String, List<String>> productsByAvailableDepthsMap = new HashMap<>();


		List<CmServiceProduct> cmServiceProductListRetry = new ArrayList<CmServiceProduct>();


		int counter = 0;
		for (CmServiceProduct cmServiceProduct : cmServiceProductList) {
			counter++;

			String serviceName = cmServiceProduct.getService();
			String productName = cmServiceProduct.getProduct();

			LOGGER.info(String.format("%03d", counter) + ": " + serviceName + "." + productName);

			ProductMetadataInfo productMetadataInfo = cmService.getProductMetadataInfo(cmServiceProduct.getUrl());

			if (productMetadataInfo == null) {
				LOGGER.info("skipping this case due to the fact that the object is null");
				
				cmServiceProductListRetry.add(cmServiceProduct);
				continue;
			}

			LOGGER.info(productMetadataInfo.toString());
			LOGGER.info("");

			if (!serviceNamesList.contains(serviceName)) {
				serviceNamesList.add(serviceName);

				productsByServiceMap.put(serviceName, new ArrayList<String>());
			}

			productsByServiceMap.get(serviceName).add(productName);


			if (!productNamesList.contains(productName)) {
				productNamesList.add(productName);
			}



			Variables variables = productMetadataInfo.getVariables();
			if (variables != null && variables.getVariable() != null && !variables.getVariable().isEmpty()) {
				List<Variable> variablesList = variables.getVariable();
				String variablesString = variablesList.stream().map(Variable::getName).collect(Collectors.joining("|"));

				if (!productsByVariablesMap.containsKey(variablesString)) {
					productsByVariablesMap.put(variablesString, new ArrayList<String>());
				}

				productsByVariablesMap.get(variablesString).add(productName);
			}

			AvailableDepths availableDepths = productMetadataInfo.getAvailableDepths();
			if (availableDepths != null && availableDepths.getText() != null && !availableDepths.getText().isEmpty()) {
				String availableDepthsText = availableDepths.getText().replace(";", "|");

				if (!productsByAvailableDepthsMap.containsKey(availableDepthsText)) {
					productsByAvailableDepthsMap.put(availableDepthsText, new ArrayList<String>());
				}

				productsByAvailableDepthsMap.get(availableDepthsText).add(productName);
			}
		}


		if (!cmServiceProductListRetry.isEmpty()) {}


		LOGGER.debug(serviceNamesList.toString());
		LOGGER.debug(productNamesList.toString());
		LOGGER.debug(productsByServiceMap.toString());
		LOGGER.debug(productsByVariablesMap.toString());
		LOGGER.debug(productsByAvailableDepthsMap.toString());

		RawData rawData = new RawData();
		rawData.setServiceNamesList(serviceNamesList);
		rawData.setProductNamesList(productNamesList);
		rawData.setProductsByServiceMap(productsByServiceMap);
		rawData.setProductsByVariablesMap(productsByVariablesMap);
		rawData.setProductsByAvailableDepthsMap(productsByAvailableDepthsMap);

		return rawData;
	}

	private Mission createMissionData(RawData rawData) {
		Mission cmMission = new Mission();

		cmMission.setName("CM");
		cmMission.setIndexname("platformname");
		cmMission.setIndexvalue("CM");

		List<Filter> filters = new ArrayList<>();

		if (rawData != null) {
			filters = createFiltersData(rawData);
		}

		cmMission.setFilters(filters);

		return cmMission;
	}

	private List<Filter> createFiltersData(RawData rawData) {
		List<Filter> filters = new ArrayList<>();

		filters.addAll(createProductTypeFiltersData(rawData));
		filters.addAll(createProtocolFiltersData(rawData));
		filters.addAll(createDatasetFiltersData(rawData));
		filters.addAll(createVariablesFiltersData(rawData));
		filters.addAll(createDepthFiltersData(rawData));

		return filters;
	}

	private List<Filter> createProductTypeFiltersData(RawData rawData) {
		List<String> serviceNamesList = rawData.getServiceNamesList();

		String serviceNamesPipeSeparatedValues = serviceNamesList.stream().collect(Collectors.joining("|"));

		List<Filter> filters = new ArrayList<>();

		Filter filter = new Filter();

		filter.setIndexname("producttype");
		filter.setIndexlabel("Product Type");
		filter.setIndexvalues(serviceNamesPipeSeparatedValues);
		filter.setRegex(".*");
		filter.setIndexvalue(serviceNamesList.get(0));

		filters.add(filter);


		return filters;
	}

	private List<Filter> createProtocolFiltersData(RawData rawData) {
		List<String> serviceNamesList = rawData.getServiceNamesList();

		String serviceNamesPipeSeparatedValues = serviceNamesList.stream().collect(Collectors.joining("|"));
		String visibilityConditions = "producttype:" + serviceNamesPipeSeparatedValues;

		List<Filter> filters = new ArrayList<>();

		Filter filter = new Filter();

		filter.setIndexname("protocol");
		filter.setIndexlabel("Protocol");
		filter.setIndexvalues("SUBS");
		filter.setRegex(".*");
		filter.setVisibilityConditions(visibilityConditions);
		filter.setIndexvalue("SUBS");

		filters.add(filter);


		return filters;
	}

	private List<Filter> createDatasetFiltersData(RawData rawData) {
		List<String> serviceNamesList = rawData.getServiceNamesList();

		Map<String, List<String>> productsByServiceMap = rawData.getProductsByServiceMap();

		List<Filter> filters = new ArrayList<>();

		for (String serviceName : serviceNamesList) {
			List<String> productNamesList = productsByServiceMap.get(serviceName);

			if (productNamesList != null & !productNamesList.isEmpty()) {
				String productNamesPipeSeparatedValues = productNamesList.stream().collect(Collectors.joining("|"));

				String visibilityConditions = "producttype:" + serviceName;

				Filter filter = new Filter();

				filter.setIndexname("dataset");
				filter.setIndexlabel("Dataset");
				filter.setIndexvalues(productNamesPipeSeparatedValues);
				filter.setRegex(".*");
				filter.setVisibilityConditions(visibilityConditions);
				filter.setIndexvalue(productNamesList.get(0));

				filters.add(filter);
			}
		}

		return filters;
	}

	private List<Filter> createVariablesFiltersData(RawData rawData) {
		Map<String, List<String>> productsByVariablesMap = rawData.getProductsByVariablesMap();

		List<Filter> filters = new ArrayList<>();

		for (Entry<String, List<String>> entry : productsByVariablesMap.entrySet()) {
			String variablesString = entry.getKey();
			List<String> productNamesList = entry.getValue();

			String variablesStringWithPlus = variablesString.replace("|", "+");
			String variablesStringWithCombined = variablesString.equals(variablesStringWithPlus) ? variablesString : variablesStringWithPlus + "|" + variablesString;

			if (productNamesList != null & !productNamesList.isEmpty()) {
				String datasetString = productNamesList.stream().collect(Collectors.joining("|dataset:", "dataset:", ""));


				String visibilityConditions;

				if (productNamesList.size() == 1) {
					visibilityConditions = datasetString + "&protocol:SUBS";
				} else {
					visibilityConditions = "(" + datasetString + ")&protocol:SUBS";
				}

				Filter filter = new Filter();

				filter.setIndexname("variables");
				filter.setIndexlabel("Variables");
				filter.setIndexvalues(variablesStringWithCombined);
				filter.setRegex(".*");
				filter.setVisibilityConditions(visibilityConditions);
				filter.setIndexvalue(variablesStringWithPlus);

				filters.add(filter);
			}
		}

		return filters;
	}

	private List<Filter> createDepthFiltersData(RawData rawData) {
		Map<String, List<String>> productsByAvailableDepthsMap = rawData.getProductsByAvailableDepthsMap();

		List<Filter> filters = new ArrayList<>();

		for (Entry<String, List<String>> entry : productsByAvailableDepthsMap.entrySet()) {
			String depthsString = entry.getKey();
			List<String> productNamesList = entry.getValue();

			if (productNamesList != null & !productNamesList.isEmpty()) {
				String datasetString = productNamesList.stream().collect(Collectors.joining("|dataset:", "dataset:", ""));


				String visibilityConditions;

				if (productNamesList.size() == 1) {
					visibilityConditions = datasetString + "&protocol:SUBS";
				} else {
					visibilityConditions = "(" + datasetString + ")&protocol:SUBS";
				}

				String[] depths = depthsString.split("\\|");
				String firstDepth = depths[0];
				String lastDepth = depths[depths.length - 1];

				Filter filterStart = new Filter();

				filterStart.setIndexname("startDepth");
				filterStart.setIndexlabel("Depth From");
				filterStart.setIndexvalues(depthsString);
				filterStart.setRegex(".*");
				filterStart.setVisibilityConditions(visibilityConditions);
				filterStart.setIndexvalue(firstDepth);

				filters.add(filterStart);


				Filter filterEnd = new Filter();

				filterEnd.setIndexname("endDepth");
				filterEnd.setIndexlabel("Depth To");
				filterEnd.setIndexvalues(depthsString);
				filterEnd.setRegex(".*");
				filterEnd.setVisibilityConditions(visibilityConditions);
				filterEnd.setIndexvalue(lastDepth);

				filters.add(filterEnd);
			}
		}

		return filters;
	}

	private static void writeDataToFile(Mission mission) {

		try {
			ObjectMapper mapper = new ObjectMapper();

			mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get("cm_appconfig.json").toFile(), mission);
		} catch (JsonProcessingException e) {
			LOGGER.error(e.getMessage());
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
		}
		
	}

}
