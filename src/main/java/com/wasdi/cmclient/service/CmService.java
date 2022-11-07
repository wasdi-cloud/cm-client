package com.wasdi.cmclient.service;

import java.util.List;

import com.wasdi.cmclient.model.xml.CmServiceProduct;
import com.wasdi.cmclient.model.xml.ProductMetadataInfo;

public interface CmService {

	List<CmServiceProduct> getCatalog();

	ProductMetadataInfo getProductMetadataInfo(String uri);

}
