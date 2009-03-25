<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="/WEB-INF/StandardExt" prefix="stdx" %>

<div class="columnHeader" id="edit${simpleName}${entityId}header">${simpleName} - ${entityId}</div>
<form name="entityForm">
    <input type="hidden" name="action" value="merge" />
    <table border="0" cellpadding="0" cellspacing="10" class="infoTable" id="${simpleName}${entityId}_info">
        <c:forEach items="${fields}" var="field">
            <tr>
                <td class="label">${field.name}</td>
                <td>
                    <c:choose>
                        <c:when test="${stdx:stringLength(field.value) > 30}">
                            <textarea name="${field.name}" ${field.readOnly || not empty field.entityClass ? 'class="readonly" readonly="readonly"' : ''}>${field.value}</textarea>
                        </c:when>
                        <c:otherwise>
                            <input type="text" name="${field.name}" value="${field.value}" ${field.readOnly ? 'class="readonly" readonly="readonly"' : ''}/>
                        </c:otherwise>
                    </c:choose>
                    <c:if test="${not empty field.entityClass}">
                        <a href="javascript:void(0);" onclick="admin.list('edit${simpleName}${entityId}header','col4','${field.entityClass.simpleName}',0,100,true);">Select a ${field.entityClass.simpleName}</a>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <tr>
            <td>&nbsp;</td>
            <td>
                <input type="button" value=" Save " onclick="admin.save('${simpleName}',${entityId});" />
                <input type="button" value=" Delete " onclick="admin.remove('${simpleName}',${entityId});" />
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