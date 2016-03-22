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
				window.fireEvent(document.getElementsByTagName('body')[0], 'scroll');
			}
		});
	
		$('.ui-tab-active').click();
		
		// from coordinator-utils.js
	jstreeTablization('.execPlan', 
		{plugins: ['checkbox', 'types', 'decorators'],
			// this combination with tie_selection set false is what ui expected
		checkbox: {/*keep_selected_style: false, */whole_node: false, tie_selection: false},
		types: {leaf: {icon: 'coordinator-icon coordinator-leaf'},
				serial: {icon: 'coordinator-icon coordinator-serial'},
				parallel: {icon: 'coordinator-icon coordinator-parallel'}},
		decorators: {
			'.jstree-table-row': function(liContainer, targetElem){
					liContainer = $(liContainer);
					targetElem = $(targetElem);
					liContainer.prepend(targetElem);
			},

		}});
		// for the sake of simplicity, I would like to do this by override parameters
		$('.task-link[href$="parameters"]').parent('.task').remove();
	});
})(jQuery.noConflict());