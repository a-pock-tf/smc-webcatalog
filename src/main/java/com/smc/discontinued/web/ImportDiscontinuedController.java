package com.smc.discontinued.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import com.smc.discontinued.config.DiscontinuedConfig;
import com.smc.discontinued.dao.DiscontinuedCategoryRepository;
import com.smc.discontinued.dao.DiscontinuedSeriesRepository;
import com.smc.discontinued.model.DiscontinuedCategory;
import com.smc.discontinued.model.DiscontinuedModelState;
import com.smc.discontinued.model.DiscontinuedSeries;
import com.smc.webcatalog.model.User;
import com.smc.webcatalog.web.ScreenStatusHolder;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@SessionAttributes(value= {"SessionScreenState", "SessionUser"})
public class ImportDiscontinuedController {

	@Autowired
	DiscontinuedCategoryRepository disconCategoryRepo;

	@Autowired
	DiscontinuedSeriesRepository discontinuedSeriesRepository;

	private class slug{
		String name;
		String slug;
	}
	private String _slug = "方向制御機器	directional-control-valves\r\n" +
			"エアシリンダ	air-cylinders\r\n" +
			"ロータリアクチュエータ/エアチャック	rotary-actuators-air-grippers\r\n" +
			"真空用機器	vacuum-equipment\r\n" +
			"圧縮空気清浄化機器	air-preparation-equipment\r\n" +
			"管継手	fittings-and-tubing\r\n" +
			"駆動制御機器	flow-control-equipment-speed-controllers\r\n" +
			"モジュラF.R.L./圧力制御機器	modular-frl-units-pressure-control-equipment\r\n" +
			"サイレンサ	silencers-exhaust-cleaners-blow-guns-pressure-gauges\r\n" +
			"スイッチ／センサ	switches-sensors-controllers\r\n" +
			"静電対策機器／除電機器	static-neutralization-equipment-ionizers\r\n" +
			"流体制御用機器	process-valves\r\n" +
			"薬液用バルブ	chemical-liquid-valves-fittings-needle-valves-tubing\r\n" +
			"プロセスポンプ	process-pumps-diaphragm-pumps\r\n" +
			"温調機器	temperature-control-equipment\r\n" +
			"工業用フィルタ	industrial-filters-sintered-metal-elements\r\n" +
			"電動アクチュエータ	electric-actuators-cylinders\r\n" +
			"高真空機器	high-vacuum-equipment\r\n" +
			"計装機器	pneumatic-instrumentation-equipment\r\n" +
			"油圧用機器	hydraulic-equipment";

	// これでカテゴリ、シリーズすべてインポート。
	// インポート後、下記パス変換！！
	@RequestMapping("/login/admin/discontinued/importDiscontinued")
	public ModelAndView importCategory(ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		mav.setViewName("/login/admin/discontinued/category/list");

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		try {
			BufferedReader br = null;
	        String score_csv = "src/main/resources/csv/discontinued_category_ja_20210930-2.csv";
	        File file = new File(score_csv);
            br = new BufferedReader(new FileReader(file));
            String line;
            String[] data;
            br.readLine();

//    		db.dropCollection(DiscontinuedCategory.class);
//    		db.dropCollection(DiscontinuedSeries.class);

    		disconCategoryRepo.deleteAll();

            while ((line = br.readLine()) != null) {
                data = line.split("\t");
                DiscontinuedCategory c = new DiscontinuedCategory();
                c.setName(data[1]);
                c.setSlug(getSlug(data[1]));
                c.setOldId(data[0]);
                c.setState(DiscontinuedModelState.TEST);
                c.setOrder(Integer.parseInt(data[6]));
                c.setUser(s_user);

                DiscontinuedCategory ret = disconCategoryRepo.save(c);
                String langBaseId = ret.getId();

                DiscontinuedCategory cP = c.Copy();
                cP.setStateRefId(ret.getId());
                cP.setState(DiscontinuedModelState.PROD);
                DiscontinuedCategory retP = disconCategoryRepo.save(cP);

                DiscontinuedCategory cE = new DiscontinuedCategory();
                cE.setId(null);
                cE.setLangRefId(langBaseId);
                cE.setName(data[3]);
                cE.setSlug(getSlug(data[1]));
                cE.setLang("en-jp");
                cE.setOldId(data[2]);
                cE.setOrder(Integer.parseInt(data[6]));
                cE.setUser(s_user);

                ret = disconCategoryRepo.save(cE);

                DiscontinuedCategory cPE = cE.Copy();
                cPE.setLangRefId(retP.getId());
                cPE.setStateRefId(ret.getId());
                cPE.setState(DiscontinuedModelState.PROD);

                disconCategoryRepo.save(cPE);

                DiscontinuedCategory cZ = new DiscontinuedCategory();
                cZ.setId(null);
                cZ.setLangRefId(langBaseId);
                cZ.setName(data[5]);
                cZ.setSlug(getSlug(data[1]));
                cZ.setLang("zh-cn");
                cZ.setOldId(data[4]);
                cZ.setOrder(Integer.parseInt(data[6]));
                cZ.setUser(s_user);

                ret = disconCategoryRepo.save(cZ);

                DiscontinuedCategory cPZ = cZ.Copy();
                cPZ.setLangRefId(retP.getId());
                cPZ.setStateRefId(ret.getId());
                cPZ.setState(DiscontinuedModelState.PROD);

                disconCategoryRepo.save(cPZ);
            }
            br.close();

            // シリーズ
            String[] csv = {"src/main/resources/csv/discontinued_ja_20220108.csv",
            		"src/main/resources/csv/discontinued_en_20220111.csv",
            		"src/main/resources/csv/discontinued_zh_20211116.csv"};

            discontinuedSeriesRepository.deleteAll();

            List<DiscontinuedSeries> jaList = new LinkedList<DiscontinuedSeries>(); // for langRef
            List<DiscontinuedSeries> jaProdList = new LinkedList<DiscontinuedSeries>();

            for(int i = 0; i < 3; i++) {
	            file = new File(csv[i]);
	            br = new BufferedReader(new FileReader(file));
	            br.readLine();
	            String pdfUrl =DiscontinuedConfig.oldPDFUrl;
	            String lang = "ja-jp";
	            String shortLang = "";
	            if (i == 1) {
	            	lang = "en-jp";
	            	shortLang = "e";
	            	pdfUrl = pdfUrl.replace("ja/", "en/");
	            }
	            else if (i == 2) {
	            	lang = "zh-cn";
	            	shortLang = "zh";
	            	pdfUrl = pdfUrl.replace("ja/", "en/");
	            }

	            List<DiscontinuedCategory> list = disconCategoryRepo.listAll(lang, DiscontinuedModelState.TEST, null);
	            List<DiscontinuedCategory> prodList = disconCategoryRepo.listAll(lang, DiscontinuedModelState.PROD, null);

	            DiscontinuedSeries s = null;
	            DiscontinuedSeries ps = null;
	            int ln = 0;
	            while ((line = br.readLine()) != null) {
	                data = line.split("\t");
	                try {
		                ln = Integer.parseInt(data[7]); // H

		                if (ln == 1) {
			                s = new DiscontinuedSeries();
			                s.setCategoryId(getCategoryId(data[0], list));
			                s.setSeriesName(data[1]);
			                s.setSeriesId(data[2]);
			                s.setName(data[3]);
			                s.setSeries(data[4]);
			                s.setNewSeries(data[6]); // G

			                if (data[8] != null && data[8].isEmpty() == false) s.setImage(DiscontinuedConfig.oldImageUrl+ data[8]); // I
			                // 代替品がない場合はここまでで終了の場合あり。以下、かならずカラチェック！
			                if (data.length > 13 && data[13] != null && data[13].isEmpty() == false) s.setNewImage(DiscontinuedConfig.oldImageUrl+ data[13]); // N
			                if (data.length > 14 && data[14] != null && data[14].isEmpty() == false) {
			                	if (data[14].indexOf("/") == 0) {
			                		s.setNewCatalogLink(data[14]);
			                	} else {
			                		s.setNewCatalogLink(pdfUrl+ data[14]);
			                	}
			                }
			                if (data.length > 15 && data[15] != null && data[15].isEmpty() == false) {// 代替品がない場合はここまでで終了。以下、かならずカラチェック！
			                	if (data[15].indexOf("/") == 0) {
			                		s.setCatalogLink(data[15]);
			                	} else {
			                		s.setCatalogLink(pdfUrl+ data[15]);
			                	}
			                }
			                if (data.length > 16 && data[16] != null && data[16].isEmpty() == false) {
			                	if (data[16].indexOf("/") == 0) {
			                		s.setManualLink(data[16]);
			                	} else {
			                		s.setManualLink(pdfUrl+ data[16]);
			                	}
			                }
		                	if (data.length > 17) s.setCompatibility(data[17]);
		                	if (data.length > 18) s.setOther(data[18]);
		                	if (data.length > 19) s.setCaution(data[19]); // T
			                if (data.length > 20 && data[20] != null && data[20].isEmpty() == false) s.setComparison(DiscontinuedConfig.oldCompareUrl+data[20]);

			                s.setLang(lang);

			                if (data[11] != null && data[11].isEmpty() == false) {
				                Date tmp = format.parse(data[11]);
				                s.setDate(format.format(tmp));
			                } else {
			                	s.setDate("");
			                }

			        		// data[9] data[11] data[12] data[22]
			        		String spec = "[[";
			        		spec+="\"Series\", \"End date\", \"Replacement Series\", \"SeriesID\" ";
			        		spec += "],[";
			        		if (data.length > 22) spec += "\"" + data[9] + "\",\""+data[11]+"\",\"" + data[12] + "\",\"" + data[22]+"\"";
			        		else if (data.length > 12) spec += "\"" + data[9] + "\",\""+data[11]+"\",\"" + data[12] + "\",\"\"";
			        		else if (data.length > 11)  spec += "\"" + data[9] + "\",\""+data[11]+"\",\"\",\"\"";
			        		spec+= "]]";
			        		s.setDetail(spec);
			        		if (i != 0) s.setLangRefId(getLangRefId(s.getSeriesId(), shortLang, jaList));

		                } else {
		                	// 追記
		                	String spec = s.getDetail().substring(0, s.getDetail().length()-1);
			        		spec += ",[";
		                	Date tmp = format.parse(data[11]);
			        		if (data.length > 22) {
			        			spec += "\"" + data[9] + "\",\""+format.format(tmp)+"\",\"" + data[12] + "\",\"" + data[22]+"\"";
			        		} else if (data.length > 12) {
			        			spec += "\"" + data[9] + "\",\""+format.format(tmp)+"\",\"" + data[12] + "\",\"\"";
			        		} else if (data.length > 11) {
			        			spec += "\"" + data[9] + "\",\""+data[11]+"\",\"\",\"\"";
			        		}
			        		spec+= "]]";
			        		s.setDetail(spec);
			        		ps.setDetail(spec);
		                }

		                s = discontinuedSeriesRepository.save(s);
		                if (i == 0) jaList.add(s);
		                if (ln == 1) {
			                ps = s.Copy();
			                ps.setCategoryId(getCategoryId(data[0], prodList));
			                ps.setStateRefId(s.getId());
			                ps.setState(DiscontinuedModelState.PROD);
			                if (i != 0) ps.setLangRefId(getLangRefId(ps.getSeriesId(), shortLang, jaProdList));
		                }

		                ps = discontinuedSeriesRepository.save(ps);
		                if (i == 0) jaProdList.add(ps);
	                } catch (Exception e) {
	                	log.debug("ImportDiscontinuedController. br.readLine() line="+ln);
	                	log.debug("ImportDiscontinuedController. br.readLine() e="+e.getStackTrace());
	                }
	            }
	            br.close();
            }

        } catch (Exception e) {
        	log.debug("ImportDiscontinuedController. e="+e.getStackTrace());
			log.debug("ImportDiscontinuedController. e="+e.getMessage());
		}

		return mav;
	}
	@RequestMapping("/login/admin/discontinued/importChangePath")
	public ModelAndView importChangePath(ModelAndView mav,
			@ModelAttribute("SessionUser") User s_user,
			@ModelAttribute("SessionScreenState") ScreenStatusHolder s_state) {

		{
			List<DiscontinuedSeries> list = discontinuedSeriesRepository.listAll("ja-jp", DiscontinuedModelState.TEST);
			ChangePath(list);
			list = discontinuedSeriesRepository.listAll("ja-jp", DiscontinuedModelState.PROD);
			ChangePath(list);
			list = discontinuedSeriesRepository.listAll("en-jp", DiscontinuedModelState.TEST);
			ChangePath(list);
			list = discontinuedSeriesRepository.listAll("en-jp", DiscontinuedModelState.PROD);
			ChangePath(list);
			list = discontinuedSeriesRepository.listAll("zh-cn", DiscontinuedModelState.TEST);
			ChangePath(list);
			list = discontinuedSeriesRepository.listAll("zh-cn", DiscontinuedModelState.PROD);
			ChangePath(list);
		}
		mav.setViewName("/login/admin/discontinued/category/list");
		List<DiscontinuedCategory> list = disconCategoryRepo.listAll("ja-jp", DiscontinuedModelState.TEST, null);
		mav.addObject("list", list);
		return mav;
	}
	private String addPath ="/upfiles/etc";
	private void ChangePath(List<DiscontinuedSeries> list) {
		List<DiscontinuedSeries> save = new LinkedList<DiscontinuedSeries>();
		for(DiscontinuedSeries s : list) {
			if (s.getImage() != null && s.getImage().isEmpty()==false) s.setImage(addPath + s.getImage());
			if (s.getNewImage() !=  null && s.getNewImage().isEmpty() == false) s.setNewImage(addPath + s.getNewImage());
			if (s.getCatalogLink() != null && s.getCatalogLink().isEmpty()==false) s.setCatalogLink(addPath + s.getCatalogLink());
			if (s.getNewCatalogLink() != null && s.getNewCatalogLink().isEmpty()==false) s.setNewCatalogLink(addPath + s.getNewCatalogLink());
			if (s.getComparison() != null && s.getComparison().isEmpty() == false) s.setComparison(addPath + s.getComparison());
			if (s.getManualLink() != null && s.getManualLink().isEmpty() == false) s.setManualLink(addPath + s.getManualLink());
			save.add(s);
		}
		discontinuedSeriesRepository.saveAll(save);
	}

	private List<slug> slugList = new LinkedList<ImportDiscontinuedController.slug>();
	private String getSlug(String name) {
		String ret = null;
		if (slugList != null && slugList.size() == 0) {
			String[] arr = _slug.split("\r\n");
			for(String tmp : arr) {
				String[] arr2 = tmp.split("\t");
				ImportDiscontinuedController.slug s = new slug();
				s.name = arr2[0].trim();
				s.slug = arr2[1].trim();
				slugList.add(s);
			}
		}
		for(slug s : slugList) {
			if (s.name.equals(name)) {
				ret = s.slug;
				break;
			}
		}
		return ret;
	}
	private String getLangRefId(String oldId, String sLang, List<DiscontinuedSeries> list) {
		String ret = null;
		for(DiscontinuedSeries c : list) {
			if (oldId != null && oldId.equals(c.getSeriesId()+"-"+sLang)) {
				ret = c.getId();
				break;
			}
		}
		return ret;
	}
	private String getCategoryId(String oldId, List<DiscontinuedCategory> list) {
		String ret = null;
		for(DiscontinuedCategory c : list) {
			if (c.getOldId().equals(oldId)) {
				ret = c.getId();
				break;
			}
		}
		return ret;
	}
}
