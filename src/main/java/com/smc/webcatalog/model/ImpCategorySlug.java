package com.smc.webcatalog.model;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import lombok.Getter;

public class ImpCategorySlug {

	@Getter
	public class CategorySlug{
		String name;
		String slug;
		boolean large;
	}
	private List<CategorySlug> list;

	// Excelをコピーして""の中に貼り付け。自動でタブと改行になる。
	// 最終更新：2021/8/24(Slug_0726_1.xlsx)
	static String slug = "方向制御機器		directional-control-valves	\r\n" +
			"	パイロット式4·5ポートソレノイドバルブ		pilot-operated-4-5-port-solenoid-valves\r\n" +
			"	パイロット式3ポートソレノイドバルブ（電磁弁）		pilot-operated-3-port-solenoid-valves\r\n" +
			"	直動式3·4·5ポートソレノイドバルブ（電磁弁）		direct-operated-3-4-5-port-solenoid-valves\r\n" +
			"	省配線フィールドバスシステム(シリアル伝送システム)	 	reduced-wiring-fieldbus-system\r\n" +
			"	エアオペレートバルブ		air-operated-valves\r\n" +
			"	メカニカルバルブ/中継機器	 	mechanical-valves-transmitters\r\n" +
			"	フィンガバルブ/ハンドバルブ/残圧抜き3ポート弁		finger-valve-hand-valves-residual-relief-3-port-valve\r\n" +
			"	ブローガン		blow-guns\r\n" +
			"	パワーバルブ		power-valves\r\n" +
			"	ISOバルブ		iso-valves\r\n" +
			"	防爆バルブ		explosion-proof-valves\r\n" +
			"エアシリンダ		air-cylinders	\r\n" +
			"	標準形エアシリンダ（丸形）		air-cylinders-round-type\r\n" +
			"	標準形エアシリンダ（角形カバー）		air-cylinders-square-cover\r\n" +
			"	コンパクトエアシリンダ		compact-air-cylinders\r\n" +
			"	耐環境仕様シリンダ		environment-resistant-cylinders\r\n" +
			"	フローティングジョイント／ロッドエンド		floating-joints-rod-end\r\n" +
			"	エアハイドロユニット		air-hydro-units\r\n" +
			"	メカジョイント式ロッドレスシリンダ		mechanically-jointed-rodless-cylinders\r\n" +
			"	マグネット式ロッドレスシリンダ		magnetically-coupled-rodless-cylinders\r\n" +
			"	テーブル付シリンダ		table-cylinders\r\n" +
			"	ガイド付シリンダ（MGシリーズ）		guide-cylinders-MG\r\n" +
			"	ガイド付シリンダ（CXシリーズ）		guide-cylinders-CX\r\n" +
			"	ファインロックシリンダ		fine-lock-cylinders\r\n" +
			"	ロック付シリンダ／エンドロック付シリンダ			lock-cylinders\r\n" +
			"	特殊シリンダ		specialty-cylinders\r\n" +
			"	クランプシリンダ		clamp-cylinders\r\n" +
			"	ピンシフトシリンダ		pin-shift-cylinder\r\n" +
			"	ストッパシリンダ		stopper-cylinders\r\n" +
			"	計測シリンダ		stroke-reading-cylinders\r\n" +
			"	バルブ付エアシリンダ		valve-mounted-air-cylinders\r\n" +
			"	ISOシリンダ		iso-cylinders\r\n" +
			"	ショックアブソーバ		shock-absorbers\r\n" +
			"	オートスイッチ		auto-switches\r\n" +
			"	アクチュエータ位置センサ		actuator-position-sensor\r\n" +
			"	簡易特注システム		simple-specials\r\n" +
			"	オーダーメイド仕様		made-to-order\r\n" +
			"ロータリアクチュエータ／エアチャック		rotary-actuators-air-grippers	\r\n" +
			"	ロータリアクチュエータ ベーンタイプ		rotary-actuators-vane-type\r\n" +
			"	ロータリアクチュエータ ラックピニオンタイプ		rotary-actuators-rack-pinion-type\r\n" +
			"	エアチャック（平行開閉形）		parallel-type-air-grippers\r\n" +
			"	エアチャック（支点開閉形）		angular-type-air-grippers\r\n" +
			"	協働ロボット用エアグリッパ		air-gripper-unit-for-collaborative-robots\r\n" +
			"	オートスイッチ		auto-switches\r\n" +
			"電動アクチュエータ／電動シリンダ		electric-actuators-cylinders	\r\n" +
			"	カードモータ		card-motors\r\n" +
			"	バッテリレス アブソリュートエンコーダタイプ		battery-less-absolute-encoder-type\r\n" +
			"	スライダタイプ		slider-type\r\n" +
			"	ロッドタイプ／ガイド付ロッドタイプ		rod-type-guide-rod-type\r\n" +
			"	スライドテーブル		electric-slide-tables\r\n" +
			"	ミニチュア		miniature\r\n" +
			"	ロータリテーブル		electric-rotary-tables\r\n" +
			"	グリッパ		electric-grippers\r\n" +
			"	コントローラ／ドライバ		controllers-drivers\r\n" +
			"	環境		environment\r\n" +
			"	モータレス仕様		motorless-type\r\n" +
			"	電動アクチュエータ・シリンダ		electric-cylinders\r\n" +
			"	オーダーメイド		made-to-order\r\n" +
			"真空用機器		vacuum-equipment	\r\n" +
			"	真空エジェクタ・真空ポンプシステム		vacuum-ejectors-vacuum-generators\r\n" +
			"	真空エジェクタ		vacuum-ejectors\r\n" +
			"	真空破壊弁ユニット		vacuum-releaser-valve-units\r\n" +
			"	エアサクションフィルタ		air-suction-filters\r\n" +
			"	真空パッド		vacuum-pads\r\n" +
			"	特殊パッド		special-pads\r\n" +
			"	協働ロボット用グリッパユニット		gripper-unit-for-collaborative-robots\r\n" +
			"	サクションアシストバルブ		vacuum-saving-valves\r\n" +
			"	バキュームフロー		vacuum-flow\r\n" +
			"	真空レギュレータ		vacuum-regulators\r\n" +
			"	真空システム用関連機器		related-equipment-for-vacuum-systems\r\n" +
			"圧縮空気清浄化機器		air-preparation-equipment	\r\n" +
			"	アフタクーラ/エアタンク		aftercoolers-air-tanks\r\n" +
			"	エアドライヤ		air-dryers\r\n" +
			"	圧縮空気清浄化フィルタ		air-preparation-filters\r\n" +
			"	オートドレン／差圧計		auto-drains-differential-pressure-gauges\r\n" +
			"モジュラF.R.L.／圧力制御機器		modular-frl-units-pressure-control-equipment	\r\n" +
			"	モジュラF.R.L.		modular-frl-units\r\n" +
			"	レギュレータ		regulators\r\n" +
			"	電空レギュレータ		electro-pneumatic-regulators\r\n" +
			"	比例弁		proportional-valves\r\n" +
			"	増圧弁		booster-regulators\r\n" +
			"潤滑機器		lubrication-equipment	\r\n" +
			"	潤滑機器		lubrication-equipment\r\n" +
			"管継手＆チューブ		fittings-and-tubing	\r\n" +
			"	汎用管継手		fittings-for-general-purposes\r\n" +
			"	汎用管継手（Sカプラー／マルチコネクタ）		s-couplers-multi-connectors\r\n" +
			"	特殊環境用管継手		fittings-for-special-environments\r\n" +
			"	特殊環境用管継手 (クリーン／フッ素樹脂)		clean-fluoropolymer\r\n" +
			"	チューブ		tubing\r\n" +
			"	チューブ（特殊環境用チューブ）		tubing-for-special-environments\r\n" +
			"	チューブ (フッ素樹脂／クリーン)		fluoropolymer-tubing\r\n" +
			"	付属関連機器		related-products\r\n" +
			"駆動制御機器　スピードコントローラ		flow-control-equipment-speed-controllers	\r\n" +
			"	汎用スピードコントローラ		general-purposes\r\n" +
			"	低速制御用スピードコントローラ		low-speed-control\r\n" +
			"	特殊環境用スピードコントローラ		special-environments\r\n" +
			"	特殊機能スピードコントローラ		with-special-functions\r\n" +
			"	工具操作形スピードコントローラ		tool-operation-type\r\n" +
			"	省エアスピードコントローラ／エアセービングバルブ		air-saving-speed-controllers-air-saving-valves\r\n" +
			"	駆動制御関連機器		related-equipment\r\n" +
			"サイレンサ／エキゾーストクリーナ／ブローガン／圧力計		silencers-exhaust-cleaners-blow-guns-pressure-gauges	\r\n" +
			"	サイレンサ		silencers\r\n" +
			"	エキゾーストクリーナ		exhaust-cleaners\r\n" +
			"	ブローガン		blow-guns\r\n" +
			"	圧力計		pressure-gauges\r\n" +
			"スイッチ／センサ／コントローラ		switches-sensors-controllers	\r\n" +
			"	電子式圧力スイッチ／センサ（センサ・アンプ一体型）		electronic-pressure-switches-sensors-self-contained-type\r\n" +
			"	電子式圧力スイッチ／センサ（センサ・アンプ分離型）		electronic-pressure-switches-sensors-remote-type\r\n" +
			"	機械式圧力スイッチ		mechanical-pressure-switches\r\n" +
			"	電子式フロースイッチ／センサ		electronic-flow-switches-sensors\r\n" +
			"	機械式フロースイッチ		mechanical-flow-switches\r\n" +
			"	コントローラ		flow-controllers\r\n" +
			"静電気対策機器／イオナイザー(除電機器)		static-neutralization-equipment-ionizers	\r\n" +
			"	静電気対策機器/イオナイザー（除電機器）		ionizers\r\n" +
			"産業用通信機器／無線システム		industrial-communication	\r\n" +
			"	無線システム　PROFINET 対応機器		wireless-system-profinet-compatible\r\n" +
			"	無線システム　EtherNet/IP 対応機器		wireless-system-ethernet-ip-compatible\r\n" +
			"	無線システム　IO-Link 対応機器		wireless-system-io-link-compatible\r\n" +
			"	産業用通信機器　PROFINET 対応機器		industrial-communication-profinet-compatible\r\n" +
			"	産業用通信機器　EtherNet/IP 対応機器		industrial-communication-ethernet-ip-compatible\r\n" +
			"	産業用通信機器　EtherCAT 対応機器		industrial-communication-ethercat-compatible\r\n" +
			"	産業用通信機器　Modbus TCP 対応機器		industrial-communication-modbus-tcp-compatible\r\n" +
			"	産業用通信機器　ETHERNET POWERLINK 対応機器		industrial-communication-ethernet-powerink\r\n" +
			"	産業用通信機器　CC-Link IE Field 対応機器		industrial-communication-cc-link-ie-compatible\r\n" +
			"	産業用通信機器　SSCNET III 対応機器		industrial-communication-sscnet3-compatible\r\n" +
			"	産業用通信機器　MECHATROLINK-Ⅲ 対応機器		industrial-communication-mechatrolink-3-compatible\r\n" +
			"	産業用通信機器　PROFIsafe 対応機器		industrial-communication-profisafe-compatible\r\n" +
			"	産業用通信機器　IO-Link 対応機器		industrial-communication-io-link-compatible\r\n" +
			"	産業用通信機器　PROFIBUS 対応機器		industrial-communication-profibus-compatible\r\n" +
			"	産業用通信機器　DeviceNet 対応機器		industrial-communication-devicenet-compatible\r\n" +
			"	産業用通信機器　CC-Link 対応機器		industrial-communication-cc-link-compatible\r\n" +
			"	産業用通信機器　AS-interface 対応機器		industrial-communication-as-interface-compatible\r\n" +
			"	産業用通信機器　CANopen 対応機器		industrial-communication-canopen-compatible\r\n" +
			"	産業用通信機器　CompoNet 対応機器		industrial-communication-componet-compatible\r\n" +
			"	産業用通信機器　Interbus 対応機器		industrial-communication-interbus-compatible\r\n" +
			"	産業用通信機器　MECHATROLINK-Ⅱ 対応機器		industrial-communication-mechatrolink-2-compatible\r\n" +
			"	その他　RS232C 対応機器		industrial-communication-rs232c-compatible\r\n" +
			"	その他　RS485 対応機器		industrial-communication-rs485-compatible\r\n" +
			"流体制御用機器		process-valves	\r\n" +
			"	2・3ポートソレノイドバルブ／エアオペレートバルブ		2-3-port-solenoid-valves-air-operated-valves\r\n" +
			"	2･3ポートソレノイドバルブ		compact-2-3-port-solenoid-valves\r\n" +
			"	5.0MPa対応2･3ポートソレノイドバルブ		5.0-mpa-2-3-port-solenoid-valves\r\n" +
			"	汎用流体制御用2・3ポートバルブ		2-3-port-valves-for-general-purpose-fluid-control\r\n" +
			"	クーラント用バルブ		coolant-valves\r\n" +
			"	水性・溶剤系流体用バルブ		valves-for-water-and-chemical-base-fluids\r\n" +
			"薬液用バルブ/管継手＆ニードルバルブ/チューブ		chemical-liquid-valves-fittings-needle-valves-tubing	\r\n" +
			"	薬液用バルブ		chemical-liquid-valves\r\n" +
			"	管継手／ニードルバルブ		fittings-needle-valves\r\n" +
			"	チューブ		fluoropolymer-tubing\r\n" +
			"プロセスポンプ（ダイヤフラム式ポンプ）		process-pumps-diaphragm-pumps	\r\n" +
			"	プロセスポンプ		process-pumps-diaphragm-pumps\r\n" +
			"温調機器		temperature-control-equipment	\r\n" +
			"	サーモチラー（循環液温調装置）		thermo-chillers\r\n" +
			"	サーモコン／サーモ恒温槽（ペルチェ式温調装置）		thermo-cons-thermoelectric-baths\r\n" +
			"	空気温調機		air-temperature-controllers\r\n" +
			"プロセスガス用機器		process-gas-equipment	\r\n" +
			"	プロセスガス用機器		process-gas-equipment\r\n" +
			"高真空機器		high-vacuum-equipment	\r\n" +
			"	高真空バルブ		high-vacuum-valves\r\n" +
			"	その他		other\r\n" +
			"	スリットバルブ		slit-valves\r\n" +
			"	真空ロッドレスシリンダ		rodless-cylinders-for-vacuum\r\n" +
			"工業用フィルタ/焼結金属エレメント		industrial-filters-sintered-metal-elements	\r\n" +
			"	工業用フィルタ		industrial-filters\r\n" +
			"	焼結金属エレメント		sintered-metal-elements\r\n" +
			"計装用補助機器		pneumatic-instrumentation-equipment	\r\n" +
			"	ポジショナ		positioners\r\n" +
			"	減圧弁		regulators\r\n" +
			"	リレー／バルブ		relays-valves\r\n" +
			"	電ー空変換器		electro-pneumatic-transducers\r\n" +
			"	アクチュエータ		actuators\r\n" +
			"	検出変換器		detection-conversion-units\r\n" +
			"	ソレノイドバルブ		solenoid-valves\r\n" +
			"	配管材		piping-materials\r\n" +
			"油圧機器		hydraulic-equipment	\r\n" +
			"	油圧シリンダ		hydraulic-cylinders\r\n" +
			"	油圧用機器		hydraulic-equipment\r\n" +
			"クリーン／低発塵		clean-series-low-particle-generation	\r\n" +
			"	方向制御機器		directional-control-valves\r\n" +
			"	エアシリンダ		air-cylinders\r\n" +
			"	ロータリアクチュエータ		rotary-actuators\r\n" +
			"	エアチャック		air-grippers\r\n" +
			"	エアドライヤ		air-dryers\r\n" +
			"	圧縮空気清浄化フィルタシリーズ		compressed-air-cleaning-filter-Series\r\n" +
			"	クリーンガスフィルタ・エアフィルタ		clean-gas-filters-air-filters\r\n" +
			"	クリーンエキゾーストクリーナ・フィルタ		exhaust-cleaner-for-clean-room-clean-exhaust-filter\r\n" +
			"	モジュラF.R./圧力制御機器		modular-frl-units-pressure-control-equipment\r\n" +
			"	管継手		fittings\r\n" +
			"	チューブ		tubing\r\n" +
			"	駆動制御機器		flow-control-equipment-speed-controllers\r\n" +
			"	圧力スイッチ/圧力センサ		switches-sensors\r\n" +
			"	フロースイッチ		flow-switches\r\n" +
			"	電動アクチュエータ		electric-actuators\r\n" +
			"フッ素樹脂製機器/塩化ビニル製機器		fluoropolymer-equipment-pvc-equipment	\r\n" +
			"	薬液用バルブ		high-purity-chemical-liquid-valves\r\n" +
			"	管継手＆ニードルバルブ／チューブ		fittings-needle-valve-tubing\r\n" +
			"	フッ素樹脂製レギュレータ		fluororesin-regulators\r\n" +
			"	フッ素樹脂製フロースイッチ		fluoropolymer-flow-switches\r\n" +
			"	フッ素樹脂製プロセスポンプ		fluoropolymer-process-pumps\r\n" +
			"	塩化ビニル製バルブ		pvc-valves\r\n" +
			"	塩化ビニル製フロースイッチ		pvc-flow-switches\r\n" +
			"二次電池対応シリーズ 25A-シリーズ		series-compatible-with-secondary-batteries-series-25a	\r\n" +
			"	方向制御機器		directional-control-valves\r\n" +
			"	アクチュエータ		actuators\r\n" +
			"	付属関連機器		related-products\r\n" +
			"	ロータリアクチュエータ/エアチャック		rotary-actuators-air-grippers\r\n" +
			"	真空機器		vacuum-equipment\r\n" +
			"	圧縮空気清浄化機器/クリーンエアフィルタ		air-preparation-equipment-clean-air-filters\r\n" +
			"	モジュラF. R. L./圧力制御機器		modular-frl-units-pressure-control-equipment\r\n" +
			"	駆動制御機器/管継手		flow-control-equipment-fittings\r\n" +
			"	チューブ		tubings\r\n" +
			"	検出スイッチ/オートスイッチ		detection-switches-auto-switches\r\n" +
			"	流体制御機器		process-valves\r\n" +
			"	電動アクチュエータ		electric-actuators\r\n" +
			"銅系・フッ素系不可仕様 20-シリーズ		copper-fluorine-free-equipment-series-20	\r\n" +
			"	銅系・フッ素系不可 20-		copper-fluorine-free-20\r\n" +
			"協働ロボット	 	gripper-for-collaborative-robots	\r\n" +
			"	ファナック株式会社向け		fanuc\r\n" +
			"	三菱電機株式会社向け		mitsubishi-electric\r\n" +
			"	オムロン株式会社TECHMAN ROBOT Inc.向け		omron-and-techman-robot\r\n" +
			"	UNIVERSAL ROBOTS向け		universal-robots\r\n" +
			"	株式会社安川電機向け		yaskawa\r\n" +
			"ISO規格準拠製品		iso-products	\r\n" +
			"	ソレノイドバルブ		solenoid-vales\r\n" +
			"	エアシリンダ		cylinders\r\n" +
			"アーク溶接工程向機器		arc-welding-process-equipment	\r\n" +
			"	アーク溶接用耐スパッタシリンダ		spatter-resistant-cylinders-for-arc-welding\r\n" +
			"	特殊シリンダ		specialty-cylinders\r\n" +
			"	ガス／エア切換弁		gas-air-switching-valve\r\n" +
			"	検出スイッチ		detection-switches\r\n" +
			"	チューブ／管継手／駆動制御機器		tubing-fittings-flow-control-equipment\r\n" +
			"	サーモチラー設置のご提案		proposal-for-installation-of-thermo-chiller\r\n" +
			"IO-Link対応機器		io-link-compatible-products	\r\n" +
			"	圧力スイッチ		pressure-switches\r\n" +
			"	デジタルフロースイッチ		digital-flow-switches\r\n" +
			"	位置センサ		position-sensors\r\n" +
			"	SIユニット		si-unit\r\n" +
			"	電空レギュレータ		electro-pneumatic-regulators\r\n" +
			"	ステップモータコントローラ		step-motor-controllers\r\n" +
			"	IO-Linkマスタ		io-link-master\r\n" +
			"ノングリース 窒素（N<sub>2</sub>）対応機器		nitrogen-n2-ompatible-equipment	\r\n" +
			"	圧縮空気清浄化フィルタ		air-preparation-filters\r\n" +
			"	圧力制御機器		pressure-control-equipment\r\n" +
			"	圧力計		pressure-gauges\r\n" +
			"	管継手		fittings\r\n" +
			"	チューブ		tubings\r\n" +
			"	絞り弁		throttle-valves\r\n" +
			"	圧力スイッチ		pressure-switches\r\n" +
			"	フロースイッチ		flow-switches-2-3-port-solenoid-valves\r\n" +
			"	流体制御用2・3ポート ソレノイドバルブ		process-valves\r\n" +
			"	薬液用バルブ		chemical-liquid-valves\r\n" +
			"	高真空バルブ		high-vacuum-valves\r\n" +
			"	プロセスガス用機器		process-gas-equipment\r\n" +
			"	焼結金属エレメント		sintered-metal-elements\r\n" +
			"	関連機器		related-equipment\r\n" +
			"";

	public ImpCategorySlug(){
		list = new LinkedList<ImpCategorySlug.CategorySlug>();
		String[] arr = StringUtils.split(slug,"\r\n");
		for(String str : arr) {
			String[] tmp = StringUtils.splitPreserveAllTokens(str,'\t');
			if (tmp.length >= 4) {
				CategorySlug s = new CategorySlug();
				if (tmp[0].isEmpty()) {
					s.name = tmp[1];
					s.slug = tmp[3];
					s.large = false;
				} else {
					s.name = tmp[0];
					s.slug = tmp[2];
					s.large = true;
				}
				list.add(s);
			}
		}
	}
	public String getSlug(String name) {
		String ret = null;
		if (list != null) {
			for(CategorySlug c : list) {
				if (c.name.equals(name)) {
					ret = c.slug;
					break;
				}
			}
		}
		return ret;
	}
	// for TEST
	public List<CategorySlug> get() {
		return list;
	}
}
