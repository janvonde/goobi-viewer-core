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
 * Module to manage bookmarks if the user is logged in.
 * 
 * @version 4.3.0
 * @module viewerJS.bookmarks
 * @requires jQuery
 */
var viewerJS = ( function( viewer ) {
    'use strict';
    
    var _debug = true;
    var _riotTags = [];
    var _defaults = {
        root: '',
        typePage: false,
        msg: {
            resetBookmarkLists: "",
            resetBookmarkListsConfirm: "",
            noItemsAvailable: "",
            selectBookmarkList: "",
            addNewBookmarkList: "",
            type_label: "",
            typeRecord: "",
            typePage: ""
        }
    };
    
    viewer.bookmarks = {
            listsNeedUpdate : new Rx.Subject(),
            init: function( config ) {
                if ( _debug ) {
                    console.log( '##############################' );
                    console.log( 'viewer.bookmarks.init' );
                    console.log( '##############################' );
                    console.log( 'viewer.bookmarks.init: config - ', config );
                }
                
                this.config = $.extend( true, {}, _defaults, config );
                this.typePage = this.config.typePage;
                this.renderBookmarksNavigationList();
                this.prepareBookmarksPopup();

            },
            renderBookmarksNavigationList: function() {
                var $button = $('[data-bookmark-list-type="dropdown"]');
                
                var dropdownWidth = 275;
                var buttonPosition = $button.offset();
                var buttonSize = {
                        width: $button.width(),
                        height: $button.height()
                }
                console.log("button ", buttonPosition, buttonSize)
                var dropdownPosition = {
                    left: buttonPosition.left + buttonSize.width - dropdownWidth,
                    top: buttonPosition.top + buttonSize.height
                }
                
                var $dropdown = $("<bookmarkList></bookmarkList>");
                $dropdown.addClass("bookshelf-navigation__dropdown");
//                $dropdown.css("position", "absolute");
//                $dropdown.css("top", dropdownPosition.top + "px");
//                $dropdown.css("left", dropdownPosition.left + "px");
                $button.on("click", () => $dropdown.show());
                
                $button.after($dropdown);
                console.log("mount bookmarks-dropdown") 
                riot.mount('bookmarkList', {
                    data: {        
                        pi: '',
                        logid: '',
                        page: ''
                    },
                    style: {
                        mainClass : "bookshelf-navigation__dropdown-list"
                    },
                    button: '[data-bookmark-list-type="dropdown"]',
                    msg: this.config.msg,
                    bookmarks: this,
                });
                
            },
            prepareBookmarksPopup: function() {
                
                // toggle bookmarks dropdown
                $( '[data-bookmark-list-type="dropdown"]' ).off().on( 'click', function( event ) {
                    event.stopPropagation();
                    // hide other dropdowns
                    $( '.login-navigation__login-dropdown, .login-navigation__user-dropdown, .navigation__collection-panel' ).hide();
                    $( '.bookshelf-popup' ).remove();
                    $( '.bookshelf-navigation__dropdown' ).slideToggle( 'fast' );
                } );
                
                // render bookmarks popup
                $( '[data-bookmark-list-type="add"]' ).off().on( 'click', function( event ) {
                    event.stopPropagation();
                    // hide other dropdowns
                    $( '.bookshelf-navigation__dropdown, .login-navigation__user-dropdown' ).hide();
                    
                    
                    var currBtn = $( event.target ).closest('[data-bookmark-list-type="add"]');
                    var currPi = currBtn.attr( 'data-pi' );
                    var currLogid = currBtn.attr( 'data-logid' );
                    var currPage = currBtn.attr( 'data-page' );
                    var currType = currBtn.attr( 'data-type' );
                    
                    // render bookmarks popup
                    this.renderBookmarksPopup( currPi, currLogid, currPage, currBtn );
                }.bind(this) );  
            },
            renderBookmarksPopup: function(pi, logid, page, button) {
                var $popup = $("<bookmarksPopup></bookmarksPopup>");
                $('body').append($popup);
                console.log("mount bookmarksPopup") 
                riot.mount('bookmarksPopup', {
                    data: {        
                        pi: pi,
                        logid: logid,
                        page: page
                    },
                    button: button,
                    msg: this.config.msg,
                    bookmarks: this,
                });
            },

            action: function(verb, id) {
                let url = this.config.root + (this.config.userLoggedIn ? "/rest/bookmarks/user" : "/rest/bookmarks/session");
                if(id !== undefined) {
                    url += "/get/" + id;
                };
                url += "/" + verb + "/";
                url = url.replace(/\/+/g, "/");

                return url;
            },
            

            getBookmarkLists: function() {
                
                let url = this.action("get");
                
                return fetch(url, {cache:"no-cache"})
                .then( data => data.json());
                
            },


            addToBookmarkList: function(listId, pi, page, logid, bookmarkPage) {
                
                let url = this.action("add", listId) + pi + "/"; 
                if(bookmarkPage) { 
                    url += (page ? page : "-") + "/" + (logid ? logid : "-") + "/";
                }
                
             
                return fetch(url, {method:"POST"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to add bookmark: " + data.message);
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },

            removeFromBookmarkList: function(listId, pi, page, logid, bookmarkPage) {
                
                let url = this.action("delete", listId) + pi + "/";
                if(bookmarkPage) {
                    url += (page ? page : "-") + "/" + (logid ? logid : "-") + "/";
                }
                
             
                return fetch(url, {method:"DELETE"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to remove bookmark: " + data.message);
                    } else {
                        
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },
            
            addBookmarkList: function(name) {
                
                let url = this.action("add")
                if(name) {
                    url +=  name + "/"
                }
                
             
                return fetch(url, {method:"POST"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to create bookmarklist: " + data.message);
                    } else {
                        
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },
            
            removeBookmarkList: function(id) {
                
                let url = this.action("delete")
                url += id + "/";
                
             
                return fetch(url, {method:"DELETE"})
                .then( res => res.json())
                .then(data => {
                    if(data.success === false) {
                        return Promise.reject("Failed to delete bookmarklist: " + data.message);
                    } else {
                        
                    }
                })
                .catch(error => {
                    console.log(error);
                })
                
            },
            
            isTypePage: function() {
                return this.typePage;
            },
            isTypeRecord: function() {
                return !this.typePage;
            },
            setTypePage: function() {
                this.typePage = true;
            },
            setTypeRecord: function() {
                this.typePage = false;
            }
            
    }
    return viewer;
    
} )( viewerJS || {}, jQuery );

