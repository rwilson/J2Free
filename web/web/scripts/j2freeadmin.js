var J2FreeAdmin = Class.create({

    reqSent: false,
    cache: null,
    maxCachedItems: 25,
    cacheArray: null,

    initialize: function() {
        reqSent    = false;
        cache      = new Object();
        cacheArray = new Array();
    },

    cacheEntity: function(key,ent) {
        cache[key] = ent;
        if (cacheArray.unshift(key) > maxCachedItems)
            cacheArray.pop();
    },

    clearCache: function() {
        cache = new Obect();
        cacheArray.clear();
    },

    list: function(elem,id,clazz,s,l,selector) {
        if (reqSent) 
            return;

        if (!selector)
            selector = false;

        // Get the column, if it exists, otherwise create it
        var column = $(id);
        if (!column) {
            column = document.createElement('div');
            column.className = 'column';
            column.id = id;
            Element.insert($(elem), {after: column});
        }

        // If we're working in col x, make sure there are no cols > x
        var curCol = parseInt(id.charAt(3)) + 1;
        var col = null;
        while ((col = $('col' + curCol)) != null) {
            Element.remove(col);
            curCol++;
        }

        Element.show('loader');
        new Ajax.Updater(column,'list/' + clazz, {
            parameters: {
                start: s,
                limit: l,
                selector: selector
            },
            onComplete: function() {
                reqSent = false;
                Element.hide('loader');
            }
        });
        reqSent = true;
    },

    listappend: function(id,clazz,s,l,selector) {
        if (reqSent) 
            return;

        if (!selector)
            selector = false;

        // Get the column, if it exists, otherwise create it
        var column = $(id);

        Element.show('loader');
        new Ajax.Updater(column,'list/' + clazz, {
            parameters: {
                start: s,
                limit: l,
                selector: selector
            },
            insertion: 'bottom',
            onComplete: function() {
                reqSent = false;
                Element.hide('loader');
            }
        });
        reqSent = true;
    },

    reqReturned: function() {
        reqSent = false;
    },

    find: function(elem,id,clazz,eid) {

        if (reqSent) 
            return;

        // Get the column, if it exists, otherwise create it
        var column = $(id);
        if (!column) {
            column = document.createElement('div');
            column.className = 'column';
            column.id = id;
            Element.insert($(elem), {after: column});
        }

        // if it's already in the cache, just show it
        var ent = cache[clazz + eid];
        if (ent) {
            Element.update(column,ent);
            return;
        }

        Element.show('loader');

        new Ajax.Updater(column,'find/' + clazz + '/' + eid, {
            evalScripts: true,
            onComplete: function(transport) {
                reqSent = false;
                Element.hide('loader');
                admin.cacheEntity(clazz + eid,transport.responseText);
                //Element.update(column,trasnport.responseText);
            }
        });

        reqSent = true;
    },

    save: function(clazz,eid) {
        // need to do cache maintenance here
        alert('Not yet implemented.');
    },

    remove: function(clazz,eid) {
        // need to do cache maintenance here
        alert('Not yet implemented.');
    },

    create: function(elem,id,clazz) {
        
        if (reqSent) 
            return;
        
        // Get the column, if it exists, otherwise create it
        var column = $(id);
        if (!column) {
            column = document.createElement('div');
            column.className = 'column';
            column.id = id;
            Element.insert($(elem), {after: column});
        }

        Element.show('loader');
        new Ajax.Updater(column,'create/' + clazz, {
            onComplete: function() {
                reqSent = false;
                Element.hide('loader');
            }
        });
        reqSent = true;
    },

    showGeneralSearch: function() {
       alert('Not yet implemented.');
    },

    showSearch: function() {
        alert('Not yet implemented.');
    },
    
    starveColumn: function(col) {
        if (typeof col == "string")
            col = $(col);

        col.setAttribute('originalWidth',col.getWidth());

        col.setStyle({overflowX: 'hidden'});
        new Effect.Morph(col,{style: {width: '25px'}, duration: .5});
        Event.observe(col,'mouseover',function() {
            new Effect.Morph(col,{style: {width: col.getAttribute('originalWidth') + 'px'}, duration: .5});
        });
    }
});

var admin = new J2FreeAdmin();