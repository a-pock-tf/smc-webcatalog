

function getJSONAndRendar(url, dstId){

  var options = {
        collapsed: $('#collapsed').is(':checked'),
        rootCollapsable: $('#root-collapsable').is(':checked'),
        withQuotes: $('#with-quotes').is(':checked'),
        withLinks: $('#with-links').is(':checked')
      };

    $.ajaxSetup({
      async: false,
      cache: false
    });

  $.getJSON(
    url,
    function(data) {
      $('#'+dstId).jsonViewer(data, options);
    }
  );

}
