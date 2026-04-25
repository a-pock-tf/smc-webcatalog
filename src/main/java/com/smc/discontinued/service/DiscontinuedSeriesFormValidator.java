package com.smc.discontinued.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.discontinued.dao.DiscontinuedSeriesRepository;
import com.smc.discontinued.dao.DiscontinuedSeriesTemplateImpl;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.discontinued.model.DiscontinuedSeriesForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class DiscontinuedSeriesFormValidator {

	@Autowired
	DiscontinuedSeriesRepository discontinuedSeriesRepository;

	@Autowired
	DiscontinuedSeriesTemplateImpl disconSeriesTemplate;

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");

	// 新規時のチェック
	public boolean validateNew(BindingResult result, DiscontinuedSeriesForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}
		if (form.getDate() != null && form.getDate().isEmpty()) {
			form.setDate("");
		} else {
			try {
				Date tmp = sdf.parse(form.getDate());
				form.setDate(sdf.format(tmp));
			}catch (Exception e) {
				result.rejectValue("date", "my.validation.test", new String[] { form.getDate() }, "");
			}
		}
/*
 * 以下の項目は同一チェック
 * 2022/1/21
 * Discontinued image
  　　 例） VHK-old.jpg
Replacement image
  　　 例） VHK-A.jpg
Discontinued catalogLink
  　　 例） VHK-old.pdf
Product comparison details PDF
  　　 例） VHK-comp.pdf
 */
		if (form.getImage().isEmpty() == false) {
			String fn = getFileName(form.getImage());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByImage(fn);
				if (list.size() > 0) {
					result.rejectValue("image", "my.validation.test", new String[] { form.getImage() }, "");
				}
			} else {
				result.rejectValue("image", "my.validation.test", new String[] { form.getImage() }, "");
			}
		}
		if (form.getNewImage().isEmpty() == false) {
			String fn = getFileName(form.getNewImage());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByReplacementImage(fn);
				if (list.size() > 0) {
					result.rejectValue("newImage", "my.validation.test", new String[] { form.getNewImage() }, "");
				}
			} else {
				result.rejectValue("image", "my.validation.test", new String[] { form.getNewImage() }, "");
			}
		}
		if (form.getCatalogLink().isEmpty() == false) {
			String fn = getFileName(form.getCatalogLink());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByCatalogLink(getFileName(form.getCatalogLink()));
				if (list.size() > 0) {
					result.rejectValue("catalogLink", "my.validation.test", new String[] { form.getCatalogLink() }, "");
				}
			} else {
				result.rejectValue("image", "my.validation.test", new String[] { form.getCatalogLink() }, "");
			}
		}
		if (form.getComparison().isEmpty() == false) {
			String fn = getFileName(form.getComparison());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByComparisonDetailsPDF(getFileName(form.getComparison()));
				if (list.size() > 0) {
					result.rejectValue("comparison", "my.validation.test", new String[] { form.getComparison() }, "");
				}
			} else {
				result.rejectValue("image", "my.validation.test", new String[] { form.getComparison() }, "");
			}
		}
		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, DiscontinuedSeriesForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}
		if (form.getDate() != null && form.getDate().isEmpty()) {
			form.setDate("");
		} else {
			try {
				Date tmp = sdf.parse(form.getDate());
				form.setDate(sdf.format(tmp));
			}catch (Exception e) {
				result.rejectValue("date", "my.validation.test", new String[] { form.getDate() }, "");
			}
		}


		if (form.getImage().isEmpty() == false) {
			String fn = getFileName(form.getImage());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByImage(fn);
				if (list.size() > 0) {
					boolean isFind = false;
					for(DiscontinuedSeries s : list) {
						if (s.getId().equals(form.getId()) == false) {
							isFind = true;
							break;
						}
					}
					if (isFind) {
						result.rejectValue("image", "my.validation.test", new String[] { form.getImage() }, "");
					}
				}
			}
		}
		if (form.getNewImage().isEmpty() == false) {
			String fn = getFileName(form.getNewImage());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByReplacementImage(fn);
				if (list.size() > 0) {
					boolean isFind = false;
					for(DiscontinuedSeries s : list) {
						if (s.getId().equals(form.getId()) == false) {
							isFind = true;
							break;
						}
					}
					if (isFind) {
						result.rejectValue("newImage", "my.validation.test", new String[] { form.getNewImage() }, "");
					}
				}
			}
		}
		if (form.getCatalogLink().isEmpty() == false) {
			String fn = getFileName(form.getCatalogLink());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByCatalogLink(getFileName(form.getCatalogLink()));
				if (list.size() > 0) {
					boolean isFind = false;
					for(DiscontinuedSeries s : list) {
						if (s.getId().equals(form.getId()) == false) {
							isFind = true;
							break;
						}
					}
					if (isFind) {
						result.rejectValue("catalogLink", "my.validation.test", new String[] { form.getCatalogLink() }, "");
					}
				}
			}
		}
		if (form.getComparison().isEmpty() == false) {
			String fn = getFileName(form.getComparison());
			if (fn != null) {
				List<DiscontinuedSeries> list = disconSeriesTemplate.findByComparisonDetailsPDF(getFileName(form.getComparison()));
				if (list.size() > 0) {
					boolean isFind = false;
					for(DiscontinuedSeries s : list) {
						if (s.getId().equals(form.getId()) == false) {
							isFind = true;
							break;
						}
					}
					if (isFind) {
						result.rejectValue("comparison", "my.validation.test", new String[] { form.getComparison() }, "");
					}
				}
			}
		}

		ret = true;
		return ret;
	}

	/**
	 *
	 * @return
	 */
	private String getFileName(String path) {
		String ret = null;

		if (path != null && path.isEmpty() == false) {
			String[] arr = path.split("/");
			if (arr.length > 1) {
				ret = arr[arr.length-1];
			} else {
				ret = path;
			}
		}
		return ret;
	}
}
