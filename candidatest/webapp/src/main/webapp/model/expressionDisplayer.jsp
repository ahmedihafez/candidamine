<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- expressionDisplayer.jsp -->

<div id="cwhead">
  <h3 class="goog">Expression Profile</h3>
  <div  class="expressionViewer" id="geneExpressionDisplayer"></div>
  <script>

    var ex_paths = {
      js: {},
      css: {}
    };

    <c:set var = "section" value = "gene-expression-viewer" />
    <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
    ex_paths["${res.type}"]["${res.key}".split(".").pop()] = "/candidamine${res.url}";
    </c:forEach>




    var expload = function () {

      link = document.createElement("link");
      link.href = ex_paths.css.main;
      link.type = "text/css";
      link.rel = "stylesheet";
      link.media = "screen,print";

      document.getElementsByTagName("head")[0].appendChild(link);

      //link = document.createElement("script");
      //link.src = ex_paths.js.main;
      //link.type = "text/javascript";
      //document.getElementsByTagName("head")[0].appendChild(link);

      // using intermine load does not work ???
      intermine.load({
        'js': {
          'ex_main': {
            'path': ex_paths.js.main
          }
        }
      }, function (err) {

        var element = document.getElementById('geneExpressionDisplayer'),
                toolState = {}, //to be confirmed how we use this.
                navigate = function(type, data, mine) {
                // Helpful console message when calling navigate function.
                  var someMine = mine ? "on ".concat(mine) : null;
                  var msg = ["Navigating to", type, someMine, "with data:"]
                          .filter(function(e) { return e }) // remove falsy elements
                          .join(" ");
                  console.log(msg, data);
                };

        jQuery('document').ready(function () {
          jQuery.ajax('js/gene-expression-viewer-config.json').then(function(config) {
            expressionViewer.main(
                    element,
                    $SERVICE,
                    {
                      class: imSummaryFields.type,
                      format: 'id',
                      value: ${experssionObjectId}
                    },
                    toolState,
                    config,
                    navigate
                    );
          });
        });
      });
    };

    expload()
  </script>
</div>

<!-- /expressionDisplayer.jsp -->
