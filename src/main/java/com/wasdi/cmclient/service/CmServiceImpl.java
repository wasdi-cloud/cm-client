package com.wasdi.cmclient.service;

import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.wasdi.cmclient.model.xml.CmServiceProduct;
import com.wasdi.cmclient.model.xml.ProductMetadataInfo;

@Service
public class CmServiceImpl implements CmService {

	private static Logger LOGGER = LoggerFactory.getLogger(CmServiceImpl.class);

	public static final String s_sCmemsCasUrl = "https://cmems-cas.cls.fr/cas/login";

	@Autowired
	private RestTemplate restTemplate;

	@Value("${cm.user}")
	private String cmUsername;

	@Value("${cm.password}")
	private String cmPassword;

	@Value("${cm.catalog.uri}")
	private String cmCatalogUri;

	@Override
	public List<CmServiceProduct> getCatalog() {
		LOGGER.debug("getCatalog");

		try {
			HttpHeaders headers = new HttpHeaders();
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(cmCatalogUri, HttpMethod.GET, request, String.class);

			String responseBody = response.getBody();

			List<String> urlList = parseCatalog(responseBody);
			LOGGER.info("getCatalog | " + "urlList size: " + urlList.size());
			
			List<CmServiceProduct> cmServiceProductList = parseUrls(urlList);
			LOGGER.info("getCatalog | " + "cmServiceProductList size: " + cmServiceProductList.size());

			for (CmServiceProduct cmServiceProduct : cmServiceProductList) {
				LOGGER.info(cmServiceProduct.toString());
			}

			LOGGER.info("");
			return cmServiceProductList;
		} catch (HttpClientErrorException e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("" + e.getRawStatusCode());
			LOGGER.error(e.getResponseHeaders().toString());
			LOGGER.error("");
		}

		return Collections.emptyList();
	}

	private static CmServiceProduct parseUrl(String url) {
		CmServiceProduct cmServiceProduct = new CmServiceProduct();

		String service = url.substring(url.indexOf("&service=") + 9, url.indexOf("&product="));
		String product = url.substring(url.indexOf("&product=") + 9);

		cmServiceProduct.setService(service);
		cmServiceProduct.setProduct(product);
		cmServiceProduct.setUrl(url);

		return cmServiceProduct;
	}

	private static List<CmServiceProduct> parseUrls(List<String> urlList) {
		return urlList.stream().map(CmServiceImpl::parseUrl).collect(Collectors.toList());
	}

	private static List<String> parseCatalog(String catalogContent) {
		List<String> urlList = new ArrayList<>();

		String[] lines = catalogContent.split(System.getProperty("line.separator"));

		for (String line : lines) {
			if (line.trim().startsWith("<gmd:URL>")) {
				String trimmedLine = line.trim();
				if (trimmedLine.contains("/motu-web/Motu")) {
					String url = trimmedLine.substring(trimmedLine.indexOf("<gmd:URL>") + 9, trimmedLine.indexOf("</gmd:URL>"));

					String unescapedUrl =url.replace("&amp;", "&");

					urlList.add(unescapedUrl);
				}
			}
		}

		return urlList;
	}

	@Override
	public ProductMetadataInfo getProductMetadataInfo(String uri) {
		LOGGER.info("getProductMetadataInfo | uri: " + uri);
		LocalDateTime startDateTime = LocalDateTime.now();

		Map<String, String> cookies = acquireCookies(uri, cmUsername, cmPassword);

		ProductMetadataInfo productMetadataInfo = null;
		if (cookies != null) {
			productMetadataInfo = callGetAndObtainResponse(uri, cookies);

			LocalDateTime endDateTime = LocalDateTime.now();
			long seconds = ChronoUnit.SECONDS.between(startDateTime, endDateTime);

			LOGGER.info("getProductMetadataInfo | time in seconds: " + seconds);
		}

		return productMetadataInfo;
	}

	private ProductMetadataInfo callGetAndObtainResponse(String url, Map<String, String> cookies) {
		LOGGER.debug("callGetAndObtainResponse");

		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));

		for (Map.Entry<String, String> entry : cookies.entrySet()) {
			headers.set("Cookie", entry.getKey() + "=" + entry.getValue());
		}

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<ProductMetadataInfo> response = restTemplate.exchange(url, HttpMethod.GET, request, ProductMetadataInfo.class);

			return response.getBody();
		} catch (HttpClientErrorException e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("" + e.getRawStatusCode());
			LOGGER.error(e.getResponseHeaders().toString());
			LOGGER.error("");

			if (e.getMessage().contains("You are registered but have forgotten your login/password?")
					|| e.getMessage().contains("For security reasons, please Exit your web browser when you quit services requiring authentication!")) {
				resetCookies();
			}

			return null;
		} catch (ResourceAccessException e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("");

			return null;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("");

			return null;
		}
	}

	private Map<String, String> acquireCookies(String url, String username, String password) {
		Map<String, String> params = new HashMap<>();
		params.put("service", url);

		CookieManager cookieManager;
		if (CookieHandler.getDefault() == null) {
			cookieManager = new CookieManager();
			CookieHandler.setDefault(cookieManager);
		} else {
			cookieManager = ((CookieManager) CookieHandler.getDefault());
		}

		Map<String, String> cookies = new HashMap<>();

		List<HttpCookie> cookiesFromTheManager = cookieManager.getCookieStore().getCookies();

		for (HttpCookie cookie : cookiesFromTheManager) {
			cookies.put(cookie.getName(), cookie.getValue());
		}

		if (cookies.containsKey("JSESSIONID") && cookies.containsKey("CASTGC")) {
			return cookies;
		}

		LOGGER.debug("acquireCookies");

		String responseBody = callLoginGetAndObtainResponse(s_sCmemsCasUrl, params);

		Map<String, String> hiddenElementsFromHtml = extractHiddenElementsFromHtml(responseBody);

		String jsessionidFromHtml = extractJsessionidFromHtml(responseBody);

		Map<String, String> payload = preparePayloadForLoginPost(hiddenElementsFromHtml, username, password);

		cookies = callLoginPostAndObtainCookies(s_sCmemsCasUrl, payload, params, jsessionidFromHtml);

		return cookies;
	}

	private String callLoginGetAndObtainResponse(String cmemsCasUrl, Map<String, String> params) {
		LOGGER.debug("callLoginGetAndObtainResponse");

		try {
			if (params != null) {
				String query = getQuery(params);
				cmemsCasUrl = cmemsCasUrl + "?" + query;

				return this.restTemplate.getForObject(cmemsCasUrl, String.class);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return null;
	}

	private Map<String, String> callLoginPostAndObtainCookies(String cmemsCasUrl, Map<String, String> payload, Map<String, String> params, String jsessionid) {
		LOGGER.debug("callLoginPostAndObtainCookies");

		Map<String, String> cookies = new HashMap<>();

		try {
			if (params != null) {
				String query = getQuery(params);
				cmemsCasUrl = cmemsCasUrl + "?" + query;
			}

			if (payload != null) {
				String query = getQuery(payload);
				cmemsCasUrl = cmemsCasUrl + "&" + query;
			}

			CookieManager cookieManager;
			if (CookieHandler.getDefault() == null) {
				cookieManager = new CookieManager();
				CookieHandler.setDefault(cookieManager);
			} else {
				cookieManager = ((CookieManager) CookieHandler.getDefault());
			}

			HttpHeaders headers = new HttpHeaders();
			headers.set("Accept", "*/*");
			headers.set("Content-Type", "application/x-www-form-urlencoded");
			headers.set("Connection", "keep-alive");
			headers.set("Accept-Encoding", "gzip, deflate");
			headers.set("User-Agent", "WasdiLib.Java");
			headers.set("Cookie", "JSESSIONID=" + jsessionid);
			
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(cmemsCasUrl, HttpMethod.GET, request, String.class);

			int responseCode = response.getStatusCode().value();

			if (responseCode == 200) {
				List<HttpCookie> cookiesFromTheManager = cookieManager.getCookieStore().getCookies();

				for (HttpCookie cookie : cookiesFromTheManager) {
					cookies.put(cookie.getName(), cookie.getValue());
				}
			}
		} catch (HttpClientErrorException e) {
			LOGGER.error("" + e.getRawStatusCode());
			LOGGER.error(e.getMessage());
			LOGGER.error(e.getResponseHeaders().toString());
			LOGGER.error("");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("");
		}

		return cookies;
	}

	private static void resetCookies() {
		LOGGER.debug("resetCookies");

		if (CookieHandler.getDefault() != null) {
			CookieManager cookieManager = ((CookieManager) CookieHandler.getDefault());
			cookieManager.getCookieStore().removeAll();
		}
	}

	private static Map<String, String> extractHiddenElementsFromHtml(String htmlSource) {
		LOGGER.debug("extractHiddenElementsFromHtml");

		Map<String, String> hiddenElementsFromHtml = new HashMap<>();

		if (htmlSource != null && !htmlSource.isEmpty()) {

			Document document = Jsoup.parse(htmlSource);

			for (Element form : document.select("form")) {
				for (Element fieldset : form.select("fieldset")) {
					for (Element input : fieldset.select("input")) {
						String type = input.attr("type");
	
						if ("hidden".equalsIgnoreCase(type)) {
							String name = input.attr("name");
							String value = input.attr("value");
	
							hiddenElementsFromHtml.put(name, value);
						}
					}
				}
			}
		}

		return hiddenElementsFromHtml;
	}

	private static String extractJsessionidFromHtml(String htmlSource) {
//		LOGGER.debug("extractJsessionidFromHtml");

		String jsessionid = null;

		Document document = Jsoup.parse(htmlSource);

		for (Element form : document.select("form")) {
			String idAttribute = form.attr("id");

			if ("authentification".equalsIgnoreCase(idAttribute)) {
				String actionAttribute = form.attr("action");

				if (actionAttribute != null && !actionAttribute.isEmpty()) {
					jsessionid = extractCookie(actionAttribute);
				}
			}
		}

		return jsessionid;
	}

	private static String extractCookie(String action) {
//		LOGGER.debug("extractCookie");

		return action.substring(action.indexOf("jsessionid=") + "jsessionid=".length(), action.indexOf("?"));
	}

	private static Map<String, String> preparePayloadForLoginPost(Map<String, String> hiddenElementsFromHtml, String username, String password) {
//		LOGGER.debug("preparePayloadForLoginPost");

		Map<String, String> payload = new HashMap<>();
		payload.put("username", username);
		payload.put("password", password);

		hiddenElementsFromHtml.forEach(payload::put);

		return payload;
	}

	private static String getQuery(Map<String, String> params) throws UnsupportedEncodingException {
//		LOGGER.debug("getQuery");

		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (Entry<String, String> entry : params.entrySet()) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(entry.getKey());
			result.append("=");
			result.append(entry.getValue());
		}

		return result.toString();
	}

}
