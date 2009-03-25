<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core"     prefix="c"%> 
<%@taglib uri="http://packtag.sf.net"                 prefix="pack" %>
<%@taglib uri="http://tags.j2free.org/StandardExt"    prefix="standard" %>
<%@taglib uri="http://tags.j2free.org/CollectionsExt" prefix="collections" %>
<%@taglib uri="http://tags.j2free.org/DateTimeExt"    prefix="datetime" %>
<%@taglib uri="http://tags.j2free.org/Template"       prefix="template" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
   
<c:set var="loginRedirect" value="${pageContext.request.header['referer']}" scope="session" />

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login</title>
    </head>
    <body>
        <form name="login" action="j_security_check" method="POST">
            Username <input name="j_username" type="text" /><br />
            Password <input name="j_password" type="text" /><br />
            <input type="submit" value="login" />
        </form>
    </body>
</html>
