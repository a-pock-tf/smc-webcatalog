 package com.smc.discontinued.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "discontinued_series")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class DiscontinuedSeries extends DiscontinuedBaseModel {

	//初期化は明示的に
	public DiscontinuedSeries() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		setId(null);
		setLang("ja-jp");
		Date date = new Date();
		setDate(sdf.format(date));
		setCtime(date);
		setMtime(date);
		setActive(true);
	}

	// カテゴリのID
	@TextIndexed
	private String categoryId;

	// 一覧表示
	private String seriesName; // B
	private String seriesId; // C -oldが付くID
	private String name; // D
	private String series; // E
	private String newSeries; // G

	// 詳細表示
	private String image; // I
	private String newImage; // N
	private String catalogLink; // P
	private String newCatalogLink; // O
	private String manualLink; // Q
	private String compatibility; // R
	private String other; // S
	private String caution; // T
	private String comparison; // U

	private String date; // 日付はソートに利用。detailの１番目の生産終了日 L

	private String detail; // 生産終了機種、代替機種の型式と生産終了日、新ID(J/L/M/W) JSON


//	以下は使っていない
//	F列：ｼﾘｰｽﾞ１ID
//	K列：対象型式ID
//	V列：注意事項リンク先

	public String getEndDate() {
		String ret = "";
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(detail);
			ret = root.get(1).get(1).asText();
		} catch (Exception e) {
			// TODO: handle exception
		}
		return ret;
	}

	public List<String[]> getDetailList()
	{
		List<String[]> ret = new LinkedList<String[]>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(detail);
			Iterator<JsonNode> iter =root.iterator();
			while(iter.hasNext() ) {
				JsonNode jn = (JsonNode) iter.next();
				if (jn.get(0).asText().equals("Series")) continue; // タイトル行
				int size = jn.size();
				String[] arr = new String[size];
				for(int i = 0; i < size; i++) {
					arr[i] = jn.get(i).asText();
				}
				ret.add(arr);
			}
		}catch (Exception e) {
			// TODO: handle exception
		}
		return ret;
	}

	public DiscontinuedSeries Copy() {
		DiscontinuedSeries c2 = new DiscontinuedSeries();

		c2.setId(null);
		c2.setSeriesName(getSeriesName());
		c2.setSeriesId(getSeriesId());
		c2.setCategoryId(getCategoryId());
		c2.setName(getName());
		c2.setLang(getLang());

		c2.setSeries(getSeries());
		c2.setImage(getImage());
		c2.setNewSeries(getNewSeries());
		c2.setNewImage(getNewImage());
		c2.setCatalogLink(getCatalogLink());

		c2.setNewCatalogLink(getNewCatalogLink());
		c2.setManualLink(getManualLink());
		c2.setCompatibility(getCompatibility());
		c2.setOther(getOther());
		c2.setCaution(getCaution());
		c2.setComparison(getComparison());
		c2.setDate(getDate());

		c2.setDetail(getDetail());
		c2.setActive(true);
		c2.setCtime(getCtime());
		c2.setMtime(getMtime());
		c2.setOrder(1);
		c2.setState(getState());
		c2.setUser(getUser());

		// langRef stateRefは除外
		return c2;
	}

}
