<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://tags.j2free.org/CollectionsExt" prefix="cx" %>

<c:if test="${start == 0}">
    <div class="columnHeaderRight"><input type="button" value="New" onclick="admin.create('col2','col3','${simpleName}');" /></div>
    <div class="columnHeader">${simpleName} Instances</div>
</c:if>
<ul>
    <c:forEach items="${entities}" var="entity">
        <li><a href="javascript:void(0);" onclick="admin.find('col2','col3','${simpleName}',${entity.key});$$('#col2 ul li a').invoke('removeClassName','selected');this.addClassName('selected');admin.starveColumn('col1');">${entity.value}</a></li>
    </c:forEach>
</ul>
<c:if test="${start + limit < total}">
    <div id="more${simpleName}Loader" class="moreLoader">
        <c:choose>
            <c:when test="${limit > (total - (start + limit))}">
                <c:set var="linkText" value="last ${total - (start + limit)}" />
            </c:when>
            <c:otherwise>
                <c:set var="linkText" value="next ${limit} (${total - (start + limit)} more)" />
            </c:otherwise>
        </c:choose>
        <a href="javascript:void(0);" onclick="admin.listappend('col2','${simpleName}',${start + limit},${limit});Element.remove('more${simpleName}Loader');">${linkText}</a>
    </div>
</c:if>