//console
var log = function log() {};

log.LEVEL = {
	RUN: 0,
	ERROR: 1,
	WARN: 2,
	LOG: 3,
	INFO: 4,
	DEBUG: 5
};

log.level = log.LEVEL.DEBUG;

log.debug = function(msg) {
	this.level >= this.LEVEL.DEBUG &&
		this.console &&
		typeof console.log &&
		console.trace(msg); // eslint-disable-line no-undef
};
log.info = function(msg) {
	this.level >= this.LEVEL.INFO &&
		this.console &&
		typeof console.log &&
		console.info(msg); // eslint-disable-line no-undef
};
log.log = function(msg) {
	this.level >= this.LEVEL.LOG &&
		this.console &&
		typeof console.log &&
		console.log(msg); // eslint-disable-line no-undef
};
log.warn = function(msg) {
	this.level >= this.LEVEL.WARN &&
		this.console &&
		typeof console.log &&
		console.warn(msg); // eslint-disable-line no-undef
};
log.error = function(msg) {
	this.level >= this.LEVEL.ERROR &&
		this.console &&
		typeof console.log &&
		console.error(msg); // eslint-disable-line no-undef
};

$(function() {
	addErrorIcons();

	$(".ui.dropdown").dropdown({
		//clearable: true
	});
});

//.errorクラスにiconをつける
function addErrorIcons() {
	$("ul.error li").each(function() {
		$(this).prepend("<i class='exclamation circle icon'></i>");
	});
}

// idをコピー
function copyId(obj) {
	var o = new $(obj);
	log.log(o.next().html());
	o.next().select();
	log.log(document.execCommand("copy"));
}
