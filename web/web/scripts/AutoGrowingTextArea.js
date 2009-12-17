var AutoGrowingTextArea = Class.create({

    initialize: function(area) {

        this.boundStart = this.startChecking.bindAsEventListener(this);
        this.boundStop  = this.stopChecking.bindAsEventListener(this);
        this.boundCheck = this.checkExpand.bindAsEventListener(this);

        this.boundKeyUp   = this.keyUp.bindAsEventListener(this);
        this.boundKeyDown = this.keyDown.bindAsEventListener(this);

        this.ctrlKeysDown = 0;

        this.area = $(area);
        this.area.setStyle({overflowX: "hidden", resize: "none"});
        this.originalWidth = this.area.getWidth();
        this.options = Object.extend({
            maxHeight: -1,
            minHeight: 25,
            start: 0,
            submitOnCtrlEnter: null
        },arguments[1] || { });

        if (this.options.start == 0) {
            this.enable();
        } else if (this.options.start > 0){
            setTimeout(this.enable.bindAsEventListener(this),this.options.start);
        }

        if (this.options.submitOnCtrlEnter) {
            this.area.observe("keydown", this.boundKeyDown);
            this.area.observe("keyup", this.boundKeyUp);
        }
        
        if (document.all) {
            this.area.observe("keypress",function(event) {
                $(this.area).setWidth(this.originalWidth + "px");
            }.bindAsEventListener(this));
        }
    },

    enable: function() {
        this.area.observe("focus",this.boundStart);
        this.area.observe("blur",this.boundStop);
        this.checkExpand(true);
    },

    disable: function() {
        this.area.stopObserving("focus",this.boundStart);
        this.area.stopObserving("blur",this.boundStop);
    },

    startChecking:function() {
        this.checkExpand(false);
        this.interval = setInterval(this.boundCheck, 400);
    },

    stopChecking:function() {
        clearInterval(this.interval);
        this.checkExpand(true);
    },

    checkExpand: function(end) {
        
        if (this.dummy == null) {

            this.dummy = new Element("div");

            ["fontSize","fontFamily","fontStyle","fontWeight","textDecoration",
             "paddingTop","paddingBottom","paddingLeft","paddingRight",
             "borderTopWidth","borderRightWidth","borderBottomWidth","borderLeftWidth",
             "borderTopStyle","borderRightStyle","borderBottomStyle","borderLeftStyle",
             "lineHeight","letterSpacing"].each(function(k) {
                this.dummy.style[k] = this.area.getStyle(k);
            }.bindAsEventListener(this));

            var padW    = parseInt(this.area.getStyle("paddingLeft")) + parseInt(this.area.getStyle("paddingRight"));
            var borderW = parseInt(this.area.getStyle("borderLeftWidth")) + parseInt(this.area.getStyle("borderRightWidth"));
            
            this.dummy.setStyle({
                position:  "absolute",
                top: "0px",
                left: "-9999px"
            }).setWidth((document.all ? this.area.getWidth() : (this.area.getWidth() - padW - borderW)) + "px");
            
            $(document.body).insert(this.dummy);

        }
        
        var html = this.area.value.replace(/(<|>)/g,"");
        html = document.all ? html.replace(/\n/g,"<BR>new") : html.replace(/\n/g,"<br/>new");

        if (this.dummy.innerHTML != html) {

            this.dummy.update(html + (end ? "" : " extra words"));
            
            var height = this.dummy.getHeight();
            
            if (this.options.maxHeight > 0 && height >= this.options.maxHeight) {

                this.area.setHeight(this.options.maxHeight);
                this.area.setStyle({
                    overflowY: "auto"
                });
                
            } else {

                this.area.setStyle({
                    overflowY: "hidden"
                });

                if (height < this.options.minHeight) {
                    height = this.options.minHeight;
                }

                if (this.area.getHeight() != height) {
                    new Effect.Morph(this.area, {
                        style: {
                            height: height + "px"
                        },
                        duration: 0.4
                    });
                }
            }
        }
    },

    keyUp: function(e) {
        if (e.keyCode == Event.KEY_CTRL || e.keyCode == Event.KEY_CMDL || e.keyCode == Event.KEY_CMDR) {
            this.ctrlKeysDown--;
        }
    },

    keyDown: function(e) {
        if (e.keyCode == Event.KEY_RETURN) {
            if (this.ctrlKeysDown > 0) {
                
                e.stop();

                if (typeof this.options.submitOnCtrlEnter == "function") {
                    this.options.submitOnCtrlEnter();
                } else if (typeof this.options.submitOnCtrlEnter == "boolean") {
                    var form = this.area.up("form");
                    if (form) {
                        form.submit();
                    }
                }
            }
        } else if (e.keyCode == Event.KEY_CTRL || e.keyCode == Event.KEY_CMDL || e.keyCode == Event.KEY_CMDR) {
            this.ctrlKeysDown++;
        }
    }
});