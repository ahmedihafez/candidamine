<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<div width="100%">
 <iframe id="frameId" src="https://candidamine.shinyapps.io/testRNASeq/" width="100%" height="1000px"></iframe>
</div>
  <script>

  
        jQuery('document').ready(function () {
        const frame = document.getElementById('frameId');
        jQuery('iframe').load(function() { 
          // write your code here....
          frame.contentWindow.postMessage($SERVICE.token, 'https://candidamine.shinyapps.io/');
        });

          
          

          })

  </script>