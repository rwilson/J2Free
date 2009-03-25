//*****************************
// Author: Ryan Wilson
// Copyright 2008 Publi.us
//*****************************

var loadingImage = new Image();
loadingImage.src = "/img/loading.gif";

var Sandbox = Class.create();

var ggg_sandbox = 'sandbox';
var ggg_schoolyard = 'schoolyard';
var ggg_schoolyardFence = 'schoolyard_fence';
var ggg_playground = 'playground';
var ggg_confirm = 'confirm';
var ggg_register = 'register';
var ggg_login = 'frontdoor';
var ggg_loading = 'sandboxLoading';
var ggg_confirmQ = 'confirmQuestion';
var ggg_confirmD = 'confirmDetails';
var ggg_confirmYlink = 'confirmYlink';
var ggg_confirmForm = 'confirmForm';
var ggg_confirmNlink = 'confirmNlink';
var ggg_challenge = 'challengeFriends';
var ggg_feedback = 'feedback';
var ggg_requestPassword = 'requestPassword';

Sandbox.prototype = {

    initialize: function() {},

    hideSwfs: function() {
        var swfs = document.getElementsByTagName("object");
        var i;
        for (i = 0; i < swfs.length; i++) {
            Element.hideVisible(swfs[i]);
        }

        swfs = document.getElementsByTagName("embed");
        for (i = 0; i < swfs.length; i++) {
            Element.hideVisible(swfs[i]);
        }

        swfs = document.getElementsByTagName("select");
        for (i = 0; i < swfs.length; i++) {
            Element.hideVisible(swfs[i]);
        }
    },

    showSwfs: function() {
        var swfs = document.getElementsByTagName("object");
        for (var i = 0; i < swfs.length; i++) {
            Element.showVisible(swfs[i]);
        }

        swfs = document.getElementsByTagName("embed");
        for (var i = 0; i < swfs.length; i++) {
            Element.showVisible(swfs[i]);
        }

        swfs = document.getElementsByTagName("select");
        for (var i = 0; i < swfs.length; i++) {
            Element.showVisible(swfs[i]);
        }
    },

    showSandbox: function() {
        GoGoGadget.hideSwfs();
        var arrPageSize = GoGoGadget.getPageSize();
        var s = $(ggg_sandbox);
        s.style.width = arrPageSize[0] + 'px';
        s.style.height = arrPageSize[1] + 'px';
        Element.show(s);
        Element.scrollTo(s);
    },

    hideSandbox: function() {
        Element.hide(ggg_sandbox);
        GoGoGadget.showSwfs();
    },

    showLoading: function() {
        var psarr = this.getPageSize();
        var l = Math.round(psarr[2] / 2 - 63);
        var t = Math.round(psarr[3] / 2 - 11);
        $(ggg_loading).style.left = l + 'px';
        $(ggg_loading).style.top = t + 'px';
        Element.show(ggg_loading);
    },

    hideLoading: function() {
        Element.hide(ggg_loading);
    },

    umbrella: function() {
        $(ggg_sandbox).style.zIndex = 1100;
        GoGoGadget.showLoading();
    },

    closeUmbrella: function() {
        Element.hide(ggg_loading);
        $(ggg_sandbox).style.zIndex = 999;
    },

    loadingUmbrella: function() {
        GoGoGadget.showSandbox(function() { return; });
        $(ggg_sandbox).style.zIndex = 1100;
        GoGoGadget.showLoading();
    },

    confirm: function(question,details,answerYtxt,action,answerNtxt) {
        $(ggg_confirmQ).update(question);
        $(ggg_confirmD).update(details);
        $(ggg_confirmYlink).value = answerYtxt;
        $(ggg_confirmForm).action = action;
        $(ggg_confirmNlink).value = answerNtxt;
        Element.show(ggg_confirm);
        GoGoGadget.showSandbox();
    },

    cancelConfirm: function() {
        GoGoGadget.hideSandbox();
        Element.hide(ggg_confirm);
    },

    recess: function(url) {
        if (url.indexOf('?') == -1) {
            url += '?';
        } else {
            url += '&';
        }
        url += 'aaltohvv=' + Math.random()*1000*1000*1000*1000*1000;
        new Ajax.Request(url,{
            method: 'get',
            onSuccess: function(transport) {
                GoGoGadget.updatePlayground(transport.responseText);
            }
        });
        GoGoGadget.showSandbox();
        GoGoGadget.umbrella();
    },

    whistle: function() {
        GoGoGadget.hideSandbox();
        Element.hide(ggg_schoolyard);
        Element.update(ggg_playground,'');
    },

    updatePlayground: function(html) {
        var d = document.createElement("div");
        d.innerHTML = html;
        d.style.display = "none";
        document.body.appendChild(d);

        var pSize = GoGoGadget.getPageSize();

        var dimsNew = Element.getDimensions(d);
        dimsNew.width *= 1.05;

        document.body.removeChild(d);
        
        if (dimsNew.height + 200 > pSize[3]) {
            dimsNew.height = pSize[3] - 200;
        }

        var yard = $(ggg_schoolyard);
        yard.style.width = dimsNew.width + 'px';
        yard.style.left = (pSize[0]/2 - dimsNew.width/2) + 'px';
        yard.style.height = dimsNew.height + 'px';
        $(ggg_playground).style.height = dimsNew.height - 30 + 'px';
        Element.show(yard);
        Element.update(ggg_playground,html);
        GoGoGadget.closeUmbrella();
    },

    register: function() {
        GoGoGadget.showSandbox();
        Element.show(ggg_register);
    },

    dontRegister: function() {
        GoGoGadget.hideSandbox();
        Element.hide(ggg_register);
    },

    login: function() {
        GoGoGadget.showSandbox();
        Element.show(ggg_login);
    },

    dontLogin: function() {
        GoGoGadget.hideSandbox();
        Element.hide(ggg_login);
    },

    loginInstead: function() {
        Element.hide(ggg_register);
        Element.show(ggg_login);
    },

    registerInstead: function() {
        Element.hide(ggg_login);
        Element.show(ggg_register);
    },

    challengeFriends: function() {
        GoGoGadget.showSandbox();
        Element.show(ggg_challenge);
    },

    dontChallengeFriends: function() {
        Element.hide(ggg_challenge);
        GoGoGadget.hideSandbox();
    },

    feedback: function() {
        GoGoGadget.showSandbox();
        Element.show(ggg_feedback);
    },

    closeFeedback: function() {
        Element.hide(ggg_feedback);
        GoGoGadget.hideSandbox();
    },

    requestPassword: function() {
        GoGoGadget.showSandbox();
        Element.show(ggg_requestPassword);
    },

    closeRequestPassword: function() {
        Element.hide(ggg_requestPassword);
        GoGoGadget.hideSandbox();
    },

    popup: function(html) {
        Element.update(ggg_playground,html);
        Element.show(ggg_schoolyard);
        Element.show(ggg_schoolyardFence);
        GoGoGadget.showSandbox();
    },

    isInVisibleArea: function(el,offset) {
        var pageDims = this.getPageSize();
        var pageScroll = this.getPageScroll();
        var elemPos = Element.cumulativeOffset($(el));
        elemPos[1] += offset;
        return (elemPos[1] > pageScroll[1] && elemPos[1] < pageDims[1]);
    },

    getPageSize: function(){
        var xScroll, yScroll;
        if (window.innerHeight && window.scrollMaxY) {	
            xScroll = window.innerWidth + window.scrollMaxX;
            yScroll = window.innerHeight + window.scrollMaxY;
        } else if (document.body.scrollHeight > document.body.offsetHeight){ // all but Explorer Mac
            xScroll = document.body.scrollWidth;
            yScroll = document.body.scrollHeight;
        } else { // Explorer Mac...would also work in Explorer 6 Strict, Mozilla and Safari
            xScroll = document.body.offsetWidth;
            yScroll = document.body.offsetHeight;
        }

        var windowWidth, windowHeight;

        if (self.innerHeight) {	// all except Explorer
            if(document.documentElement.clientWidth){
                windowWidth = document.documentElement.clientWidth; 
            } else {
                windowWidth = self.innerWidth;
            }
            windowHeight = self.innerHeight;
        } else if (document.documentElement && document.documentElement.clientHeight) { // Explorer 6 Strict Mode
            windowWidth = document.documentElement.clientWidth;
            windowHeight = document.documentElement.clientHeight;
        } else if (document.body) { // other Explorers
            windowWidth = document.body.clientWidth;
            windowHeight = document.body.clientHeight;
        }	

        if(yScroll < windowHeight){
            pageHeight = windowHeight;
        } else { 
            pageHeight = yScroll;
        }

        if(xScroll < windowWidth){	
            pageWidth = xScroll;		
        } else {
            pageWidth = windowWidth;
        }

        return new Array(pageWidth,pageHeight,windowWidth,windowHeight);
    },

    getPageScroll: function() {

        var xScroll, yScroll;

        if (self.pageYOffset) {
            yScroll = self.pageYOffset;
            xScroll = self.pageXOffset;
        } else if (document.documentElement && document.documentElement.scrollTop){	 // Explorer 6 Strict
            yScroll = document.documentElement.scrollTop;
            xScroll = document.documentElement.scrollLeft;
        } else if (document.body) {// all other Explorers
            yScroll = document.body.scrollTop;
            xScroll = document.body.scrollLeft;	
        }

        return new Array(xScroll,yScroll);
    }
}

var GoGoGadget = new Sandbox();