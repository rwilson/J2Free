function defineTabGroup(container,rCl) {
    if (typeof container == "string")
        container = $(container);

    var children = Element.childElements(container);
    
    for (var i = 0; i < children.length; i++) {
        var child = children[i];
        child.tabContainer = container;
        child.rCl = rCl;

        if (child.className.indexOf(rCl) != -1) {
            child.onmouseover = function() {
                Element.removeClassName(this,rCl);
            }
            child.onmouseout = function() {
                Element.addClassName(this,rCl);
            }
            child.onclick = function() {
                selectTab(this);
            }
            child.style.cursor = 'pointer';
        }
    }
}

function selectTab(elem) {
    var container = elem.tabContainer;
    var rCl       = elem.rCl;

    if (typeof container == "string")
        container = $(container);

    resetTabGroup(container,rCl);

    Element.removeClassName(elem,rCl);
    elem.onmouseover = null;
    elem.onmouseout  = null;
    elem.onclick     = null;
    elem.style.pointer = 'default';
    Element.show(elem.getAttribute("tabBodyId"));
}

function resetTabGroup(container,rCl) {
    var children = Element.childElements(container);
    
    for (var i = 0; i < children.length; i++) {
        children[i].onmouseover = function() {
            Element.removeClassName(this,rCl);
        }
        children[i].onmouseout = function() {
            Element.addClassName(this,rCl);
        }
        children[i].onclick = function() {
            selectTab(this);
        }
        if (children[i].className.indexOf(rCl) == -1) {
            Element.addClassName(children[i],rCl);
            children[i].style.cursor = 'pointer';
        }
        Element.hide(children[i].getAttribute("tabBodyId"));
    }
}