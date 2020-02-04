<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn"  uri="http://java.sun.com/jsp/jstl/functions" %>
<!-- footer.jsp -->

<br/>


<div class="body innerfooter" align="center" style="clear:both" >
    <!-- contact -->
    <c:if test="${pageName != 'contact'}">
        <div id="contactFormDivButton">
            <im:vspacer height="11" />
            <div class="contactButton">
                <a href="#" onclick="showContactForm();return false">
                    <b><fmt:message key="feedback.title"/></b>
                </a>
            </div>
        </div>
        <div id="contactFormDiv" style="display:none;">
            <im:vspacer height="11" />
            <tiles:get name="contactForm" />
        </div>
    </c:if>
    <br/>

<div id="promo-footer">
  <!-- powered -->
    <div class="funding-footer footer">
      <!-- funding -->
      <div class="funders"><fmt:message key="funding" /></div>
      <div class="credits"><fmt:message key="credits" /></div>
    </div>

   
    <div class="cite-footer footer">
      <strong>Cite us:</strong>
            <cite><fmt:message key="citation" /></cite>
    </div>
</div>


</div>

<div class="funding-footer footer">
  <!-- funding -->
  <fmt:message key="funding" />
  <c:set var="credits"><fmt:message key="credits" /></c:set>

  <c:if test="${!fn:startsWith(credits,'???')}">
       ${credits}
    </c:if>
</div>


<!-- cam logo and links -->
<div class="body bottom-footer">
<div class="powered-footer footer">
    <p>Powered by</p>
    <a target="new" href="http://intermine.org" title="InterMine">
      <img src="images/icons/intermine-footer-logo.png" alt="InterMine logo" />
    </a>
  </div>
<div id="copyright">
    <a class="cambridge-logo" href="http://www.cam.ac.uk/" title="University of Cambridge" target="_blank">
        <img src="images/icons/cambridge-footer-logo.png" alt="University of Cambridge logo">
    </a>
    <p class="footer-copy">&copy; 2002 - 2019 Department of Genetics, University of Cambridge, Downing Street,<br />
        Cambridge CB2 3EH, United Kingdom</p>
</div>
<!--     <ul class="footer-links">
        <li><a href="#" onclick="showContactForm();return false;">Contact Us</a></li>
        <c:set value="${WEB_PROPERTIES['header.links']}" var="headerLinks"/>
        <c:forEach var="entry" items="${headerLinks}" varStatus="status">
            <c:set value="header.links.${entry}" var="linkProp"/>
            <c:choose>
                <c:when test="${!empty WEB_PROPERTIES[linkProp]}">
                    <li><a href="${WEB_PROPERTIES[linkProp]}">${entry}</a></li>
                </c:when>
                <c:otherwise>
                    <li><a href="${WEB_PROPERTIES['project.sitePrefix']}/${entry}.shtml">${entry}</a></li>
                </c:otherwise>
            </c:choose>
        </c:forEach>

    </ul> 
-->

    <!-- mines -->
    <div id="othermines">
    <ul class="footer-links">
        <li><a href="http://www.intermine.org" target="_blank">InterMine</a></li>
        <li><a href="http://www.flymine.org" target="_blank">FlyMine</a></li>
        <li><a href="http://www.mousemine.org" target="_blank">MouseMine</a></li>
        <li><a href="http://ratmine.mcw.edu/ratmine" target="_blank">RatMine</a></li>
        <li><a href="http://www.wormbase.org/tools/wormmine" target="_blank">WormMine</a></li>
        <li><a href="http://yeastmine.yeastgenome.org" target="_blank">YeastMine</a></li>
        <li><a href="http://www.zebrafishmine.org" target="_blank">ZebrafishMine</a></li>
        <li><a href="http://www.humanmine.org" target="_blank">HumanMine</a></li>
    </ul>
    </div>
    

    <div style="clear:both"></div>
</div>
<!-- /footer.jsp -->
