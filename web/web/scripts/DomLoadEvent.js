// Here are my functions for adding the DOMContentLoaded event to browsers other
// than Mozilla.

// Array of DOMContentLoaded event handlers.
window.onDOMLoadEvents = [];
window.DOMContentLoadedInitDone = false;

// Function that adds DOMContentLoaded listeners to the array.
function addDOMLoadEvent(listener) {
    // If the DOMContentLoaded event has happened, run the function.
    if(window.DOMContentLoadedInitDone){
        listener();
        return;
    }

    window.onDOMLoadEvents[window.onDOMLoadEvents.length] = listener;
}

// Function to process the DOMContentLoaded events array.
function DOMContentLoadedInit() {
    // quit if this function has already been called
    if (window.DOMContentLoadedInitDone) {
        return;
    }

    // flag this function so we don't do the same thing twice
    window.DOMContentLoadedInitDone = true;

    // iterates through array of registered functions 
    for (var i = 0; i < window.onDOMLoadEvents.length; i++) {
        var func = window.onDOMLoadEvents[i];
        func();
    }
}

function DOMContentLoadedScheduler() {
    // quit if the init function has already been called
    if (window.DOMContentLoadedInitDone) {
        return true;
    }

    // First, check for Safari or KHTML.
    // Second, check for IE.
    //if DOM methods are supported, and the body element exists
    //(using a double-check including document.body, for the benefit of older moz builds [eg ns7.1] 
    //in which getElementsByTagName('body')[0] is undefined, unless this script is in the body section)
    if(/KHTML|WebKit/i.test(navigator.userAgent)) {
        if(/loaded|complete/.test(document.readyState)) {
            DOMContentLoadedInit();
        } else {
            // Not ready yet, wait a little more.
            setTimeout("DOMContentLoadedScheduler()", 100);
        }
    } else if($("__ie_onload")) {
        return true;
    }

    // Check for custom developer provided function.
    if(typeof DOMContentLoadedCustom === "function") {
        if(typeof document.getElementsByTagName !== 'undefined' && (document.getElementsByTagName('body')[0] !== null || document.body !== null)) {
            // Call custom function.
            if(DOMContentLoadedCustom()) {
                DOMContentLoadedInit();
            } else {
                // Not ready yet, wait a little more.
                setTimeout("DOMContentLoadedScheduler()", 100);
            }
        }
    }

    return true;
}

// If addEventListener supports the DOMContentLoaded event.
Event.observe(document,'DOMContentLoaded',DOMContentLoadedInit);

// Schedule to run the init function.
setTimeout("DOMContentLoadedScheduler()", 100);

// Just in case window.onload happens first, add it there too.
Event.observe(window,'load',DOMContentLoadedInit);

/* for Internet Explorer */
/*@cc_on
    @if (@_win32 || @_win64)
    document.write("<script id=__ie_onload defer src=\"//:\"><\/script>");
    var script = $("__ie_onload");
    script.onreadystatechange = function() {
        if (this.readyState == "complete") {
            DOMContentLoadedInit(); // call the onload handler
        }
    };
    @end
@*/