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

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Register</title>
    </head>
    <body>
        <form name="register" action="/register" method="POST">
            Username <input type="text" name="username" maxlength="32" /><br />
            E-Mail   <input type="text" name="email" maxlength="64" /><br />
            Password <input type="text" name="password" maxlength="16" /><br />
                     <input type="submit" value="register" />
        </form>
    </body>
</html>
