(function jqueryNamespacing($, undefined){
	
	window.optimized4NetworkTransmission = function(node){
		delete node.li_attr;
		delete node.a_attr;
		delete node.data;
		for(var i=0; i<node.children.length; i++){
			optimized4NetworkTransmission(node.children[i]);
		}
	}
	
	$.jstree.defaults.decorators = {
			// <selector, handler(node, targetElem)> 
	};
	
	// it would keep selected elements under each <li/> persists
	$.jstree.plugins.decorators = function (options, parent) {
		this.bind = function(){
			parent.bind.call(this);
			this.element
				.on('loading.jstree', jQuery.proxy(function (e, data){
					// patch up for the root node
					this._data.core.original_container_html = $('<ul/>').append(this._data.core.original_container_html);
				}, this))
		};
		
		this.redraw_node = function(obj, deep, is_callback) {
			obj = parent.redraw_node.call(this, obj, deep, is_callback);
			if(options){
				var liContainer = this._data.core.original_container_html.find('#' + obj.id);
				for(var selector in options){
					// I currently restrict this on children level
					var targetElem = liContainer.children(selector).clone(true);
					var handler = options[selector];
					if(targetElem.length && $.isFunction(handler)){
						// please be ware of that, the obj is not yet appended to the document
						handler(obj, targetElem);
					}
				}
			}
			return obj;
		};
		
		this.update_redraw_template = function(nodeId, selector, newTemplate){
			var liContainer = this._data.core.original_container_html.find('#' + nodeId);
			// I currently restrict this on children level
			var targetElem = liContainer.children(selector).remove();
			var handler = options[selector];
			if(targetElem.length && $.isFunction(handler)){
				handler(liContainer, targetElem);
			}
			// update to page
			this.redraw_node(nodeId);
		}
		
	}
	
	window.jstreeTablization = function(treeId, options, onReadyHandler){
		
		if(treeId.indexOf('#') == -1){
			treeId = '#' + treeId;
		}
		
		
		$(treeId).jstree(options)
			.on('ready.jstree', function(){
				var jstreeInst = $.jstree.reference(this);
				// since prototype.js has polluted native JSON relevant methods, might as well do it here
				var container = jstreeInst.get_container();
				container.children('.jstree-container-ul').addClass('jstree-wholerow-container jstree-no-dots')
				container.find('[data-jstree]').each(function(i, e){
					var node = jstreeInst.get_node(e, true);
					var state = node.data().jstree;
					var anchor = node.children('a.jstree-anchor');
					jstreeInst.set_type(e, state.type);
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
