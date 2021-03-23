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

    var exp_paths = {
      js: {},
      css: {}
    };

    <c:set var = "section" value = "gene-expression-viewer" />
    <c:forEach var="res" items="${imf:getHeadResources(section, PROFILE.preferences)}">
    exp_paths["${res.type}"]["${res.key}".split(".").pop()] = "/candidamine${res.url}";
    </c:forEach>




    var imload = function () {

      link = document.createElement("link");
      link.href = exp_paths.css.main;
      link.type = "text/css";
      link.rel = "stylesheet";
      link.media = "screen,print";

      document.getElementsByTagName("head")[0].appendChild(link);

      intermine.load({
        'js': {
          'exp_main': {
            'path': exp_paths.js.main
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

    imload()
  </script>
</div>

<!-- /expressionDisplayer.jsp -->
