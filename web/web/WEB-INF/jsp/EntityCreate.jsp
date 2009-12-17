<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://tags.j2free.org/StandardExt" prefix="stdx" %>

<div class="columnHeader">Create a new ${simpleName}</div>
<form name="entityForm">
    <input type="hidden" name="action" value="persist" />
    <table border="0" cellpadding="0" cellspacing="10" class="infoTable" id="${simpleName}${entityId}_info">
        <c:forEach items="${fields}" var="field">
            <tr>
                <td>${field.name}</td>
                <td>
                    <c:choose>
                        <c:when test="${field.entity}">
                            <a href="javascript:void(0);" onclick="admin.list('col3','col4','${field.type}',0,100,true);">Choose a ${field.type}</a>
                        </c:when>
                        <c:when test="${field.readOnly}"> 
                            <span style="color:#777">[Generated Value]</span>
                        </c:when>
                        <c:when test="${stdx:stringLength(field.value) > 50}">
                            <textarea name="${field.name}">${field.value}</textarea>
                        </c:when>
                        <c:otherwise>
                            <input type="text" name="${field.name}" value="${field.value}" />
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <tr>
            <td>&nbsp;</td>
            <td>
                <input type="button" value=" Save " onclick="admin.save('${simpleName}',${entityId});" />
            </td>
        </tr>
    </table>
</form>
<script type="text/javascript">
    var areas = $$('#${simpleName}${entityId}_info textarea');
    for (var i = 0; i < areas.length; i++) {
        new AutoGrowingTextArea(areas[i]);
    }
</script>