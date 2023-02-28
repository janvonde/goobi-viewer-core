/**
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 * 
 * Visit these websites for more information. - http://www.intranda.com -
 * http://digiverso.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Module to initialize the backend functionality for geomaps
 * 
 * @version 4.6.0
 * @module cmsJS.geoMapBackend
 * @requires jQuery
 */
var cmsJS = ( function( cms ) {
    'use strict';
    
    // variables
    var _debug = false;
    var _defaults = {
        mapId: "geomap",
        displayLanguage : "de",
        supportedLanguages: ["de", "en"],
        popover: "#geoMapPopoverTemplate",
        allowEditFeatures: false,
        featuresInput: "#featuresInput",
        viewInput: "#viewInput",
        metadataArea: "#featureForm",
        loader: ".ajax_loader",
        msg: {
            emptyMarker: "...",
            titleLabel: "title",
            titleHelp: "help title",
            descLabel: "description",
            descHelp: "help description",
            deleteLabel: "delete"
        },
        heatmap: {
	    	enabled: false,
	    	heatmapUrl: "/viewer/api/v1/index/spatial/heatmap/{solrField}",
	    	featureUrl: "/viewer/api/v1/index/spatial/search/{solrField}",
	    	filterQuery: "BOOL_WKT_COORDS:*",
	        labelField: "LABEL",
        }
    };
    
    cms.GeoMapEditor = function(config) {
        this.config = $.extend({}, _defaults, config);
        this.currentFeature = undefined;
        this.onDeleteClick = new rxjs.Subject();
        this.onMetadataUpdate = new rxjs.Subject();
        this.metadataProvider = new rxjs.Subject();
        
        this.geoMap = new viewerJS.GeoMap({
            language: this.config.displayLanguage,
            layers: [{
            	language: this.config.displayLanguage,
	            popover: this.config.popover,
	            emptyMarkerMessage: this.config.msg.emptyMarker,
	            allowMovingFeatures: this.config.allowEditFeatures,
            }],
        });
        
        this.geoMap.onMapRightclick
        .pipe(rxjs.operators.takeWhile(() => this.config.allowEditFeatures), rxjs.operators.map(geojson => this.addFeature(geojson)))
        .subscribe(() => this.saveFeatures());
        
        this.geoMap.onMapClick
        .pipe(rxjs.operators.takeWhile(() => this.config.allowEditFeatures))
        .subscribe(() => this.setCurrentFeature());
        
        this.geoMap.layers.forEach(l => l.onFeatureClick
	        .pipe(rxjs.operators.takeWhile(() => this.config.allowEditFeatures))
	        .subscribe(geojson => this.setCurrentFeature(geojson, true))
        );
        
        this.geoMap.layers.forEach(l => l.onFeatureMove
	        .pipe(rxjs.operators.takeWhile(() => this.config.allowEditFeatures), rxjs.operators.map(geojson => this.setCurrentFeature(geojson)))
	        .subscribe(() => this.saveFeatures())
	   );
        
        this.geoMap.onMapMove
        .subscribe(() => this.saveView())
        
        this.onDeleteClick
        .pipe(rxjs.operators.takeWhile(() => this.config.allowEditFeatures), rxjs.operators.map(() => this.deleteCurrentFeature()))
        .subscribe(() => this.saveFeatures());
        
        this.onMetadataUpdate
        .pipe(rxjs.operators.takeWhile(() => this.config.allowEditFeatures), rxjs.operators.map((metadata) => this.updateCurrentFeatureMetadata(metadata)))
        .subscribe(() => this.saveFeatures());
    
        if(this.config.loader) {
            viewerJS.jsfAjax.begin.subscribe((e) => {
                if($(e.source).closest("#mapPanel").length > 0) {                    
                    $(this.config.loader).show();
                }
            });
            viewerJS.jsfAjax.complete.subscribe(() => $(this.config.loader).hide());
        }
    }
    
    cms.GeoMapEditor.prototype.init = function(defaultView) {
		console.log("init ", this);
        let features = JSON.parse($(this.config.featuresInput).val());
        features = features.filter(f => f.geometry);
        this.geoMap.init(this.getView(defaultView))
        .then( () => {
			this.geoMap.layers[0].init(features);
	        //display search results as heatmap
	    	if(this.config.heatmap.enabled) {	        	    
	        	this.heatmap = L.solrHeatmap(this.config.heatmap.heatmapUrl, this.config.heatmap.featureUrl, this.geoMap.layers[0], {
	        	    field: "WKT_COORDS",
	        	    type: "clusters",
	        	    filterQuery: this.config.heatmap.filterQuery,
	        	    labelField: this.config.heatmap.labelField,
	        	    queryAdapter: "goobiViewer"    
	        	});
	        	this.heatmap.addTo(this.geoMap.map);
	    	}    
	        
	        if($("metadataEditor").length > 0) {  
	            if(this.metadataEditor) {
	                this.metadataEditor.forEach(component => {
	                    component.unmount(true);
	                })
	            }
	            this.metadataEditor = riot.mount("metadataEditor", {
	                languages: this.config.supportedLanguages,
	                metadata: undefined,
	                provider: this.metadataProvider,
	                currentLanguage: this.config.displayLanguage,
	                updateListener: this.onMetadataUpdate,
	                deleteListener : this.onDeleteClick,
	                deleteLabel : this.config.msg.deleteLabel
	            });
	        }
		});
        
    }
    
    cms.GeoMapEditor.prototype.addFeature = function(geojson) {
        this.currentFeature = geojson;
        this.geoMap.layers[0].addMarker(geojson).openPopup();
        this.updateMetadata(geojson);
    }
    
    cms.GeoMapEditor.prototype.saveFeatures = function() {
        $(this.config.featuresInput).val(JSON.stringify(this.geoMap.layers[0].getFeatures()));
    }
    
    cms.GeoMapEditor.prototype.saveView = function() {
        $(this.config.viewInput).val(JSON.stringify(this.geoMap.getView()));
    }


    /**
     * Updates the content of the metadata editor with the content of the given geojson object
     * Also scrolls to the top of the metadata edtor (config.metadataArea)
     */
    cms.GeoMapEditor.prototype.updateMetadata = function(geojson) {
        let metadataList = [
            {   
                property: "title",
                tyle: "text",
                label: this.config.msg.titleLabel,
                value: geojson ? geojson.properties.title : "",
                required: false,
                helptext: this.config.msg.titleHelp,
                editable: geojson != undefined
            },
            {   
                property: "description",
                type:"longtext",
                label: this.config.msg.descLabel,
                value: geojson ? geojson.properties.description: "",
                required: false,
                helptext: this.config.msg.descHelp,
                editable: geojson != undefined
            }
        ]
        this.metadataProvider.next(metadataList);
        if(geojson && this.config.metadataArea) {
            $("html, body").scrollTop($(this.config.metadataArea).offset().top)
        }
    }
    
    /**
     * Set the current feature to the given feature if it isn't already the current feature or 'deselectIfCurrent' is false
     * Otherwise, set the current feature to undefined, effectively unselecting the given feature
     * Update the metadata editor with the current feature
     */
    cms.GeoMapEditor.prototype.setCurrentFeature = function(geojson, deselectIfCurrent) {
        if(_debug)console.log("set current feature ", geojson);
        if(deselectIfCurrent && this.currentFeature == geojson) {
            this.currentFeature = undefined;
            this.updateMetadata();
        } else {        
            this.currentFeature = geojson;
            this.updateMetadata(geojson);
        }
    }
    
    cms.GeoMapEditor.prototype.deleteCurrentFeature = function() {
        if(this.currentFeature) {
            this.geoMap.layers[0].removeMarker(this.currentFeature);
            this.currentFeature = undefined;
            this.updateMetadata();
        }
    }
    
    cms.GeoMapEditor.prototype.updateCurrentFeatureMetadata = function(metadata) {
        if(this.currentFeature && metadata) {                                                
            this.currentFeature.properties[metadata.property] = metadata.value;
            this.geoMap.layers[0].updateMarker(this.currentFeature.id);
        }
    }
    
    cms.GeoMapEditor.prototype.getView = function(view) {
        if(!view) {
            view = $(this.config.viewInput).val();
            if(!view) {
                view = this.geoMap.config.initialView;
            } else {                
                view = JSON.parse(view);
            }
            if(this.geoMap.layers[0].getFeatures().length > 0) {            
                return this.geoMap.getViewAroundFeatures(this.geoMap.layers[0].getFeatures(), view.zoom);
            } else {           
                return view;
            }
        } else {
            return view;
        }
    }

    cms.GeoMapEditor.prototype.updateView = function(view) {
        if(!view) {
            view = $(this.config.viewInput).val();
            if(!view) {
                view = this.geoMap.config.initialView;
            } else {                
                view = JSON.parse(view);
            }
            if(this.geoMap.layers[0].getFeatures().length > 0) {            
                this.geoMap.setView(this.geoMap.getViewAroundFeatures(this.geoMap.layers[0].getFeatures(), view.zoom));
            } else {           
                this.geoMap.setView(view);
            }
        } else {
            this.geoMap.setView(view);
        }
       
    }
    
    /**
     * Update all map features from the content of the config.featuresInput field
     */
    cms.GeoMapEditor.prototype.updateFeatures = function() {

        let features = JSON.parse($(this.config.featuresInput).val())
        if(_debug)console.log("update features", features);
        features.forEach( feature => {
            this.geoMap.layers[0].addMarker(feature);
        })
    }
    
    cms.GeoMapEditor.prototype.setAllowEditFeatures = function(allow) {
        this.config.allowEditFeatures = allow;
        this.geoMap.layers[0].config.allowMovingFeatures = allow;
        this.geoMap.layers[0].markers.filter(m => m.dragging).forEach(marker => {
            if(allow) {
                marker.dragging.enable();
            } else {
                marker.dragging.disable();
            }
        })
    }

        
       
    return cms;
    
} )( cmsJS || {}, jQuery );