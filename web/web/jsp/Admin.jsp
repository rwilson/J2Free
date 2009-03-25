<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%> 
<%@taglib uri="/WEB-INF/Template" prefix="template" %>
<%@taglib uri="/WEB-INF/PackTag" prefix="pack" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/strict.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>J2Free - Administration</title>
        <pack:style>
            <src>/WEB-INF/j2free/styles/j2freeadmin.css</src>
        </pack:style>
    </head>
    <body>
        <div class="headerRight">
            <div>Redeeming Java</div>
            <span>One situp at a time</span>
        </div>
        <div class="header">J2Free - Administration</div>
        <div class="content">
            <div class="column" id="col1">
                <div class="columnHeader">Entities</div>
                <ul>
                    <c:forEach items="${availableEntities}" var="entity">
                        <li>
                            <a href="javascript:void(0);" onclick="admin.list('col1','col2','${entity.simpleName}',0,100);$$('#col1 ul li a').invoke('removeClassName','selected');this.addClassName('selected');">${entity.simpleName}</a>
                        </li>
                    </c:forEach>
                </ul>
                <div class="columnHeader">&nbsp;</div>
                <ul>
                    <li><a href="javascript:void(0);" onclick="admin.showSearch();">Search</a></li>
                </ul>
            </div>
        </div>
        <div class="footer">
            <center>&copy; 2008 J2Free.org | Version 0.1</center>
        </div>
        <div id="loader" class="loader" style="display:none;">loading...</div>
    </body>
    <pack:script>
        <src>/WEB-INF/j2free/scripts/prototype.js</src>
        <src>/WEB-INF/j2free/scripts/effects.js</src>
        <src>/WEB-INF/j2free/scripts/j2freeadmin.js/</src>
        <src>/WEB-INF/j2free/scripts/AutoGrowingTextArea.js/</src>
    </pack:script>
</html>