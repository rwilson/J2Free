<%@tag description="J2Free Auto Paginator" pageEncoding="UTF-8"%>

<%-- Taglib directives can be specified here: --%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://tags.j2free.org/StandardExt" prefix="stdx" %>

<%-- The list of normal or fragment attributes can be specified here: --%>
<%@attribute name="numItems" required="true" rtexprvalue="true"%>
<%@attribute name="startItemNumber" required="true" rtexprvalue="true"%>
<%@attribute name="itemsPerPage" required="true" rtexprvalue="true"%>

<%@variable name-given="pageNumber" %>
<%@variable name-given="pageStartItemNumber" %>
<%@variable name-given="isSelectedPage" %>

<c:set var="currentPage" value="${1 + (startItemNumber / itemsPerPage)}" />
<c:set var="lastPage" value="${(numItems % itemsPerPage) == 0 ? (numItems / itemsPerPage) : stdx:integerDivision(numItems - (numItems % itemsPerPage) + itemsPerPage,itemsPerPage)}" />
<%--
<c:if test="${startItemNumber >= itemsPerPage}">
    <c:set var="pageNumber" value="First" />
    <c:set var="pageStartItemNumber" value="0" />
    <c:set var="isSelectedPage" value="false" />
    <jsp:doBody/>
</c:if>
--%>
<c:if test="${startItemNumber >= 3 * itemsPerPage}">
    <c:set var="pageNumber" value="1" />
    <c:set var="pageStartItemNumber" value="0" />
    <c:set var="isSelectedPage" value="false" />
    <jsp:doBody/>
    <c:if test="${startItemNumber >= 4 * itemsPerPage}">
        ...
    </c:if>
</c:if>
<c:set var="beginI" value="${currentPage - 2}" />
<c:if test="${beginI < 1}">
    <c:set var="beginI" value="${1}" />
</c:if>
<c:set var="endI" value="${beginI + 4}" />
<c:if test="${endI > lastPage}">
    <c:set var="endI" value="${lastPage}" />
</c:if>
<c:if test="${endI - beginI < 4 && lastPage > 5}">
    <c:set var="beginI" value="${lastPage - 4}" />
</c:if>
<c:forEach var="i" begin="${beginI}" step="1" end="${endI}" varStatus="status">
    <c:set var="pageNumber" value="${i}" />
    <c:set var="pageStartItemNumber" value="${(i-1)*itemsPerPage}" />
    <c:set var="isSelectedPage" value="${((startItemNumber == ((i - 1) * itemsPerPage)) || (empty startItemNumber && i == 1)) ? 'true' : 'false'}" />
    <jsp:doBody/>
</c:forEach>
<c:if test="${startItemNumber <= numItems - 3 * itemsPerPage}">
    <c:if test="${startItemNumber <= numItems - 4 * itemsPerPage}">
        ...
    </c:if>
    <c:set var="pageNumber" value="${lastPage}" />
    <c:set var="pageStartItemNumber" value="${(numItems % itemsPerPage) == 0 ? numItems - itemsPerPage : numItems - (numItems % itemsPerPage)}" />
    <c:set var="isSelectedPage" value="false" />
    <jsp:doBody/>
</c:if>
&nbsp;
<c:if test="${startItemNumber >= itemsPerPage}">
    <c:set var="pageNumber" value="&#9668;" />
    <c:set var="pageStartItemNumber" value="${startItemNumber - itemsPerPage}" />
    <c:set var="isSelectedPage" value="false" />
    <jsp:doBody/>
</c:if>
<c:if test="${startItemNumber <= numItems - itemsPerPage}">
    <c:set var="pageNumber" value="&#9658;" />
    <c:set var="pageStartItemNumber" value="${startItemNumber + itemsPerPage}" />
    <c:set var="isSelectedPage" value="false" />
    <jsp:doBody/>
</c:if>
<%--
<c:if test="${startItemNumber + itemsPerPage <= numItems}">
    <c:set var="pageNumber" value="Last" />
    <c:set var="pageStartItemNumber" value="${(numItems % itemsPerPage) == 0 ? numItems - itemsPerPage : numItems - (numItems % itemsPerPage)}" />
    <c:set var="isSelectedPage" value="false" />
    <jsp:doBody/>
</c:if>
--%>