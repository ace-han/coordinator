(function jqueryNamespacing($, undefined){
	
	window.optimized4NetworkTransmission = function(node){
		delete node.li_attr;
		delete node.a_attr;
		delete node.data;
		delete node.icon;
		for(var i=0; i<node.children.length; i++){
			window.optimized4NetworkTransmission(node.children[i]);
		}
	}
	
	window.patchUpTreeNode = function (jstreeInst, node) {
		var state;
		if(!jstreeInst){
			jstreeInst = $.jstree.reference(node.id);
		}
		var liContainer = jstreeInst.get_node(node.id, true);
		if(liContainer.find('>a.jstree-anchor>.jstree-undetermined').length){
			node.state.undetermined = true;
		}
		// since this kind of info. stores in node.data
		if(node.data && node.data.jstree){
			state = node.data.jstree;
			node.state.breaking = state.breaking;
			node.state.execPattern = state.execPattern;
		}
		for(var i=0; i<node.children.length; i++){
			window.patchUpTreeNode(jstreeInst, node.children[i]);
		}
		
	}
	
	$.jstree.defaults.decorators = {
			// <selector, handler(node, targetElem)> 
	};
	
	// options=> a map for <selector, handler>
	$.jstree.plugins.decorators = function (options, parent) {
		this.bind = function(){
			parent.bind.call(this);
			this.element
				.on('loading.jstree', jQuery.proxy(function (e, data){
					// patch up for the root node
					this._data.core.original_container_html = $('<ul/>').append(this._get_original_container_html());
				}, this))
		};
		
		this.redraw_node = function(obj, deep, is_callback) {
			// follow the new style with apply arguments in jstree.js
			obj = parent.redraw_node.apply(this, arguments);
			if(!$.isEmptyObject(options)){
				var liContainer = this._get_original_container_html().find('#' + obj.id);
				for(var selector in options){
					// I currently restrict this on children level
					var targetElem = liContainer.children(selector).clone(true);
					var handler = options[selector];
					if(targetElem.length && $.isFunction(handler)){
						// please be ware of that, the obj is not yet appended to the document
						handler.call(this, obj, targetElem);
					}
				}
			}
			return obj;
		};
		
		this.update_redraw_template = function(nodeId, selector, newTemplate){
			// for ajax polling template(newTemplate) to redraw
			var liContainer = this._get_original_container_html().find('#' + nodeId);
			// I currently restrict this on children level
			var targetElem = liContainer.children(selector).remove();
			var handler = options[selector];
			if(targetElem.length && $.isFunction(handler)){
				handler.call(this, liContainer, newTemplate);
			}
			// update to page
			this.redraw_node(nodeId);
		}
		
		// add "_" prefix for marking this function a private function
		this._get_original_container_html = function(){
			return this._data.core.original_container_html;
		}
	}
	
	window.jstreeTablization = function(selector, options, onReadyHandler){
		$(selector).jstree(options)
			.on('ready.jstree', function(){
				var jstreeInst = $.jstree.reference(this);
				// since prototype.js has polluted native JSON relevant methods, might as well do it here
				var container = jstreeInst.get_container();
				container.children('.jstree-container-ul').addClass('jstree-wholerow-container jstree-no-dots')
				container.find('[data-jstree]').each(function(i, e){
					var node = jstreeInst.get_node(e, true);
					var state = node.data().jstree;
					var anchor = node.children('a.jstree-anchor');
					// jstreeInst.set_type(e, state.type);
					if(jstreeInst.is_leaf(e)){
						if(state.checked){
							jstreeInst.check_node(e);
						}		
					}
				})
				// patch up anti wholerow selection
				container.find('.jstree-wholerow-clicked').removeClass('jstree-wholerow-clicked');
				
				// add header
				var headers = ['<div class="jstree-table-header-cont">',
				               '<div class="jstree-table-header jobStatus">Status</div>',
				              	'<div class="jstree-table-header lastDuration">Last Duration</div>',
				              	'<div class="jstree-table-header lastLaunch">Last Launch</div>',
				              	'<div class="jstree-table-header buildNumber">Build #</div>',
				              	'<div class="clear"></div>',
				              '</div>'];
				container.prepend(headers.join(''));
				
				if($.isFunction(onReadyHandler)){
					onReadyHandler(jstreeInst);
				}
				
				// logic copied from /lib/layout/breadcrumbs.js
				var menuSelector = $('#menuSelector').get(0);
				container.on('mouseover', 'a.model-link', function () {
		            menuSelector.canceller.cancel();
		            menuSelector.show(this);
		        }).on('mouseout',function () {
		            menuSelector.canceller.schedule();
		        });
			});
	}
})(jQuery);
