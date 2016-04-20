(function jQueryNamespacing($){
	$(function(){
		$('.ui-tabs').on('click', '.ui-tabs-tab', function(e){
			e.preventDefault();	// just avoid appending #xxx to the url
			var self = $(this);
			var tabsContext = self.closest('.ui-tabs');
			$('.ui-tab-active', tabsContext).removeClass('ui-tab-active');
			var targetAnchor = self.addClass('ui-tab-active').find('a[href^="#"]').attr('href');
			if(targetAnchor){
				$('.ui-tabs-pane', tabsContext).hide();
				$(targetAnchor, tabsContext).show();
				// seems jQuery way doesnot work, will have to trigger it in hudson way
				// it's written in hudson-behavior.js#fireEvent and hudson-behavior.js#adjustSticker
				window.fireEvent(window, 'scroll');
			}
		});
	
		$('.ui-tab-active').click();
		
		$('#execPlan').jstree({plugins: ['checkbox', 'types'],
					// this combination with tie_selection set false is what ui expected
					checkbox: {/*keep_selected_style: false, */whole_node: false, tie_selection: false},
					types: {leaf: {icon: 'coordinator-icon coordinator-leaf'},
						'breaking-serial': {icon: 'coordinator-icon coordinator-breaking-serial'},
						'breaking-parallel': {icon: 'coordinator-icon coordinator-breaking-parallel'},
						'non-breaking-serial': {icon: 'coordinator-icon coordinator-non-breaking-serial'},
						'non-breaking-parallel': {icon: 'coordinator-icon coordinator-non-breaking-parallel'}}
					})
					.on('ready.jstree', function(){
						var jstreeInst = $.jstree.reference(this);
						// since prototype.js has polluted native JSON relevant methods, might as well do it here 
						jstreeInst.get_container().find('[data-jstree]').each(function(i, e){
							var state = jstreeInst.get_node(e).data.jstree;
							
							// seems type plugin is now reading data-jstree settings in TreeNode/config.jelly
							//jstreeInst.set_type(e, state.type);

							// this time unchecked is not working... 
							if($(e).hasClass('jstree-leaf') && !state.checked){
								jstreeInst.uncheck_node(e);
							}
						});
					});
		
		$('#bottom-sticker').on('click', '#buildTrigger', function(){
			var jstreeInst = $('#execPlan').jstree(true);
			var rootNode = jstreeInst.get_json(null, {no_data: true})[0];
			// from coordinator-utils.js
			optimized4NetworkTransmission(rootNode);
			patchUpTreeNode(jstreeInst, rootNode);
			var jsonString = Object.toJSON(rootNode);
			$('#execPlanJsonStrInput').val(jsonString);
			$('form[name="parameters"]').submit();
		})
		
//		doCheckXXX approach is not suitable for our case, since Http Get request got QueryParameter limited length (2k)
//		we may leave the node validation 
//		1. tree level, toJSONString validation and form submission
//		2. single node level, on jstree's Event on `create_node, set_text, rename_node, paste`.
//		
//		check if the node's text stands for an existing project in jenkins 
		
	});
})(jQuery.noConflict());