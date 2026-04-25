/**UTF-8のドキュメント**/

$(function () {
    thborder();
    //mylistBts();-->global.jsのgetLoginDataでやってます
    concatCname();

    getAnchor();
    hideBorder();

    //スマホリンク表示
    //showSpLink();

    //検索結果の表示時の調整(タイトル整形など)
    afterShowResult();

    showSubCategory();

    getMyList();

    if ($("#s3s_search_form")) {
        $("#s3s_search_form").submit(function () {
            //console.log("k.val=" + $("#k").val());
            if ($("#k").val().length < 1) {
                return false;
            }
        });
    }
});

//マイリスト取得(/js/global.jsでやるのは中止)
function getMyList() {
    console.log("getMyList");

    var date = new Date();
    var timestamp = date.getTime();

    $.getJSON("/ajax/session.jsp?" + timestamp, function (json) {
        if (json.username != null) {
            //ユーザー名
            //console.log(json.username);
            console.log("hideMyListWin=" + json.hideMyListWin);

            $(".login_state").show();
            //$("#username").html(json.username);

            //マイリスト登録時にwindows表示するか
            if (json.hideMyListWin != null && json.hideMyListWin == "true") {
                //$(".bt_hide_mylistwin").show();
                prepareMyListBts(false);
            } else {
                //$(".bt_show_mylistwin").show();
                prepareMyListBts(true);
            }

            //マイリスト
            if (json.mylist_sids != null) {
                for (var i = 0; i < json.mylist_sids.length; i++) {
                    var sid = json.mylist_sids[i];
                    $("#mylist_" + sid).hide();
                    $("#end_mylist_" + sid).show();
                }
            }
        } else {
            //console.log("no-name");
            $(".logout_state").show();
        }
    });
}

function prepareMyListBts(openwin) {
    if (openwin) {
        //win表示の場合、thickboxで
        tb_init(".to_mylist");
    } else {
        //非表示の場合、add_mylist_httpをbind
        $(".to_mylist").click(function (e) {
            e.preventDefault();
            add_mylist_http(
                $(this).attr("data-sid"),
                $(this).attr("data-lang")
            );
        });
    }
}

function toggle_mylist_Bts(sid) {
    //console.log("toggle_mylist_Bts=" + sid);

    self.parent.$("#mylist_" + sid).hide();
    self.parent.$("#end_mylist_" + sid).fadeIn(1000);
}

function afterMyListWin() {
    self.parent.$(".to_mylist").each(function () {
        if ($(this).attr("id")) {
            var id = $(this)
                .attr("id")
                .replace(/mylist_/, "");
            console.log("id=" + id);
            console.log(self.parent.$("#end_mylist_" + id).html());
            if (self.parent.$("#end_mylist_" + id).is(":visible")) {
                console.log("end visible id=" + id);
                self.parent.$("#mylist_" + id).hide();
                self.parent.$("#mylist2_" + id).hide();
            }
        }
    });
}

//次回からマイリストwinを表示しない
function hide_mylist_win_http() {
    var hide = false;
    if ($("#chk1").is(":checked")) {
        hide = true;
    }

    //ボタンにしたので強制非表示
    hide = true;

    $.ajax({
        type: "GET",
        url: "/mylist/ja/add.do",
        data: "mode=HIDE_MYLIST_WIN&hide=" + hide,
        async: false,
        success: function (msg) {
            console.log("hide=" + hide);
        },
    });

    //thickboxを起動できないように
    if (hide) {
        /*
        self.parent.$(".to_mylist").unbind("click");
        self.parent.$(".to_mylist").click(function (e) {
            e.preventDefault();

            add_mylist_http(
                $(this).attr("data-sid"),
                $(this).attr("data-lang")
            );
        });
        */
    }
}

function removeThickbox() {
    $(".bt_show_mylistwin").hide();
    $(".bt_hide_mylistwin").show();

    //getLoginData(); //global.js
}

//左メニューサブカテゴリ表示
function showSubCategory() {
    $(".CUCA_IDS").each(function () {
        var id = $(this).html();
        console.log(id);
        $("#" + id).show();
    });
}

//検索結果の表示時の調整
function afterShowResult() {
    concatTitleById();
    concatTitleById3();
    concatTitleById4();

    highlightKw();

    var href = window.location.href;
    if (href.match(/\/(en|en[a-z]{2})\//)) {
        console.log("===result en");
        concatTitle(63);
        setBackUrl("en");
    } else if (href.match(/\/zh\//)) {
        console.log("===result zh");
        concatTitle(47);
        setBackUrl("zh");
    } else {
        console.log("===result ja");
        concatTitle(44);
        setBackUrl("ja");
    }
}

function submitForm(id) {
    var k = $("#" + id + " > .k").val();
    if (k != null && k != "") {
        $("#" + id).submit();
    }
}

function setType(type) {
    $(".hidden").hide();
    $(".p_" + type).show();
    //console.log(type);
}

function hideBorder() {
    $(".prod_dl_area").each(function () {
        //console.log($("a:last",$(this)).html());
        $("a:last", $(this)).css("border-right", "none");
    });
}

/*
IEにて#anchorへのリダイレクトがきかないので、、
*/
function getAnchor() {
    var sid = getUrlVars()["anc"];
    if (sid != null && sid != undefined) {
        sid = sid.replace(/#.*/, "");
        window.location.hash = sid;
    }
}

/*
非ログイン時のマイリストに入れるで tologon.do?dst=の戻りURLをセットする
*/
function setBackUrl(lang) {
    var url = location.pathname + location.search + location.hash;
    //console.log("backurl="+url);
    $(".set_back_url").attr(
        "href",
        "/customer/" + lang + "/tologin.do?dst=" + url
    );
}

function submit_prod_search_form(lang) {
    if (lang == null) lang = "en";

    var scope = $("input[name='scope']:checked").val();
    if (scope == null || scope === undefined) {
        scope = "";
    }

    var k = $("#sk").val();
    if (k != null && k != "") {
        window.location.href =
            "/products/" + lang + "/global.do?kw=" + k + "&scope=" + scope;
    }
}

/*
PsItemのキーワード検索
c1c2,seriesはクリアされます
*/

function submitPsItemKw(mode) {
    $("#c1c2").val("");
    $("#series").val("");
    $("#sk").val("");
    if (mode != null && mode != "") {
        $("#mode").val(mode);
    }
    $("#prod_search_form").submit();
}

/*
PsItemのドロップダウン検索
*/
function submitPsItemform(type) {
    if (type == "c1c2") {
        $("#series").val("");
    } else {
        $("#c1c2").val("");
    }

    $("#prod_search_form").submit();
}

//タイトル整形(文字列ベース)
function concatTitle(num) {
    $(".sename_mylist .se_name_area").each(function () {
        //console.log("sename_length=" + $(this).text().length);
        if ($(this).text().length > num) {
            //$(".txt",$(this)).css("font-size","12px");
            $(this).wrap('<p class="sename_2l"></p>');
        }
    });
}

//タイトル整形IDベース
var cids = [
    "VSS-VSR",
    "JXC73-83",
    "VQ-5",
    "VBAT-X105-E",
    "HECR-E",
    "OM-HEAT-COLD-XB-E",
    "JASV",
];
//var cids = ['JXC73-83'];
function concatTitleById() {
    for (var i = 0; i < cids.length; i++) {
        var sename = $("#sename_" + cids[i]);
        if (sename[0]) {
            console.log("sename=" + sename.attr("id"));
            sename.next("span").text("");
            sename.parent(".se_name_area").wrap('<p class="sename_2l"></p>');
            //sename.after("<br/>");
        }
    }
}

var cids3 = ["AFF-D", "AFF-D-E", "AFF30-D-E", "AFF30-D"];
function concatTitleById3() {
    for (var i = 0; i < cids3.length; i++) {
        var sename = $("#sename_" + cids3[i]);
        if (sename[0]) {
            console.log("sename=" + sename.attr("id"));
            sename.next("span").text("");
            sename.parent(".se_name_area").wrap('<p class="sename_3l"></p>');
            //sename.after("<br/>");
        }
    }
}
var cids4 = ["AFF30-D-E"];
function concatTitleById4() {
    for (var i = 0; i < cids3.length; i++) {
        var sename = $("#sename_" + cids3[i]);
        if (sename[0]) {
            console.log("sename=" + sename.attr("id"));
            sename.next("span").text("");
            sename.parent(".se_name_area").wrap('<p class="sename_4l"></p>');
            //sename.after("<br/>");
        }
    }
}

//カテゴリが長すぎたらサイズ小さく
function concatCname() {
    $(".h span").each(function () {
        console.log("seriesname length=" + $(this).text().length);

        if ($(this).text().length > 44) {
            $(this).css("font-size", "17px");
        }
    });
}

function thborder() {
    $(".resulttbl th:last-child").css("border-right-color", "#8F929F");
}

function mylistBts() {
    $(".mylist_session").each(function () {
        var id = $(this).html();
        $("#to_mylist_" + id).hide();
        $("#end_mylist_" + id).show();
    });
}

function inactiveEnter(event) {
    key = getKeyCode(event);
    if (key == 13) {
        return false;
    }
}

function add_mylist(sid) {
    /*
	$.ajax({
   		type: "GET",
   		url: "/customer/ja/mypage/mylist.do",
   		data: "mode=ADD&se_id="+se_id,
		async: false,
   		success: function(msg){
			//console.log("added:"+se_id);
    	}
 	});
	*/
    $.getJSON(
        "/customer/ja/mypage/mylist.do",

        { sid: sid, mode: "ADD_AJAX" },

        function (json) {
            if (json.result[0].overmax == "false") {
                toggle_mylist_Bts(sid);
            } else {
                alert(
                    "マイリストへの登録可能件数は" +
                        json.result[0].mylist_max +
                        "までです"
                );
            }
        }
    );
}

function add_mylist_http(sid, lang) {
    if (lang == null) {
        lang = "ja";
    }
    console.log("add:" + sid + "/lang:" + lang);

    $.getJSON(
        "/mylist/" + lang + "/add.do",

        { sid: sid, mode: "ADD_AJAX" },

        function (json) {
            //console.log(json);
            if (json.result[0].overmax == "false") {
                toggle_mylist_Bts(sid);
            } else {
                if (lang == "ja") {
                    alert(
                        "マイリストへの登録可能件数は" +
                            json.result[0].mylist_max +
                            "件までです"
                    );
                } else {
                    alert(
                        "The registerable number of items to My List is up to " +
                            json.result[0].mylist_max +
                            "."
                    );
                }
            }
        }
    ).fail(function (jqXHR, textStatus, errorThrown) {
        console.log("error " + textStatus);
        console.log("incoming Text " + jqXHR.responseText);
    });
}

/**ご意見ご要望**/
function openPosting(a) {
    var p = $("#posting");
    var atag = new $(a);

    if (!p.is(":visible")) {
        p.show();
        p.css("left", atag.offset().left - 150);
        p.css("top", atag.offset().top + 35);
    } else {
        closePosting();
    }
}

function closePosting() {
    $("#posting").hide();
}