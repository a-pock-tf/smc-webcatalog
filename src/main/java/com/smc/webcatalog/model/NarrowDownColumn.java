package com.smc.webcatalog.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "narrow_down_column")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class NarrowDownColumn extends BaseModel {

	private String categoryId; // TEST or PROD categoryのmongoDBのobjectID
	
	private String title; // 項目名 「チューブ内径、ロッドタイプ」など

	private String select; // select radio checkbox range
	
	private String[] values; // すべての選択肢 rangeの場合は最大、最小

	// active はBaseModel
	
	// TESTからPROD用にパラメータをコピー
	// id seriesId stateRefId langRefIdはここではコピーしない
	public void setUpdateParam(NarrowDownColumn s) {
		title = s.getTitle();
		select = s.getSelect();
		values = s.getValues();
		super.setActive(s.isActive()); // 表示、非表示
		super.setLang(s.getLang());
		super.setOrder(s.getOrder());
	}
	
	/**
	 *  valueの重複をみて、すべてのparamを保持
	 * @param val 1,2,3
	 * @note カラは保持しない。valueでは保持。2024/12/27
	 */
	public void setParamValue(String[] val) {
		if (val != null && val.length > 0 && val[0].isEmpty() == false) {
			if (values == null || val.length == 0) values = val;
			else {
				List<String> list = Arrays.asList(values);
				String[] inArr = val;
				List<String> addList = new ArrayList<>();
				for (String tmp : inArr) {
					if (list.contains(tmp) == false) {
						addList.add(tmp);
					}
				}
				if (addList.size() > 0) {
					if (values.length == 1 && values[0].isEmpty()) values = addList.toArray(new String[addList.size()]);
					else {
						addList.addAll(0, list); // listは固定長で落ちるのでaddListの先頭へ
						values = addList.toArray(new String[addList.size()]);
					}
				}
			}
			if (select.equals("select")) {
				values = getSortedValues();
			}
		}
	}
	
	/**
	 * 
	 * @param 
	 * @apiNote すべて数値に置き換え可能なら数値の小さい順にソートして返す。
	 * @see 数値に置き換え可能ではない値が一つでもあればそのまま返す。
	 */
	public String[] getSortedValues() {
		try {
			Arrays.sort(values, (a, b) -> Double.compare(Double.parseDouble(a), Double.parseDouble(b)));
		} catch (NumberFormatException e) {
		}
	
		return values;
	}

	/**
	 * 
	 * @param val
	 * @apiNote val contains -(hyphen)
	 * @see valueの最大範囲を保持。2-3の次に2-5 が来れば [0]2 [1]5。 2-5の次に1-4 が来れば [0]1 [1]5
	 */
	public void setParamRange(String val) {
		if (val != null && val.isEmpty() == false && val.indexOf('-') > -1) {
			if (values == null || values.length == 0) values = val.split("-");
			else if (values[0].indexOf('.') > -1 || values[1].indexOf('.') > -1 || val.indexOf('.') > -1) { // .があればfloat
				String[] arr = val.split("-");
				float inS = Float.parseFloat(values[0]);
				float inE = Float.parseFloat(values[1]);
				float s = Float.parseFloat(arr[0]);
				float e = Float.parseFloat(arr[1]);
				if (s < inS) {
					values[0] = String.valueOf(s);
				}
				if (e > inE) {
					values[1] = String.valueOf(e);
				}
			} else {
				String[] arr = val.split("-");
				int inS = Integer.parseInt(values[0]);
				int inE = Integer.parseInt(values[1]);
				int s = Integer.parseInt(arr[0]);
				int e = Integer.parseInt(arr[1]);
				if (s < inS) {
					values[0] = String.valueOf(s);
				}
				if (e > inE) {
					values[1] = String.valueOf(e);
				}
			}
		}
	}
}
