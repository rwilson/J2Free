function setCookie(name,value,expire,path) {
    document.cookie = name + "=" + value + ";expires=" + expire.toGMTString() + ";path=" + path;
}

function readCookie(cookieName) {
    var theCookie = "" + document.cookie;
    var start = theCookie.indexOf(cookieName);
    
    if (start == -1 || cookieName == "") 
        return ""; 
        
    var end = theCookie.indexOf(';',start);
    
    if (end == -1) 
        end = theCookie.length; 
        
    return unescape(theCookie.substring(start + cookieName.length + 1,end));
}