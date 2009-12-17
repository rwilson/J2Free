/* Additional Transitions */
Effect.Transitions.exponential = function(pos) {
    return 1 - Math.pow(1 - pos,2);
}

/* Version of move with patch for % positions */
Effect.Move = Class.create(Effect.Base, {
  initialize: function(element) {
    this.element = $(element);
    if (!this.element) throw(Effect._elementDoesNotExistError);
    var options = Object.extend({
      x:    0,
      y:    0,
      mode: 'relative'
    }, arguments[1] || { });
    this.start(options);
  },
  setup: function() {
    this.element.makePositioned();

    var dims = J2Free.getPageSize();

    var orig = this.element.getStyle('left') || '0';
    if (orig.match(/\d+%$/)) {
        orig = orig.replace(/%/,"");
        this.originalLeft = (parseFloat(orig) / 100) * dims.pageWidth;
    } else {
        this.originalLeft = parseFloat(this.element.getStyle('left') || '0');
    }

    orig = this.element.getStyle('top') || '0';
    if (orig.match(/\d+%$/)) {
        orig = orig.replace(/%/,"");
        this.originalTop = (parseFloat(orig) / 100) * dims.pageHeight;
    } else {
        this.originalTop = parseFloat(this.element.getStyle('top')  || '0');
    }

    if (this.options.mode == 'absolute') {
      this.options.x = this.options.x - this.originalLeft;
      this.options.y = this.options.y - this.originalTop;
    }
  },
  update: function(position) {
    this.element.setStyle({
      left: (this.options.x  * position + this.originalLeft).round() + 'px',
      top:  (this.options.y  * position + this.originalTop).round()  + 'px'
    });
  }
});


/*  Not used right now ...
Effect.Transitions.exponentialReverse = function(pos) {
    return Math.pow(pos,2);
}

Effect.Transitions.sinoidalReverse = function(pos) {
    return 1 - ((-Math.cos(pos * Math.PI) / 2) + 0.5);
}

Effect.FadeUpdate = function(element,newcontents) {
    var af = function(effect) {
                var e = effect.element;
                e.update(newcontents);
                new Effect.Appear(e);
            };
    var o = arguments[2] || {};
    Object.extend(o,{ afterFinish: af });
    new Effect.Fade(element,o);
}

Effect.SlideRightIntoView = function(element) {
    element = $(element);
    element.style.width = '0px';
    element.makeClipping();
    element.firstChild.style.position = 'relative';
    element.show();
    return new Effect.Scale(element, 100, Object.extend({
        scaleContent: false,
        scaleY: false,
        scaleMode: 'contents',
        scaleFrom: 0,
        afterUpdate: function(effect) { }
    }, arguments[1] || {}));
}

Effect.SlideRightOutOfView = function(element) {
    element = $(element);
    element.makeClipping();
    $(element.firstChild).style.position = 'relative';
    element.show();
    return new Effect.Scale(element, 0, Object.extend({
        scaleContent: false,
        scaleY: false,
        afterUpdate: function(effect){},
        afterFinish: function(effect) { 
            $(effect.element).hide(); 
        }
    }, arguments[1] || {}));
}

Effect.BlindLeft = function(element) {
    element = $(element);
    element.makeClipping();
    return new Effect.Scale(element, 0, Object.extend({ scaleContent: false, 
        scaleY: false,                     
        restoreAfterFinish: true,
        afterFinishInternal: function(effect) {
            effect.element.hide().undoClipping();
        } 
    }, arguments[1] || {}));
}


Effect.BlindRight = function(element) {
    element = $(element);
    var elementDimensions = element.getDimensions();
    return new Effect.Scale(element, 100, Object.extend({ 
        scaleContent: false, 
        scaleY: false,
        scaleFrom: 0,
        scaleMode: {originalHeight: elementDimensions.height, originalWidth: elementDimensions.width},
        restoreAfterFinish: true,
        afterSetup: function(effect) {
            effect.element.makeClipping().setWidth('0px').show();
            if (effect.options.hideContents) 
                effect.element.childElements().invoke('hide');
        },  
        afterFinishInternal: function(effect) {
            if (effect.options.hideContents) 
                effect.element.childElements().invoke('show');
            effect.element.undoClipping();
        }
    }, arguments[1] || {}));
}

Effect.BounceOnce = Class.create(Effect.Base, {
    initialize: function(element) {
        this.element = $(element);
        var options = Object.extend({
            x:0, 
            y:200,
            acceleration: 9.81,
            transition: Effect.Transitions.linear,
            mode: 'relative'
        }, arguments[1] || {});
        this.start(options);
    },

    setup: function() {
        Element.makePositioned(this.element);
        this.originalLeft = parseFloat(Element.getStyle(this.element,'left') || '0');
        this.originalTop  = parseFloat(Element.getStyle(this.element,'top')  || '0');
        if(this.options.mode == 'absolute') {
            this.options.x = this.options.x - this.originalLeft;
        }
    },

    mytransition: function(pos){
        var temp = (pos < 0.5 ? 0.5 - pos : 0.5 + (1 - pos)); 
        return (pos < 0.5 ? this.options.acceleration / 2 * temp * temp : this.options.acceleration / 2 * (1 - temp) * (1 - temp) ) * 8 / this.options.acceleration - 1;
    },
  
    update: function(position) {
        Element.setStyle(this.element, {
            left: this.options.x  * position + this.originalLeft + 'px',
            top:  this.originalTop + this.options.y * this.mytransition(position)   + 'px'
        });
    }
});

Effect.Bounce = function(element) {
    var options = Object.extend({
        repeat: arguments[1] || 0,
        queue: {position: 'end', scope: 'bounce_queue'},
        afterFinishInternal: function(effect) {
            effect.options.repeat -= 1;
            if (effect.options.repeat > 0) {
                return new Effect.Bounce(effect.element,effect.options.repeat, effect.options);
            }

            return null;
        }
    }, arguments[2] || { });
    return new Effect.BounceOnce(element,options);
}

BezierCurve = Class.create({
    initialize: function(controlPoints) {
        this.C = new Array();
        for (var i = 0; i < controlPoints.length; i++) {
            this.C[i] = controlPoints[i] || {x: 0, y: 0};
        }
    },
    quadratic: function(p,a) {
        return this.C[0][a] * p * p           +
               this.C[1][a] * 2 * p * (1 - p) +
               this.C[2][a] * (1 - p) * (1 - p);
    },
    cubic: function(p,a) {
        return this.C[0][a] * p * p * p                 +
               this.C[1][a] * 3 * p * p * (1 - p)       + 
               this.C[2][a] * 3 * p * (1 - p) * (1 - p) +
               this.C[3][a] * (1 - p) * (1 - p) * (1 - p);
    },
    quatric: function(p,a) {
        // to be implemented
    },
    x: function(position) {
        if (this.C.length == 3) {
            return this.quadratic(position,'x');
        } else if (this.C.length == 4) {
            return this.cubic(position,'x');
        } else {
            return position;
        }
    },
    y: function(position) {
        if (this.C.length == 3) {
            return this.quadratic(position,'y');
        } else if (this.C.length == 4) {
            return this.cubic(position,'y');
        } else {
            return position;
        }
    }
});
*/
/* Curve Can accept any curve object that implements x(pos) and y(pos) methods
 * to return the next position to move to.
 */
/* The responsibility for defining the curve lies on the user.
 */
/* Not Used
Effect.Curve = Class.create(Effect.Base, {
    initialize: function(element) {
        this.element = $(element);
        if (!this.element) throw(Effect._elementDoesNotExistError);
        var options = Object.extend({
            curve: new BezierCurve(
                          {x: 0, y: 0}, 
                          {x: 0, y: 0},
                          {x: 0, y: 0},
                          {x: 0, y: 0}
                       )
        }, arguments[1] || { });
        this.start(options);
    },
    setup: function() {
        this.element.makePositioned();
    },
    update: function(position) {
        this.element.setStyle({
            left: this.options.curve.x(position).round() + 'px',
            top:  this.options.curve.y(position).round() + 'px'
        });
    }
});
*/
/* CurveTo Can accept any curve object that implements x(pos) and y(pos) methods
 * to return the next position to move to.
 *
 * This method handles the responsibility for creating the curve following the
 * rule of 'always curve toward the middle of the page'.  It accepts arguments
 * for the start and end positions as well as the desired radius.  The radius is
 * not really the radius so much as it is a measurement of the distance from a 
 * straight line that the curve should follow.
 *
 *  pps = pixels per second, used to auto-calculate duration based on the
 *        desired speed of movement.  Also utilizes min/maxDuration for
 *        constraining the auto-calculated duration to a reasonable range.
 *
 * points = number of points to form the shape of the curve (currently
 *          supporting only 3 or 4 points)
 */
/* Not Used
Effect.CurveTo = Class.create(Effect.Base, {
    initialize: function(element) {
        this.element = $(element);
        if (!this.element) throw(Effect._elementDoesNotExistError);
        
        var options = Object.extend({
            // for curving
            scale: true,
            radius: 200,
            start: null,
            end: null,
            pps: 1000,
            duration: null,
            minDuration: 0,
            maxDuration: null,
            points: 3,
            // for scaling
            scaleX: true,
            scaleY: true,
            scaleContent: true,
            scaleMode: 'box',       // set to 'none' to disable 3D effect
            scaleFrom: 100.0,
            scaleTo: 200.0,
            // other
            transition: Effect.Transitions.sinoidal,
            debug: false
        }, arguments[1] || {});
        
        var controlPoints = new Array();

        var pos = null;
        try {
            pos = element.cumulativeOffset();
        } catch (err) { }
        
        var start = options.start || {x: pos ? pos.left : 0, y: pos ? pos.top : 0};
        var end   = options.end   || {x: 0, y: 0};

        controlPoints.push(end);

        if (!options.duration) {
            var dist = Math.sqrt(Math.pow(end.x - start.x,2) + Math.pow(end.y - start.y,2));
            options.duration = dist / options.pps;
            if (options.duration < options.minDuration) {
                options.duration = options.minDuration;
            }
            if (options.maxDuration && options.duration > options.maxDuration) {
                options.duration = options.maxDuration;
            }
        }

        var pageDims = $(document.body).getDimensions();

        // Get the dimensions of the page
        var w = pageDims.width / 2, h = pageDims.height / 2;

        // What quadrant of the screen are we starting in?
        var q0 = start.x < w && start.y < h;
        var q1 = start.x > w && start.y < h;
        var q2 = start.x < w && start.y > h;
        var q3 = start.x > w && start.y > h;

        var xOff = (0.25 * Math.abs(end.x - start.x)).round();
        var yOff = (0.25 * Math.abs(end.y - start.y)).round();

        if (xOff < options.radius) xOff = options.radius;
        if (yOff < options.radius) yOff = options.radius;

        var xOffset, yOfset;

        // Create a rectangle where start and end are two points, and the other
        // two points are generated to form a rectangle toward the middle of the
        // page.
        //
        // if the shape is too narrow, things look funny, so there's a min width
        if (Math.abs(start.x - end.x) < options.radius) {
            xOffset = (q0 || q2) ? xOff * -1 : xOff;
            yOffset = (q0 || q1) ? yOff * -1 : yOff;

            if (start.y == end.y) {
                // starting and ending in the same place, so a curve for this would actually
                // be a triangle, not a rectangle.  Maybe work on this another day...
                // For now, do nothing, get out.
                //
                 return;
            } else {
                if (options.points == 4) {
                    controlPoints.push({x: (q0 || q2) ? start.x + options.radius : start.x - options.radius, y: start.y});
                    controlPoints.push({x: (q0 || q2) ? start.x + options.radius : start.x - options.radius, y: end.y  });
                } else if (options.points == 3) {
                    controlPoints.push({x: start.x + xOffset, y: end.y + yOffset});
                } else {
                    return;
                }
            }
        } else {
            xOffset = (q0 || q2) ? xOff * -1 : xOff;
            yOffset = (q0 || q1) ? yOff : yOff * -1;

            if (Math.abs(start.y - end.y) < options.radius) {
                if (options.points == 4) {
                    controlPoints.push({x: start.x, y: (q0 || q1) ? start.y + options.radius : start.y - options.radius });
                    controlPoints.push({x: end.x  , y: (q0 || q1) ? start.y + options.radius : start.y - options.radius });
                } else if (options.points == 3) {
                    controlPoints.push({x: end.x + xOffset, y: end.y + yOffset});
                } else {
                    return;
                }
            } else {
                if (options.points == 4) {
                    controlPoints.push({x: end.x  , y: start.y});
                    controlPoints.push({x: start.x, y: end.y  });
                } else if (options.points == 3) {
                    controlPoints.push({x: end.x + xOffset, y: end.y + yOffset});
                } else {
                    return;
                }
            }
        }
        controlPoints.push(start);
        options.curve = new BezierCurve(controlPoints);
        this.start(options);
    },
    setup: function() {
        // for curving
        if (this.element.style.position != 'absolute')
            this.element.makePositioned();

        if (this.options.debug) {
            $$('._curveDebugPoint').invoke('remove');
            $A(this.options.curve.C).each(function(c) {
                var d = new Element('div');
                d.setStyle({
                    width: '4px',
                    height: '4px',
                    backgroundColor: 'red',
                    position: 'absolute',
                    zIndex: 5,
                    left: (c.x - 2) + 'px',
                    top: (c.y - 2) + 'px'
                });
                d.addClassName('_curveDebugPoint');
                document.body.appendChild(d);
            });
        }

        // for scaling
        if (this.options.scaleMode != "none") {
            this.restoreAfterFinish = this.options.restoreAfterFinish || false;
            this.elementPositioning = this.element.getStyle('position');

            this.originalStyle = { };
            ['width','height','fontSize'].each( function(k) {
                this.originalStyle[k] = this.element.style[k];
            }.bind(this));

            var fontSize = this.element.getStyle('font-size') || '100%';
            ['em','px','%','pt'].each( function(fontSizeType) {
                if (fontSize.indexOf(fontSizeType) > 0) {
                    this.fontSize     = parseFloat(fontSize);
                    this.fontSizeType = fontSizeType;
                }
            }.bind(this));

            this.options.scaleTo *= 2;

            this.factor = (this.options.scaleTo - this.options.scaleFrom) / 100;

            this.dims = null;
            if (this.options.scaleMode == 'box')
                this.dims = [this.element.offsetHeight, this.element.offsetWidth];
            if (/^content/.test(this.options.scaleMode))
                this.dims = [this.element.scrollHeight, this.element.scrollWidth];
            if (!this.dims)
                this.dims = [this.options.scaleMode.originalHeight,
                             this.options.scaleMode.originalWidth];
         }
    },
    update: function(position) {
        this.element.setStyle({
            left: this.options.curve.x(position).round() + 'px',
            top:  this.options.curve.y(position).round() + 'px'
        });
        if (this.options.scaleMode != "none") {
            var currentScale = position <= 0.5 ? (this.options.scaleFrom / 100.0) + (this.factor * position) : (this.options.scaleTo / 100.0) - (this.factor * position);
            if (this.options.scaleContent && this.fontSize) {
                this.element.setStyle({fontSize: this.fontSize * currentScale + this.fontSizeType });
            }
            this.setDimensions(this.dims[0] * currentScale, this.dims[1] * currentScale);
        }
    },
    finish: function(position) {
        // set back to original size
        if (this.options.scaleMode != "none") {
            this.setDimensions(this.dims[0], this.dims[1]);
        }
    },
    setDimensions: function(height, width) {
        var d = { };
        if (this.options.scaleX) d.width = width.round() + 'px';
        if (this.options.scaleY) d.height = height.round() + 'px';
        this.element.setStyle(d);
    }
});

Effect.Shoot = function(shooter,target) {
    shooter = $(shooter);
    target = $(target);
    
    var particle = new Element('div');
    Element.extend(particle);
    particle.setStyle({
        backgroundColor: '#444',
        width: '12px',
        height: '12px',
        position: 'absolute'
    });
    particle.setOpacity(.8);

    shooter.insert(particle);

    particle.clonePosition(shooter,{setHeight: false, setWidth: false});

    var td = target.getDimensions();
    var tc = target.cumulativeOffset();

    new Effect.CurveTo(particle, {
        end: { x: tc.left + td.width / 2 - 3, y: tc.top + td.height / 2 - 3 },
        pps: 750,
        points: 3,
        afterFinish: function(effect) {
            effect.element.remove();
        }
    });
};
*/