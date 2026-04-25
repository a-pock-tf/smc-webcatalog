package com.smc.webcatalog.util;

import java.util.ArrayList;
import java.util.List;

public class S3SDetailResult extends S3SResult {

	private String modelNo = "";
	private String productName = "";
	private Serial serial;
	private String picture = "";
	private String catalog = "";
	private String cadParameter = "";
	private String typeID = "";
	private RelatedProducts relatedProducts;

	private List<Prop> properties = new ArrayList<S3SDetailResult.Prop>();
	private List<Split> splits = new ArrayList<S3SDetailResult.Split>();
	private List<String> typeIdJapan = new ArrayList<String>();
	private Consolidation consolidation;

	public class Consolidation {
		public String typeID;
		public String consolidatedDate;
		public String newTypeID;
		public String description;
		public String detailDes;
		public String picFile;
		public String cataFile;
		public String desType;
		public String newModel;
		public String newProperty;
	}
	public class Split {

		private String isDefaultWay = "";

		private List<SplitDetail> details = new ArrayList<SplitDetail>();

		public List<SplitDetail> getDetails() {
			return details;
		}

		public void setDetails(List<SplitDetail> details) {
			this.details = details;
		}

		public String getIsDefaultWay() {
			return isDefaultWay;
		}

		public void setIsDefaultWay(String isDefaultWay) {
			this.isDefaultWay = isDefaultWay;
		}

	}

	public class SplitDetail {
		private String modelNo = "";
		private String quantity = "";

		public String getModelNo() {
			return modelNo;
		}

		public void setModelNo(String modelNo) {
			this.modelNo = modelNo;
		}

		public String getQuantity() {
			return quantity;
		}

		public void setQuantity(String quantity) {
			this.quantity = quantity;
		}

	}

	public class RelatedProducts {
		private List<Accessories> accessories = new ArrayList<Accessories>();
		private List<SuitableSeries> suitableSeries = new ArrayList<SuitableSeries>();

		public List<Accessories> getAccessories() {
			return accessories;
		}

		public void setAccessories(List<Accessories> accessories) {
			this.accessories = accessories;
		}

		public List<SuitableSeries> getSuitableSeries() {
			return suitableSeries;
		}

		public void setSuitableSeries(List<SuitableSeries> suitableSeries) {
			this.suitableSeries = suitableSeries;
		}

	}

	public class SuitableSeries {
		private String typeID;
		private String name;
		private String picture;
		private String catalog;

		public String getTypeID() {
			return typeID;
		}

		public void setTypeID(String typeID) {
			this.typeID = typeID;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCatalogByLang(String lang) {

			String url = this.getCatalog();
			if (url != null) {
				url = url.replace("{lan}", lang);
				//com.whitebase.common.Log.log("url="+url);
			}
			return url;
		}

		public String getCatalogJa() {
			return getCatalogByLang("ja-JP");
		}

		//size = S,M,L
		public String getPictureBySize(String size) {

			String url = this.getPicture();
			if (url != null) {
				url = url.replace("{size}", size);
				//com.whitebase.common.Log.log("url="+url);
			}
			return url;
		}

		public String getPictureS() {

			return this.getPictureBySize("S");
		}

		public String getPicture() {
			return picture;
		}

		public void setPicture(String picture) {
			this.picture = picture;
		}

		public String getCatalog() {
			return catalog;
		}

		public void setCatalog(String catalog) {
			this.catalog = catalog;
		}

	}

	public class Prop {
		String name = "";
		String value = "";
		String description = "";
		Integer position;
		String valuePic = "";
		String valuePdf = "";

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public class PicUrl {
		String s;
		String m;
		String l;

		public String getS() {
			return s;
		}

		public void setS(String s) {
			this.s = s;
		}

		public String getM() {
			return m;
		}

		public void setM(String m) {
			this.m = m;
		}

		public String getL() {
			return l;
		}

		public void setL(String l) {
			this.l = l;
		}

	}

	public class Serial {

		String id = "";
		String name = "";
		String seriesHome = "";
		String series = "";
		String description = "";
		PicUrl picUrl;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSeriesHome() {
			return seriesHome;
		}

		public void setSeriesHome(String seriesHome) {
			this.seriesHome = seriesHome;
		}

		public String getSeries() {
			return series;
		}

		public void setSeries(String series) {
			this.series = series;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public void setPicUrl(PicUrl picUrl) {
			this.picUrl = picUrl;
		}

		public PicUrl getPicUrl() {
			return picUrl;
		}

	}

	public class Accessories {

		String model;
		String description;
		String picture;

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getPicture() {
			return picture;
		}

		public void setPicture(String picture) {
			this.picture = picture;
		}

		//size = S,M,L
		public String getPictureBySize(String size) {

			String url = this.getPicture();
			if (url != null) {
				url = url.replace("{size}", size);
				//com.whitebase.common.Log.log("url="+url);
			}
			return url;
		}

		public String getPictureL() {

			return this.getPictureBySize("L");
		}

		public String getPictureM() {

			return this.getPictureBySize("M");
		}

		public String getPictureS() {

			return this.getPictureBySize("S");
		}

	}

	public List<Prop> getProperties() {
		return properties;
	}

	public void setProperties(List<Prop> properties) {
		this.properties = properties;
	}

	public String getModelNo() {
		return modelNo;
	}

	public void setModelNo(String modelNo) {
		this.modelNo = modelNo;
	}

	//size = S,M,L
	public String getPictureBySize(String size) {

		String url = this.getPicture();
		if (url != null) {
			url = url.replace("{size}", size);
			//com.whitebase.common.Log.log("url="+url);
		}
		return url;
	}

	public String getPictureL() {

		return this.getPictureBySize("L");
	}

	public String getPictureM() {

		return this.getPictureBySize("M");
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getCadParameter() {
		return cadParameter;
	}

	public void setCadParameter(String cadParameter) {
		this.cadParameter = cadParameter;
	}

	public void setRelatedProducts(RelatedProducts relatedProducts) {
		this.relatedProducts = relatedProducts;
	}

	public Serial getSerial() {
		return serial;
	}

	public void setSerial(Serial serial) {
		this.serial = serial;
	}

	public RelatedProducts getRelatedProducts() {
		return relatedProducts;
	}

	public List<Split> getSplits() {
		return splits;
	}

	public void setSplits(List<Split> splits) {
		this.splits = splits;
	}

	public String getTypeID() {
		return typeID;
	}

	public void setTypeID(String typeID) {
		this.typeID = typeID;
	}

	public void setTypeIdJapan(List<String> typeIdJapan) {
		this.typeIdJapan = typeIdJapan;
	}

	public List<String> getTypeIdJapan() {
		return typeIdJapan;
	}

	public Consolidation getConsolidation() {
		return consolidation;
	}

	public void setConsolidation(Consolidation consolidation) {
		this.consolidation = consolidation;
	}

}
