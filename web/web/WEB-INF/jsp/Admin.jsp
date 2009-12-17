<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>

<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://jawr.net/tags" prefix="jawr" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/strict.dtd">

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>J2Free - Entity Admin</title>
        <jawr:script src="/bundles/lib.js" />
        <style type="text/css">
            body {
                font-family: "Lucida Grande", Tahoma, Arial, sans-serif;
                font-size: 9pt;
                line-height: 1.2em;
                margin: 0;
                padding: 0;
                background-color: #666;
            }
            a, a:link, a:visited {
                color: #396F30;
                text-decoration: none;
            }
            a:hover {
                text-decoration: none;
                color: #333;
            }
            input[type=text] {
                border-width: 1px;
                border-style: solid;
                border-color: #888;
                background-color: #F8F8F8;
                font-size: 10pt;
                padding: 2px 5px;
            }
            textarea {
                border-width: 1px;
                border-style: solid;
                border-color: #888;
                background-color: #F8F8F8;
                font-size: 10pt;
                padding: 2px 5px;
                width: 300px;
                height: auto;
                overflow: hidden;
            }
            input[readonly], input[type=text].readonly, textarea.readonly {
                background-color: #CCC;
            }
            .header {
                font-size: 16pt;
                color: #666;
                padding: 25px 20px;
                border-bottom: 1px #bbb solid;
                border-top: 1px #bbb solid;
                background-color: #D5EFD1;
            }
            .headerRight {
                float: right;
                text-align: right;
                padding: 0;
                margin: 0;
                padding: 15px 20px;
            }
            .headerRight div {
                text-align: right;
                font-size: 14pt;
                color: #676767;
                margin-bottom: 5px;
            }
            .headerRight span {
                text-align: right;
                font-size: 10.5pt;
                color: #858585;
            }
            .footer {
                text-align: center;
                color: #eee;
                padding: 20px 0;
                border-top: 1px #999 solid;
                background-color: #666;
            }
            .content {
                background-color: #F5F5F5;
                height: 550px;
                padding: 0;
                margin: 0;
                border-top: 1px #888 solid;
                border-bottom: 1px #ccc solid;
            }
            #col1 {
            }
            #col2 {
                width: 250px;
            }
            #col3 {
                max-width: 450px;
                border-right-color: transparent;
            }
            #col4 {
                float: right;
            }
            .column {
                float: left;
                border-right: 1px #ccc solid;
                padding: 0;
                height: 550px;
                overflow: hidden;
            }
            .column .columnHeaderRight {
                float: right;
                padding: 12px 10px 0 0;
                cursor: pointer;
            }
            .column .columnHeader {
                font-size: 12pt;
                color: #444;
                padding: 12px 0 0 10px;
                line-height: 1.1em;
            }
            #col3 .columnHeader {
                line-height: 1.4em;
            }
            .column ul {
                height: 500px;
                list-style-type: none;
                margin: 5px 0 0;
                padding: 0 0 0 0;
                overflow-x: hidden;
                overflow-y: auto;
            }
            .column ul li {
                padding: 0;
                font-size: 10pt;
                cursor: pointer;
            }
            .column ul li a, .column ul li a:link, .column ul li a:visited {
                padding: 5px 10px 5px 20px;
                display: block;
                text-decoration: none;
                color: #396F30;
            }
            .column ul li a:hover {
                background-color: #396F30;
                color: #FFF;
            }
            .column ul li a.selected, .column ul li a.selected:link, .column ul li a.selected:visited {
                background-color: #ddd;
            }
            .column ul li a.selected:hover {
                background-color: #ddd;
                color: #396F30;
                cursor: default;
            }
            .loader {
                font-size: 12pt;
                position: absolute;
                right: 0px;
                top: 67px;
                width: 100px;
                text-align: center;
                padding: 8px 0;
                border: 1px #FFE08F solid;
                background-color: #FFF4BF;
                color: #BF6700;
            }
            .infoTable {
                margin: 10px 20px;
            }
            .infoTable tr td {
                vertical-align: top;
            }
            .infoTable tr td.label {
                vertical-align: top;
                padding-top: 5px;
            }
            .moreLoader {
                text-align: center;
                padding: 5px 0;
            }
        </style>
    </head>
    <body>
        <div class="headerRight">
            <div>Redeeming Java Webapps</div>
            <span>One crunch at a time</span>
        </div>
        <div class="header">J2Free - Entity Admin</div>
        <div class="content">
            <div class="column" id="col1">
                <div class="columnHeader">Entities</div>
                <ul>
                    <c:forEach items="${availableEntities}" var="entity">
                        <li>
                            <a href="javascript:void(0);" onclick="admin.list('col1','col2','${entity.simpleName}',0,100);$$('#col1 ul li a').invoke('removeClassName','selected');this.addClassName('selected');">${entity.simpleName}</a>
                        </li>
                    </c:forEach>
                </ul>
                <div class="columnHeader">&nbsp;</div>
                <ul>
                    <li><a href="javascript:void(0);" onclick="admin.showSearch();">Search</a></li>
                </ul>
            </div>
        </div>
        <div class="footer">
            &copy; 2009 FooBrew, Inc. | Version 0.1
        </div>
        <div id="loader" class="loader" style="display:none;">loading...</div>
    </body>
    <script type="text/javascript">
        
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
                var column = this.getOrCreateAndInsertColumn(id, elem);

                // If we're working in col x, make sure there are no cols > x
                var curCol = parseInt(id.charAt(3)) + 1;
                var col = null;
                while ((col = $("col" + curCol)) != null) {
                    col.remove();
                    curCol++;
                }

                this.doUpdate(column,"list/" + clazz, {
                    parameters: {
                        start: s,
                        limit: l,
                        selector: selector
                    }
                });
            },

            listappend: function(id,clazz,s,l,selector) {
                if (reqSent)
                    return;

                if (!selector) selector = false;

                // Get the column, if it exists, otherwise create it
                var column = this.getOrCreateAndInsertColumn(id, elem);

                this.doUpdate(column, "list/" + clazz, {
                    parameters: {
                        start: s,
                        limit: l,
                        selector: selector
                    },
                    insertion: "bottom"
                });
            },

            reqReturned: function() {
                reqSent = false;
            },

            find: function(elem, id, clazz, eid) {

                if (reqSent)
                    return;

                // Get the column, if it exists, otherwise create it
                var column = this.getOrCreateAndInsertColumn(id, elem);

                // if it"s already in the cache, just show it
                var ent = cache[clazz + eid];
                if (ent) {
                    column.update(ent);
                    return;
                }

                this.doUpdate(column,"find/" + clazz + "/" + eid, {
                    evalScripts: true,
                    onComplete: function(transport) {
                        admin.cacheEntity(clazz + eid, transport.responseText);
                    }
                });
            },

            save: function(clazz,eid) {
                // need to do cache maintenance here
                alert("Not yet implemented.");
            },

            remove: function(clazz, eid) {
                // need to do cache maintenance here
                alert("Not yet implemented.");
            },

            create: function(elem, id, clazz) {

                if (reqSent)
                    return;

                // Get the column, if it exists, otherwise create it
                var column = this.getOrCreateAndInsertColumn(id, elem);

                this.doUpdate(column,"create/" + clazz, {
                    onComplete: function() {
                        reqSent = false;
                        Element.hide("loader");
                    }
                });
            },

            showGeneralSearch: function() {
                alert("Not yet implemented.");
            },

            showSearch: function() {
                alert("Not yet implemented.");
            },

            starveColumn: function(col) {
                
                if (typeof col == "string")
                    col = $(col);

                col.setAttribute("originalWidth",col.getWidth());

                col.setStyle({overflowX: "hidden"});
                new Effect.Morph(col,{style: {width: "25px"}, duration: .5});
                Event.observe(col,"mouseover",function() {
                    new Effect.Morph(col,{style: {width: col.getAttribute("originalWidth") + "px"}, duration: .5});
                });
            },

            getOrCreateAndInsertColumn: function(id, elem) {
                var column = $(id);
                if (!column) {
                    column = new Element("div", { className: "column", id: id });
                    $(elem).insert({ after: column });
                }
                return column;
            },

            doUpdate: function(column, url, props) {
                if (!props.onComplete) {
                    props.onComplete = function() {
                                            this.reqSent = false;
                                            $("loader").hide();
                                        }.bind(this);
                }
                $("loader").show();
                new Ajax.Updater(column, url, props);
                this.reqSent = true;
            }
        });

        var admin = new J2FreeAdmin();
    </script>
</html>