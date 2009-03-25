var imagePath = "/public/img/globals/tooltiparrow"
var imageExt  = ".gif";

var _directions = {
    up: 0,
    right: 1,
    down: 2,
    left: 3
};

var offsetsfromcursor = [
    { x: -13, y: -20 },
    { x: 10, y: -13 },
    { x: -13, y: 20 },
    { x: -20, y: -13 }
];

var offsetsfrompointer = [
    { x: -13, y: 2 },
    { x: 13, y: -2 },
    { x: -13, y: 13 },
    { x: 2, y: -2 }
];

var ie = document.all;
var ns6 = document.getElementById && !document.all;
var enabletip = false;
var globalTooltipEnable = false;

var tooltip, direction, globalTooltip, globalTooltipPointer, globalTooltipTimer, globalTooltipToggle;
var tooltipPointers = [4];

function initTooltip() {
    tooltip = Element.extend(document.createElement('div'));
    tooltip.id = 'tooltip';
    tooltip.addClassName('tooltip');
    tooltip.hide();
    document.body.appendChild(tooltip);

    globalTooltip = Element.extend(document.createElement('div'));
    globalTooltip.id = 'globalTooltip';
    globalTooltip.addClassName('tooltip');
    globalTooltip.hide();
    document.body.appendChild(globalTooltip);

    ['up','right','down','left'].each(function(d) {
        if (document.images) {
            var t = new Image();
            Element.extend(t);
            t.src = imagePath + d + imageExt;
            t.id = 'tooltipPointer' + d;
            t.addClassName('tooltipPointer');
            t.hide();
            document.body.appendChild(t);
            tooltipPointers[_directions[d]] = t;
        } else {
            document.write('<img id="tooltipPointer' + d + '" class="tooltipPointer" src="' + imagePath + d + imageExt + '">');
            var t = $('tooltipPointer' + d);
            t.hide();
            tooltipPointers[_directions[d]] = t;
        }
    });

    if (document.images) {
        globalTooltipPointer = new Image();
        Element.extend(globalTooltipPointer);
        globalTooltipPointer.src = imagePath + 'up' + imageExt;
        globalTooltipPointer.id = 'globalTooltipPointer';
        globalTooltipPointer.addClassName('tooltipPointer');
        globalTooltipPointer.hide();
        document.body.appendChild(globalTooltipPointer);
    } else {
        document.write('<img id="globalTooltipPointer" class="globalTooltipPointer" src="' + imagePath + 'up' + imageExt + '">');
        globalTooltipPointer = $('globalTooltipPointer');
        globalTooltipPointer.hide();
    }

    direction = _directions['up'];

    $$('.addToolTip').each(function(e) {
        e.tooltiptext = e.title;
        e.removeAttribute("title");
        var cursor = e.getStyle('cursor');
        if (!cursor || cursor == 'default') {
            e.setStyle({cursor: 'help'});
        }
        e.observe('mouseover',createTooltip.bindAsEventListener(e));
        e.observe('mouseout',function() {
            hideTooltip();
        });
    });
    
    globalTooltipToggle = $('globalTooltipToggle');
    globalTooltipToggle.tooltiptext = '<div class="tooltipClose" onclick="hideGlobalTooltip();">[x]</div><span>Mouse-over Tooltips</span><div>Click here to disable mouse-over tooltips throughout Fantasy Congress.</div>';
    globalTooltipToggle.observe('mouseover',createTooltip.bindAsEventListener(globalTooltipToggle));
    globalTooltipToggle.observe('mouseout',function() {
        hideTooltip();
    });
    
    var c = readCookie('fcEnableTooltips');
    globalTooltipEnable = !c || (c && c == 'true');

    if (!globalTooltipEnable) {
        globalTooltipToggle.tooltiptext = '<div class="tooltipClose" onclick="hideGlobalTooltip();">[x]</div><span>Mouse-over Tooltips</span><div>Click here to enable mouse-over tooltips throughout Fantasy Congress.</div>';
        globalTooltipToggle.update('enable tooltips');
    }
}

function globalToggleTooltips() {
    globalTooltipEnable = !globalTooltipEnable;
    hideGlobalTooltip();
    if (globalTooltipToggle.innerHTML.indexOf('disable') >=0 ) {
        hideGlobalTooltip();
        globalTooltipToggle.update('enable tooltips');
        globalTooltipToggle.tooltiptext = '<div class="tooltipClose" onclick="hideGlobalTooltip();">[x]</div><span>Mouse-over Tooltips</span><div>Click here to enable mouse-over tooltips throughout Fantasy Congress.</div>';
        tooltip.update(globalTooltipToggle.tooltiptext);
    } else {
        globalTooltipToggle.update('disable tooltips');
        globalTooltipToggle.tooltiptext = '<div class="tooltipClose" onclick="hideGlobalTooltip();">[x]</div><span>Mouse-over Tooltips</span><div>Click here to disable mouse-over tooltips throughout Fantasy Congress.</div>';
        tooltip.update(globalTooltipToggle.tooltiptext);
    }
    var ex = new Date(); 
    ex.setTime(ex.getTime() + 1000*60*60*24*30);
    setCookie('fcEnableTooltips',globalTooltipEnable,ex,'/');
}

function ietruebody() {
    return (document.compatMode && document.compatMode != "BackCompat") ? document.documentElement : document.body;
}

function createTooltip(txt){
    if (this.getAttribute('disableTooltip'))
        return;

    if (!globalTooltipEnable || this == globalTooltipToggle) {
        if (this != globalTooltipToggle) {
            var c = readCookie('globalTooltipReminder');
            if (c && c == 'true') {
                return;
            } else {
                var ex = new Date(); 
                ex.setTime(ex.getTime() + 1000*60*60*24);
                setCookie('globalTooltipReminder','true',ex,'/');
            }
        }
        if (!globalTooltip.visible()) {
            globalTooltip.update(globalTooltipToggle.tooltiptext);
            [globalTooltip,globalTooltipPointer].invoke('show');
            positionGlobalTip();
            [globalTooltip,globalTooltipPointer].invoke('hide');
            [globalTooltip,globalTooltipPointer].invoke('appear',{duration: .4});
        }
        clearTimeout(globalTooltipTimer);
        globalTooltipTimer = setTimeout(hideGlobalTooltip,5000);
        return;
    }
    
    if (globalTooltip.visible) 
        hideGlobalTooltip();
    
    if (arguments[1]) {
        if (arguments[1].style) {
            tooltip.setStyle(arguments[1].style);
        }
        if (arguments[1].direction) {
            direction = _directions[arguments[1].direction];
        }
    }
    tooltip.update(this.tooltiptext);
    [tooltip,tooltipPointers[direction]].invoke('show');
    enabletip = true;
}

function positionTip(e){
    if (!enabletip) return;

    var curX = (ns6) ? e.pageX : event.clientX + ietruebody().scrollLeft;
    var curY = (ns6) ? e.pageY : event.clientY + ietruebody().scrollTop;

    //Find out how close the mouse is to the corner of the window
    var winwidth  = ie && !window.opera ? ietruebody().clientWidth  : window.innerWidth  - 20;
    var winheight = ie && !window.opera ? ietruebody().clientHeight : window.innerHeight - 20;

    var rightedge  = ie && !window.opera ? winwidth  - event.clientX - offsetsfromcursor[direction].x : winwidth  - e.clientX - offsetsfromcursor[direction].x;
    var bottomedge = ie && !window.opera ? winheight - event.clientY - offsetsfromcursor[direction].y : winheight - e.clientY - offsetsfromcursor[direction].y;

    var leftedge = (offsetsfromcursor[direction].x < 0) ? offsetsfromcursor[direction].x * (-1) : -1000;

    var tdims = tooltip.getDimensions();

    var nextdirection = direction;

    //if the horizontal distance isn't enough to accomodate the width of the context menu
    if (direction == _directions['left']) {
        var l = curX - tdims.width + offsetsfromcursor[direction].x + offsetsfrompointer[direction].x;
        if (l < 0) {
            tooltipPointers[direction].hide();
            direction = _directions['right'];
            tooltipPointers[direction].show();
            tooltip.style.left = (curX + offsetsfromcursor[direction].x + offsetsfrompointer[direction].x) + 'px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
            nextdirection = _directions['left'];
        } else {
            tooltip.style.left = l + 'px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
        }
    } else if (direction == _directions['right']) {
        if (rightedge < tdims.width) {
            tooltipPointers[direction].hide();
            direction = _directions['left'];
            tooltipPointers[direction].show();
            tooltip.style.left = (curX - tdims.width + offsetsfromcursor[direction].x + offsetsfrompointer[direction].x) + 'px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
            nextdirection = _directions['right'];
        } else {
            tooltip.style.left = (curX + offsetsfromcursor[direction].x + offsetsfrompointer[direction].x) + 'px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
        }
    } else {
        if (rightedge < tdims.width) {
            //move the horizontal position of the menu to the left by it's width
            tooltip.style.left = (curX - tdims.width - offsetsfromcursor[direction].x - offsetsfrompointer[direction].x) + 'px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
        } else if (curX < leftedge) {
            tooltip.style.left = '5px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
        } else {
            //position the horizontal position of the menu where the mouse is positioned
            tooltip.style.left = (curX + offsetsfromcursor[direction].x + offsetsfrompointer[direction].x) + 'px';
            tooltipPointers[direction].style.left = (curX + offsetsfromcursor[direction].x) + 'px';
        }
    }

    //same concept with the vertical position
    if (direction == _directions['up']) {
        var h = (curY + offsetsfromcursor[direction].y - tdims.height + offsetsfrompointer[direction].y);
        tooltip.style.top = h + 'px';
        tooltipPointers[direction].style.top = (curY + offsetsfromcursor[direction].y) + 'px';
    } else {
        if (bottomedge < tdims.height) {
            tooltip.style.top = (curY - tdims.height + offsetsfromcursor[direction].y) + 'px';
        } else {
            tooltip.style.top = (curY + offsetsfromcursor[direction].y + offsetsfrompointer[direction].y) + 'px';
            tooltipPointers[direction].style.top = (curY + offsetsfromcursor[direction].y) + 'px';
        }
    }
    
    direction = nextdirection;
}

function hideTooltip() {
    enabletip = false;
    tooltip.hide();
    tooltipPointers.invoke('hide');
}

function hideGlobalTooltip() {
    [globalTooltip,globalTooltipPointer].invoke('fade',{duration: .4});
    globalTooltipTimer = null;
}

function positionGlobalTip(){
    var off = globalTooltipToggle.cumulativeOffset();
    var dim = globalTooltipToggle.getDimensions();

    // fake the current cursor position
    var curX = off.left + parseInt(dim.width / 2);
    var curY = off.top + parseInt(dim.height / 2);

    //Find out how close the mouse is to the corner of the window
    var winwidth  = ie && !window.opera ? ietruebody().clientWidth  : window.innerWidth  - 20;
    var winheight = ie && !window.opera ? ietruebody().clientHeight : window.innerHeight - 20;

    var rightedge  = winwidth  - curX - offsetsfromcursor[_directions['up']].x;
    var bottomedge = winheight - curY - offsetsfromcursor[_directions['up']].y;

    var leftedge = (offsetsfromcursor[_directions['up']].x < 0) ? offsetsfromcursor[_directions['up']].x * (-1) : -1000;

    var tdims = globalTooltip.getDimensions();

    if (rightedge < tdims.width) {
        globalTooltip.style.left = (curX - tdims.width - offsetsfromcursor[_directions['up']].x - offsetsfrompointer[_directions['up']].x) + 'px';
        globalTooltipPointer.style.left = (curX + offsetsfromcursor[_directions['up']].x) + 'px';
    } else if (curX < leftedge) {
        globalTooltip.style.left = '5px';
        globalTooltipPointer.style.left = (curX + offsetsfromcursor[_directions['up']].x) + 'px';
    } else {
        globalTooltip.style.left = (curX + offsetsfromcursor[_directions['up']].x + offsetsfrompointer[_directions['up']].x) + 'px';
        globalTooltipPointer.style.left = (curX + offsetsfromcursor[_directions['up']].x) + 'px';
    }

    var h = (curY + offsetsfromcursor[_directions['up']].y - tdims.height + offsetsfrompointer[_directions['up']].y);
    globalTooltip.style.top = h + 'px';
    globalTooltipPointer.style.top = (curY + offsetsfromcursor[_directions['up']].y) + 'px';
}

Event.observe(document,'mousemove',positionTip);
Event.observe(window,'load',initTooltip);