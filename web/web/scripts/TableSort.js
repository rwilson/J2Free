function $T(v,i) { 
    return((typeof(i) == 'string' ? $(i) : (i ? i : document)).getElementsByTagName(v)); 
}
function iSW(w,n) { 
    if($(w))
        $(w).id = ''; 
    if(n) 
        n.id = w; 
}

function grabText(o) {
    if (typeof(o) == "string" || typeof(o) == "undefined") 
        return(o);
    
    if (o.innerText)
        return(o.innerText); // IE
    
    if (o.textContent)
        return(o.textContent); // FireFox + Opera

    var z = '', r = o.childNodes;

    for(var i = 0; i < r.length; i++) { 

        switch(r[i].nodeType) {
            case 1: 
                z += grabText(r[i]); 
                break; // ELEMENT_NODE
            case 3:
                z += r[i].nodeValue; 
                break; // TEXT_NODE
        } 
    }
    return(z);
}

function resortTable(o,fn) { 

    SORT_INDEX = o.cellIndex;

    var T = getParent(o.parentNode,'TABLE'); 

    if (T.rows.length <= 1)
        return;

    var S = $T('span',o)[0];

    if (!S) {
        S = document.createElement('span');
        S.innerHTML = '&nbsp;';
        o.appendChild(S);
    }

    if (!fn) {
        var rX = grabText(T.rows[1].cells[SORT_INDEX]);
    
        if (rX.match(/^\d\d[\/-]\d\d[\/-][0-9]{2,4}$/)) {
            fn = sortDate;
        } else if(rX.match(/^[\d\.]+$/)) {
            fn = sortNumeric;
        } else if(rX.match(/^[£$]/)) {
            fn = sortCurrency;
        } else {
            fn = sortLower;
        }
    }

    var r = new Array();

    for (i = 1; i < T.rows.length; i++) { 
        r[i-1] = T.rows[i]; 
    }
    r.sort(fn);

    if (S.innerHTML.charCodeAt(0) == 8595) { 
        r.reverse();
        S.innerHTML = '&#8593;'; 
    } else {
        S.innerHTML = '&#8595;';
    }

    if ($('currentlySortedBy') && $('currentlySortedBy') != o) {
        $T('span',$('currentlySortedBy'))[0].innerHTML = '&nbsp;'; 
    }
    iSW('currentlySortedBy',o);

    for (i = 0; i < r.length; i++) { 
        T.tBodies[0].appendChild(r[i]); 
    }
}

function sortCurrency(a,b) {
    function z(v) { 
        return (grabText(v.cells[SORT_INDEX]).replace(/[^0-9.]/g,'')); 
    }
    return (parseFloat(z(a)) - parseFloat(z(b)));
}

function sortDate(a,b) { 
    a = z(a); 
    b = z(b);
    function z(v) { 
        v = grabText(v.cells[SORT_INDEX]); 
        return Date.parse(v);
        //return (v.substr(6,v.length == 10 ? 4 : 2) + v.substr(3,2) + v.substr(0,2)); 
    }
    return((a == b ? 0 : (a < b ? -1 : 1)));
}

function sortNumeric(a,b) { 
    a = z(a); 
    b = z(b);
    function z(v) { 
        return (parseFloat(grabText(v.cells[SORT_INDEX]))); 
    }
    return ((isNaN(a) ? 0 : a) - (isNaN(b) ? 0 : b));
}

function sortLower(a,b) { 
    a = z(a); 
    b = z(b);
    function z(v) { 
        return (grabText(v.cells[SORT_INDEX]).toLowerCase()); 
    }
    return ((a == b) ? 0 : ((a < b) ? -1 : 1));
}

function getParent(o,tag) {
    if(o == null) 
        return null;
    else if (o.nodeType == 1 && o.tagName.toLowerCase() == tag.toLowerCase()) 
        return (o);
    else 
        return (getParent(o.parentNode,tag));
}