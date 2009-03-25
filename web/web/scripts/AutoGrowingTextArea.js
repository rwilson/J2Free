var AutoGrowingTextArea = Class.create({

    initialize: function(area) {
        this.area = $(area);
        this.area.setStyle({overflowX: 'hidden'});
        this.originalWidth = this.area.getWidth();
        this.options = Object.extend({
            maxHeight: -1,
            minHeight: 25,
            start: 0
        },arguments[1] || { });
        if (this.options.start == 0) {
            this.enable();
        } else if (this.options.start > 0){
            setTimeout(this.enable.bindAsEventListener(this),this.options.start);
        }

        if (document.all) {
            Event.observe(this.area,'keydown',new function() {
                this.area.setStyle({width: this.originalWidth});
            }.bindAsEventListener(this));
        }
    },

    enable: function() {
        Event.observe(this.area,'focus',this.startChecking.bindAsEventListener(this));
        Event.observe(this.area,'blur',this.stopChecking.bindAsEventListener(this));
        this.checkExpand(true);
    },

    disable: function() {
        Event.stopObserving(this.area,'focus',this.startChecking.bindAsEventListener(this));
        Event.stopObserving(this.area,'blur',this.stopChecking.bindAsEventListener(this));
    },

    startChecking:function() {
        this.checkExpand(false);
        this.interval = setInterval(function(){this.checkExpand()}.bindAsEventListener(this), 400);
    },

    stopChecking:function() {
        clearInterval(this.interval);
        this.checkExpand(true);
    },

    checkExpand: function(end) {
        if (this.dummy == null) {
            this.dummy = Element.extend(document.createElement('div'));
            var paddingL = this.area.getStyle('padding-left');
            var paddingR = this.area.getStyle('padding-right');
            var borderL = this.area.getStyle('borderLeftWidth');
            var borderR = this.area.getStyle('borderRightWidth');
            this.dummy.setStyle({
                fontSize: this.area.getStyle('fontSize'),
                fontFamily: this.area.getStyle('fontFamily'),
                padding: this.area.getStyle('padding-top') + ' ' + paddingR + ' ' + this.area.getStyle('padding-bottom') + ' ' + paddingL,
                lineHeight: this.area.getStyle('lineHeight'),
                borderWidth: this.area.getStyle('borderTopWidth') + ' ' + borderR + ' ' + this.area.getStyle('borderBottomWidth') + ' ' + borderL,
                borderStyle: this.area.getStyle('borderTopStyle') + ' ' + this.area.getStyle('borderRightStyle') + ' ' + this.area.getStyle('borderBottomStyle') + ' ' + this.area.getStyle('borderLeftStyle'),
                width: (document.all ? this.area.getWidth() : (this.area.getWidth() - parseInt(paddingL) - parseInt(paddingR) - parseInt(borderL) - parseInt(borderR))) + 'px',
                position:  'absolute',
                top: 0,
                left: -9999 + 'px'
            });
            $(document.body).insert(this.dummy);
        }
        var html = this.area.value.replace(/(<|>)/g,'');
        html = document.all ? html.replace(/\n/g,'<BR>new') : html.replace(/\n/g,'<br>new');
        if (this.dummy.innerHTML != html) {
            this.dummy.update(html + (end ? '' : ' end words'));
            var height = this.dummy.getHeight();
            if (this.options.maxHeight > 0 && height > this.options.maxHeight) {
                this.area.setStyle({overflowY: 'auto'});
            } else {
                this.area.setStyle({overflowY: 'hidden'});
                if (height < this.options.minHeight) {
                    height = this.options.minHeight;
                }
                if (this.area.getHeight() != height) {
                    new Effect.Morph(this.area,{style: {height: height + 'px'}, duration: .4});
                }
            }
        }
    }
});