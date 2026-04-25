package com.smc.webcatalog.model;

import org.apache.commons.lang.StringUtils;

public class ImpCategoryTemplate {

	// HeartCoreの文字をコピー
	// 最終更新：2021/12/3
	static String jpNameList = "IO-Link対応機器\r\n" +
			"ISO規格準拠製品\r\n" +
			"WEBカタログ\r\n" +
			"WEBカタログ-検索窓\r\n" +
			"アーク溶接工程向機器\r\n" +
			"エアシリンダ\r\n" +
			"クリーン／低発塵\r\n" +
			"コンパクトエアシリンダ\r\n" +
			"サイレンサ／エキゾーストクリーナ／ブローガン／圧力計\r\n" +
			"スイッチ／センサ／コントローラ\r\n" +
			"ノングリース 窒素（N<sub>2</sub>）対応機器\r\n" +
			"フッ素樹脂製機器/塩化ビニル製機器\r\n" +
			"プロセスガス用機器\r\n" +
			"プロセスポンプ（ダイヤフラム式ポンプ）\r\n" +
			"ミニフリーマウントシリンダ CUJ/CDUJ\r\n" +
			"モジュラF.R.L.／圧力制御機器\r\n" +
			"ロータリアクチュエータ／エアチャック\r\n" +
			"二次電池対応シリーズ 25A-シリーズ\r\n" +
			"圧縮空気清浄化機器\r\n" +
			"工業用フィルタ/焼結金属エレメント\r\n" +
			"方向制御機器\r\n" +
			"油圧機器\r\n" +
			"流体制御用機器\r\n" +
			"温調機器\r\n" +
			"潤滑機器\r\n" +
			"産業用通信機器／無線システム\r\n" +
			"真空用機器\r\n" +
			"管継手＆チューブ\r\n" +
			"薬液用バルブ/管継手＆ニードルバルブ/チューブ\r\n" +
			"計装用補助機器\r\n" +
			"銅系・フッ素系不可仕様 20-シリーズ\r\n" +
			"電動アクチュエータ／電動シリンダ\r\n" +
			"静電気対策機器／イオナイザー(除電機器)\r\n" +
			"駆動制御機器\r\n" +
			"高真空機器";
	static String jpIdList = "90405\r\n" +
			"90403\r\n" +
			"82659\r\n" +
			"89962\r\n" +
			"90404\r\n" +
			"90376\r\n" +
			"90399\r\n" +
			"82667\r\n" +
			"90385\r\n" +
			"90386\r\n" +
			"90406\r\n" +
			"90400\r\n" +
			"90393\r\n" +
			"90391\r\n" +
			"82668\r\n" +
			"90381\r\n" +
			"90377\r\n" +
			"90401\r\n" +
			"90380\r\n" +
			"90396\r\n" +
			"82662\r\n" +
			"90398\r\n" +
			"90389\r\n" +
			"90392\r\n" +
			"90382\r\n" +
			"90388\r\n" +
			"90379\r\n" +
			"90383\r\n" +
			"90390\r\n" +
			"90397\r\n" +
			"90402\r\n" +
			"90378\r\n" +
			"90387\r\n" +
			"90384\r\n" +
			"90394";

	static String enNameList ="Air Cylinders\r\n" +
		"Air Preparation Equipment\r\n" +
		"Arc Welding Process Equipment\r\n" +
		"Chemical Liquid Valves/Fittings & Needle Valves/Tubing\r\n" +
		"Clean Series/Low-Particle Generation\r\n" +
		"Copper, Fluorine-free Equipment Series 20-\r\n" +
		"Directional Control Valves\r\n" +
		"Electric Actuators/Cylinders\r\n" +
		"Fittings and Tubing\r\n" +
		"Flow Control Equipment\r\n" +
		"Fluoropolymer Equipment/PVC Equipment\r\n" +
		"High Vacuum Equipment\r\n" +
		"Hydraulic Equipment\r\n" +
		"Industrial Filters/Sintered Metal Elements\r\n" +
		"IO-Link Compatible Products\r\n" +
		"ISO Products\r\n" +
		"Lubrication Equipment\r\n" +
		"Modular F.R.L./Pressure Control Equipment\r\n" +
		"Pneumatic Instrumentation Equipment\r\n" +
		"Process Gas Equipment\r\n" +
		"Process Pumps(Diaphragm Pumps)\r\n" +
		"Process Valves\r\n" +
		"Rotary Actuators/Air Grippers\r\n" +
		"Series Compatible with Secondary Batteries Series 25A-\r\n" +
		"Silencers/Exhaust Cleaners/Blow Guns/Pressure Gauges\r\n" +
		"Static Neutralization Equipment\r\n" +
		"Switches/Sensors/Controller\r\n" +
		"Temperature Control Equipment\r\n" +
		"Vacuum Equipment (Vacuum Generators/Vacuum Suction Cups/Other)";

	static String enIdList = "90407\r\n" +
			"90618\r\n" +
			"90647\r\n" +
			"90627\r\n" +
			"90641\r\n" +
			"90645\r\n" +
			"90308\r\n" +
			"90616\r\n" +
			"90621\r\n" +
			"90622\r\n" +
			"90642\r\n" +
			"90631\r\n" +
			"90634\r\n" +
			"90632\r\n" +
			"90648\r\n" +
			"90646\r\n" +
			"90620\r\n" +
			"90619\r\n" +
			"90633\r\n" +
			"90630\r\n" +
			"90628\r\n" +
			"90626\r\n" +
			"90615\r\n" +
			"90644\r\n" +
			"90623\r\n" +
			"90625\r\n" +
			"90624\r\n" +
			"90629\r\n" +
			"90617";
	static String zhNameList ="F.R.L组件/压力控制元件\r\n" +
			"WEBカタログ-検索窓\r\n" +
			"二次电池 25A-系列\r\n" +
			"产品目录\r\n" +
			"传感器/开关\r\n" +
			"化学液用阀/管接头&针阀/管\r\n" +
			"工业用过滤器/烧结金属滤芯\r\n" +
			"工艺气体元件（特气阀）\r\n" +
			"摆动气缸/气爪\r\n" +
			"方向控制元件\r\n" +
			"气源处理元件\r\n" +
			"气缸\r\n" +
			"氟树脂元件/氯乙烯元件\r\n" +
			"洁净/低发尘系列\r\n" +
			"流体控制元件\r\n" +
			"消音器／排气洁净器／气枪/压力计\r\n" +
			"润滑元件\r\n" +
			"液压元件\r\n" +
			"温控器\r\n" +
			"电动执行器/电缸\r\n" +
			"真空用元件\r\n" +
			"禁铜、禁氟 20-系列\r\n" +
			"符合ISO规格的产品\r\n" +
			"过程控制元件\r\n" +
			"速度控制阀\r\n" +
			"配管&接头\r\n" +
			"隔膜泵\r\n" +
			"静电对策元件（静电消除器）\r\n" +
			"面向弧焊工程的元件\r\n" +
			"高真空元件";
	static String zhIdList ="92301\r\n" +
			"92264\r\n" +
			"92303\r\n" +
			"92278\r\n" +
			"92297\r\n" +
			"92313\r\n" +
			"92305\r\n" +
			"92299\r\n" +
			"92302\r\n" +
			"92306\r\n" +
			"92304\r\n" +
			"92295\r\n" +
			"92298\r\n" +
			"92320\r\n" +
			"92308\r\n" +
			"92296\r\n" +
			"92310\r\n" +
			"92307\r\n" +
			"92309\r\n" +
			"92316\r\n" +
			"92311\r\n" +
			"92315\r\n" +
			"92277\r\n" +
			"92314\r\n" +
			"92318\r\n" +
			"92312\r\n" +
			"92300\r\n" +
			"92317\r\n" +
			"92262\r\n" +
			"92319";

	private String[] jaNameArr = null;
	private String[] jaIdArr = null;
	private String[] enNameArr = null;
	private String[] enIdArr = null;
	private String[] zhNameArr = null;
	private String[] zhIdArr = null;


	public ImpCategoryTemplate(){
		jaNameArr = StringUtils.split(jpNameList,"\r\n");
		jaIdArr = StringUtils.split(jpIdList,"\r\n");
		enNameArr = StringUtils.split(enNameList,"\r\n");
		enIdArr = StringUtils.split(enIdList,"\r\n");
		zhNameArr = StringUtils.split(zhNameList,"\r\n");
		zhIdArr = StringUtils.split(zhIdList,"\r\n");
	}
	public String getIdFromName(String name, String lang) {
		String ret = "";
		String[] nameArr = null;
		String[] idArr = null;
		name = name.trim();
		if (lang.equals("ja-jp" )) {
			nameArr = jaNameArr;
			idArr = jaIdArr;
		} else if (lang.equals("en-jp" )) {
			nameArr = enNameArr;
			idArr = enIdArr;
		} else {
			nameArr = zhNameArr;
			idArr = zhIdArr;
		}
		if (nameArr != null) {
			int cnt = 0;
			for(String n : nameArr) {
				if (n.equals(name)) {
					ret = idArr[cnt];
					break;
				}
				cnt++;
			}
		}
		return ret;
	}
}
