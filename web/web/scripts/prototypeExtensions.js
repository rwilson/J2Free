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
    replaceClassName: function(element,oldClass,newClass) {
        if (element.hasClassName(oldClass))
            element.removeClassName(oldClass);

        if (!element.hasClassName(newClass))
            element.addClassName(newClass);

        return element;
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
    fixHeight: function(element) {
        return element.setHeight(element.getHeight() + 'px').makeClipping();
    },
    unfixHeight: function(element) {
        return element.setHeight("auto").undoClipping();
    },
    // Bug fix in clonePosition
    clonePosition: function(element, source) {
        var options = Object.extend({
            setLeft:    true,
            setTop:     true,
            setWidth:   true,
            setHeight:  true,
            offsetTop:  0,
            offsetLeft: 0
        }, arguments[2] || { });

        // find page position of source
        source = $(source);
        var p = source.viewportOffset();

        // find coordinate system to use
        element = $(element);
        var delta = [0, 0];
        var parent = null;
        // delta [0,0] will do fine with position: fixed elements,
        // position:absolute needs offsetParent deltas
        if (Element.getStyle(element, 'position') == 'absolute') {
            parent = element.getOffsetParent();
            try {
                delta = parent.viewportOffset();
            } catch (e) { }
        }

        // correct by body offsets (fixes Safari)
        if (parent == document.body) {
            delta[0] -= document.body.offsetLeft;
            delta[1] -= document.body.offsetTop;
        }

        // set position
        if (options.setLeft)   element.style.left  = (p[0] - delta[0] + options.offsetLeft) + 'px';
        if (options.setTop)    element.style.top   = (p[1] - delta[1] + options.offsetTop) + 'px';
        if (options.setWidth)  element.style.width = source.offsetWidth + 'px';
        if (options.setHeight) element.style.height = source.offsetHeight + 'px';

        return element;
    }
});

Element.addMethods(["INPUT","TEXTAREA"], {
    positionCursor: function(element, pos) {

        var input = $(element);

        if (typeof pos == "string") {
            if (pos === "end") {
                pos = input.getValue().length - 1;
            } else if (pos == "start") {
                pos = 0;
            }
        }

        if (input.getValue().length - 1 >= pos) {
            if (typeof input.setSelectionRange == "function") {
                input.setSelectionRange(pos, pos);
            } else if (typeof input.createTextRange == "function") {
                var range = input.createTextRange();
                range.collapse(true);
                range.moveEnd("character", pos);
                range.moveStart("character", pos);
                range.select();
            }
        }
    }
});

Object.extend(Event, {
    KEY_CTRL: 17,   // L and R control (mac, at least)
    KEY_CMDL: 91,   // L command (mac, maybe windows key too?)
    KEY_CMDR: 93    // R command (mac, maybe windows key too?)
});

String.prototype.compareTo = function(other) {
    var len1 = this.length;
    var len2 = other.length;

    var n = len1 < len2 ? len1 : len2;

    var c1, c2;
    for (var i = 0; i < n; i++) {
        c1 = this.charCodeAt(i);
        c2 = other.charCodeAt(i);
        if (c1 != c2) {
            return (c1 - c2) < 0 ? -1 : 1;
        }
    }

    var c3 = len1 - len2;
    return c3 == 0 ? 0 : c3 < 0 ? -1 : 1;
};

String.prototype.equals = function(other) {
    if (other == null)
        return false;

    if (typeof other != "string")
        return false;

    if (this === other)
        return true;

    return (this == other);
};

String.prototype.equalsIgnoreCase = function(other) {
    if (other == null)
        return false;

    if (typeof other != "string")
        return false;

    if (this === other)
        return true;

    return (this == other);
};

Prototype.Browser.ieVersion = Prototype.Browser.IE ? parseFloat((new RegExp("MSIE ([0-9]{1,}[.0-9]{0,})")).exec(navigator.userAgent)[1]) : -1;

function onDOMLoaded(f) {
    if (document.loaded) {
        f();
    } else {
        document.observe("dom:loaded", f);
    }
}