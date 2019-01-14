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
 * Module which handles the viewer reading mode.
 * 
 * @version 3.4.0
 * @module viewImage.readingMode
 * @requires jQuery
 */
var ImageView = ( function( imageView ) {
    'use strict';
    
    var _debug = false;
    var _fadeout;
    var _defaults = {
    	resizeSelector: '#readingModeViewSidebar',
    	msg: {}
    };
    
    imageView.readingMode = {
        /**
         * Method to initialize the viewer reading mode.
         * 
         * @method init
         */
        init: function( config ) {
            if ( _debug ) {
                console.log( '##############################' );
                console.log( 'osViewer.readingMode.init' );
                console.log( '##############################' );
                console.log( 'osViewer.readingMode.init: config - ', config );
            }
            
            $.extend( true, _defaults, config );
            
            // switch to fullscreen after mouse doesn't move
            $( '#readingModeViewImage' ).on( 'mousemove', function() {  
                _switchToFullscreen();
            } );
            
            // make elements resizable
            if ( window.matchMedia( '(min-width: 769px)' ).matches ) {
            	_setResizable( _defaults.resizeSelector );
            }
            
            // reset tooltips for sidebar
            $( '.widget-toc__title-expand [data-toggle="tooltip"]' ).tooltip( 'destroy' );
            $( '.widget-toc__title-expand [data-toggle="tooltip"]' ).tooltip( {
            	placement: 'bottom'
            } );
            
            // set sidebar and panel status
            _setSidebarStatus();
            _setPanelStatus();

            // toggle sidebar
            $( 'body' ).on( 'click', '[data-close="rm-sidebar"]', function() {
            	$( '#readingModeViewSidebar' ).addClass( 'closed' );
            	setTimeout( function() {
            		$( '#viewSidebarOpen' ).fadeIn( 'fast' );
            		_unsetResizable( _defaults.resizeSelector );
            		
            		sessionStorage.setItem( 'rmSidebarStatus', false );
            	}, 300 );
            } );
            $( 'body' ).on( 'click', '[data-open="rm-sidebar"]', function() {
            	$( '#readingModeViewSidebar' ).removeClass( 'closed' );
            	setTimeout( function() {
            		$( '#viewSidebarOpen' ).hide();
            		_setResizable( _defaults.resizeSelector );
            		
            		sessionStorage.setItem( 'rmSidebarStatus', true );
            	}, 300 );
            } );
            
            // scroll resize handle
            $( '#readingModeViewSidebar' ).on( 'scroll', function() {
            	$( '.ui-resizable-handle' ).css( 'top', $( this ).scrollTop() );
            } );
            
            // toggle sidebar panels
            $( 'body' ).on( 'click', '.reading-mode__view-sidebar-accordeon-panel-title', function() {
                var parentPanelId = $( this ).parent().attr( 'id' );
                var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );
                
                if ( $( this ).hasClass( 'in' ) ) {
                    $( this ).toggleClass( 'in' );
                    $( this ).next().slideToggle( 'fast' );
                    
                    panelSessionStatus[ parentPanelId ] = false;
                    sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
                }
                else {                	
                    $( '.reading-mode__view-sidebar-accordeon-panel-title' ).each( function() {
                        $( this ).removeClass( 'in' );
                    } );
                    $( '.reading-mode__view-sidebar-accordeon-panel-body' ).each( function() {
                        $( this ).slideUp( 'fast' );
                    } );

                    $( this ).toggleClass( 'in' );
                    $( this ).next().slideToggle( 'fast' );
                    
                    for ( var status in panelSessionStatus ) {
                    	panelSessionStatus[ status ] = false;
                    }

                    panelSessionStatus[ parentPanelId ] = true;
                    sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
                }
            } );

            // hide all panels
            $( 'body' ).on( 'click', '[data-close="all-tabs"]', function() {
            	var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );
            	
            	$( '.reading-mode__view-sidebar-accordeon-panel-title' ).each( function() {
            		$( this ).removeClass( 'in' );
                    $( this ).next().slideUp( 'fast' );
            	} );
            	
            	for ( var status in panelSessionStatus ) {
                	panelSessionStatus[ status ] = false;
                }
            	
            	sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
            } );

            // show all panels
            $( 'body' ).on( 'click', '[data-open="all-tabs"]', function() {
            	var panelSessionStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );

            	$( '.reading-mode__view-sidebar-accordeon-panel-title' ).each( function() {
            		$( this ).addClass( 'in' );
            		$( this ).next().slideDown( 'fast' );
            	} );
            	
            	for ( var status in panelSessionStatus ) {
                	panelSessionStatus[ status ] = true;
                }
            	
            	sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelSessionStatus ) );
            } );            
        },
    };
    
    /**
     * @description Method to set the sidebar status.
     * @method _setSidebarStatus
     * */
    function _setSidebarStatus() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setSidebarStatus' );
    	}
    	
    	var sidebarStatus;
    	
    	if ( sessionStorage.getItem( 'rmSidebarStatus' ) == undefined || sessionStorage.getItem( 'rmSidebarStatus' ) == '' ) {
    		sessionStorage.setItem( 'rmSidebarStatus', true );
    	}
    	else {
    		sidebarStatus = sessionStorage.getItem( 'rmSidebarStatus' );
    		
    		if ( sidebarStatus === 'false'  ) {
    			$( '#viewSidebarOpen' ).show();
    			$( '#readingModeViewSidebar' ).addClass( 'closed' );    			
    			_unsetResizable( _defaults.resizeSelector );
    		}
    	}
    }
    
    /**
     * @description Method to set the accordeon panel status.
     * @method _setPanelStatus
     * */
    function _setPanelStatus() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _setPanelStatus' );
    	}
    	
    	var panelStatus;
    	
    	if ( sessionStorage.getItem( 'rmPanelStatus' ) == undefined || sessionStorage.getItem( 'rmPanelStatus' ) == '' ) {
    		panelStatus = {};
    		
    		// build panel status object
    		$( '.reading-mode__view-sidebar-accordeon-panel' ).each( function() {
    			var currId = $( this ).attr( 'id' );
    			
    			if ( !panelStatus.hasOwnProperty( currId ) ) {
    				panelStatus[ currId ] = false;
    			}
    			else {
    				return false;
    			}        	
    		} );
    		
    		// write object to session storage
    		sessionStorage.setItem( 'rmPanelStatus', JSON.stringify( panelStatus ) );    		
    	}
    	else {
    		panelStatus = JSON.parse( sessionStorage.getItem( 'rmPanelStatus' ) );
    		
    		$( '.reading-mode__view-sidebar-accordeon-panel' ).each( function() {
    			var currId = $( this ).attr( 'id' );
    			
    			if ( panelStatus[ currId ] ) {
    				$( this ).find( '.reading-mode__view-sidebar-accordeon-panel-title' ).addClass( 'in' );
    				$( this ).find( '.reading-mode__view-sidebar-accordeon-panel-body' ).show();
    			}    			        	
    		} );
    	}
    }

    /**
     * @description Method to initialize the resizable view.
     * @method _setResizable
     * @param {String} selector The selector of the element which should be resizable.
     * */
    function _setResizable( selector ) {
    	if ( _debug ) {
            console.log( 'EXECUTE: _setResizable' );
            console.log( 'selector: ', selector );
        }
    	
    	$( selector ).resizable({
    		handles: 'w',
    		minWidth: 400,
    		maxWidth: 900
    	});
    }
    
    /**
     * @description Method to destroy the resizable view.
     * @method _unsetResizable
     * @param {String} selector The selector of the element which should be unset.
     * */
    function _unsetResizable( selector ) {
    	if ( _debug ) {
            console.log( 'EXECUTE: _unsetResizable' );
            console.log( 'selector: ', selector );
        }

    	$( selector ).resizable( 'destroy' );
    }

    /**
     * @description Method to switch the view to fullscreen.
     * @method _switchToFullscreen
     * */
    function _switchToFullscreen() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _switchToFullscreen' );
    	}  	
    	
    	if ( _fadeout ) {
            clearTimeout( _fadeout );
            _hideFullscreen();
        }

    	_fadeout = setTimeout( _showFullscreen, 3000 );
    }

    /**
     * @description Method to show the fullscreen.
     * @method _showFullscreen
     * */
    function _showFullscreen() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _showFullscreen' );
    	}
    	
    	$( '#readingModeHeader' ).addClass( 'fullscreen' );
    }
    
    /**
     * @description Method to hide the fullscreen.
     * @method _hideFullscreen
     * */
    function _hideFullscreen() {
    	if ( _debug ) {
    		console.log( 'EXECUTE: _hideFullscreen' );
    	}
    	
    	$( '#readingModeHeader' ).removeClass( 'fullscreen' );
    }
    
    return imageView;
    
} )( ImageView );

var viewImage = ImageView;
