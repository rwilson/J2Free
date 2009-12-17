<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://tags.j2free.org/CollectionsExt" prefix="cx" %>
<%@taglib uri="http://tags.j2free.org/StandardExt" prefix="stdx" %>

<c:if test="${start == 0}">
    <div class="columnHeader">Select a${stdx:startsWithVowel(simpleName) ? 'n' : ''} ${simpleName}</div>
</c:if>
<ul>
    <c:forEach items="${entities}" var="entity">
        <li><a href="javascript:void(0);" onclick="admin.find('col4','col5','${simpleName}',${entity.key});$$('#col4 ul li a').invoke('removeClassName','selected');this.addClassName('selected');">${entity.value}</a></li>
    </c:forEach>
</ul>
<c:if test="${start + limit < total}">
    <div id="more${simpleName}SelectorLoader" class="moreLoader">
        <c:choose>
            <c:when test="${limit > (total - (start + limit))}">
                <c:set var="linkText" value="last ${total - (start + limit)}" />
            </c:when>
            <c:otherwise>
                <c:set var="linkText" value="next ${limit} (${total - (start + limit)} more)" />
            </c:otherwise>
        </c:choose>
        <a href="javascript:void(0);" onclick="admin.listappend('col4','${simpleName}',${start + limit},${limit});Element.remove('more${simpleName}SelectorLoader');">${linkText}</a>
    </div>
</c:if>