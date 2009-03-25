Element.addMethods({
    addClassNames: function(element) {
        for (var i = 1; i < arguments.length; i++) {
            element.addClassName(arguments[i]);
        }
    },

    removeClassNames: function(element) {
        for (var i = 1; i < arguments.length; i++) {
            element.removeClassName(arguments[i]);
        }
    },

    hasClassNames: function(element) {
        for (var i = 1; i < arguments.length; i++) {
            if (!element.hasClassName(arguments[i]))
                return false;
        }
        return true;
    },
    setHeight: function(element,h) {
        return element.setStyle({height: h});
    },
    setWidth: function(element,w) {
        return element.setStyle({width: w});
    },
    setVisibility: function(element,v) {
        return element.setStyle({visibility: v});
    },
    display: function(element,d) {
        return element.setStyle({display: d});
    },
    createElement: function(tagName) {
        var e = document.createElement(tagName);
        return Element.extend(e);
    },
    fixHeight: function(element) {
        element.makeClipping();
        return element.setStyle({height: element.getHeight() + 'px'});
    }
});