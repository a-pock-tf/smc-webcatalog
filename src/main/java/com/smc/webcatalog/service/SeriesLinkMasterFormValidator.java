package com.smc.webcatalog.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.smc.webcatalog.dao.SeriesLinkMasterRepository;
import com.smc.webcatalog.model.ErrorObject;
import com.smc.webcatalog.model.SeriesLinkMaster;
import com.smc.webcatalog.model.SeriesLinkMasterForm;

import lombok.extern.slf4j.Slf4j;

/**
 * FormValidation用のクラスBindingResultを受け取り、エラーセット
 * 簡単な実装、かつ泥臭い仕事
 * @author miyasit
 *
 */
@Service
@Slf4j
public class SeriesLinkMasterFormValidator {

	@Autowired
	SeriesLinkMasterRepository repo;

	@Autowired
	SeriesLinkMasterServiceImpl service;

	// 新規時のチェック
	public boolean validateNew(BindingResult result, SeriesLinkMasterForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		// 同一nameが存在するか
		if (service.isNameExists(form.getName(), form.getLang(), obj)) {
			result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
		}

		ret = true;
		return ret;
	}

	// 更新時のチェック(共通処理が増えたら、細かく分割)
	public boolean validateUpate(BindingResult result, SeriesLinkMasterForm form) {
		boolean ret = false;
		//すでにエラーがあれば、チェックしない
		//Annotationでのエラーを優先
		if (result.hasErrors()) {
			return ret;
		}

		ErrorObject obj = new ErrorObject();
		Optional<SeriesLinkMaster> m = repo.findById(form.getId());
		if (m.isPresent()) {
			SeriesLinkMaster master = m.get();
			// nameが変更された場合、同一nameが存在するか
			if (master.getName().equals(form.getName()) == false) {
				master.setName(form.getName());
				if (service.isNameExists(master.getName(), master.getLang(), obj)){
					result.rejectValue("name", "my.validation.test", new String[] { form.getName() }, "");
				}
			}

			ret = true;
		}


		//2) その他のチェック
		// private chekcABC();

		return ret;

	}

}
