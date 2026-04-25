package com.smc.webcatalog.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "narrow_down_value")
@Getter
@Setter
@ToString(callSuper = true, includeFieldNames = true)
public class NarrowDownValue extends BaseModel {

	private String categoryId; // TEST or PROD categoryのmongoDBのobjectID
	
	private String seriesId; // TEST or PROD categoryのmongoDBのobjectID
	
	private String columnId; // TEST or PROD NarrowDownCategoryColumnのobjectID
	
	private String[] param; // select radio checkboxの選択肢

	private int start; // rangeの場合に利用
	private int end; // rangeの場合に利用
	
	public String getRangeParam() {
		String ret = param[0];
		if (param == null || param.length == 0) {
			if (start > 0 || end > 0) {
				ret = start +"-"+end;
			}
		}
		return ret;
	}
	public void setRangeParam(String p) {
		if (p != null && p.isEmpty() == false) {
			String[] arr = p.split("-");
			if (arr.length == 2) {
				start = Integer.parseInt(arr[0]);
				end = Integer.parseInt(arr[1]);
			}
		}
	}
	
	// TESTからPROD用にパラメータをコピー
	// 以下はPRODのIDなので、ここではコピーしない
	// id categoryId seriesId columnId stateRefId langRefId
	public void setUpdateParam(NarrowDownValue s) {
		param = s.getParam();
		start = s.getStart();
		end = s.getEnd();
		super.setLang(s.getLang());
		super.setOrder(s.getOrder());
	}
}
