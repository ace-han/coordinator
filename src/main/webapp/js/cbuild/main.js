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
		jstreeTablization('#execPlan',
			{plugins: ['checkbox', 'types', 'decorators'],
				// this combination with tie_selection set false is what ui expected
			checkbox: {/*keep_selected_style: false, */whole_node: false, tie_selection: false},
			types: {leaf: {icon: 'coordinator-icon coordinator-leaf'},
				'breaking-serial': {icon: 'coordinator-icon coordinator-breaking-serial'},
				'breaking-parallel': {icon: 'coordinator-icon coordinator-breaking-parallel'},
				'non-breaking-serial': {icon: 'coordinator-icon coordinator-non-breaking-serial'},
				'non-breaking-parallel': {icon: 'coordinator-icon coordinator-non-breaking-parallel'}},
			decorators: {
				'.jstree-table-row': function(liContainer, targetElem){
						liContainer = $(liContainer);
						targetElem = $(targetElem);
						liContainer.prepend(targetElem);
				},

			}},	function(jstreeInst){
				uncheckBasedOnJobStatus(jstreeInst);
				startPollingBuildInfo(jstreeInst);
				// update the bottom build button position
				jstreeInst.element
					.on('after_open.jstree after_close.jstree', function(){
						window.fireEvent(document.getElementsByTagName('body')[0], 'scroll');
					});
			});


		function uncheckBasedOnJobStatus(jstreeInst, nodes){
			var keepCheckedRegex = /(\.gif|(red|aborted)\.png)$/;
			if(!nodes || !nodes.length){
				var nodes = jstreeInst.get_json(null, {no_data: true, no_state: true, flat: true});
			}
			var container = jstreeInst.get_container(); // using standard api instead of the one from decorator plugin
			$.each(nodes, function(i, node){
				if(node.type!=='leaf'){
					return true;
				}
				// var node = jstreeInst.get_node(node.id, true);
				// we need to uncheck_node based on original_container_html
				var imgSrcUri = container.find('#' + node.id).children('.jstree-table-row')
										.find('.jobStatus img').prop('src');
				if(imgSrcUri && !keepCheckedRegex.test(imgSrcUri)){
					jstreeInst.uncheck_node(node.id);
				}
			})
		}

		$('#bottom-sticker').on('click', '#buildTrigger', function(){
			$('span.apply-button').trigger('click');
		});

		// make the bottom-sticker appear at the appropriate place
		// since it need to wait on a little bit
		setTimeout(function(){
			window.fireEvent(document.getElementsByTagName('body')[0], 'scroll');
		});
		

		Event.observe($('form[name="parameters"]').get(0), "jenkins:apply", function(){
			var jstreeInst = $('#execPlan').jstree(true);
			var rootNode = jstreeInst.get_json(null, {no_data: true})[0];
			// from coordinator-utils.js
			optimized4NetworkTransmission(rootNode);
			patchUpTreeNode(jstreeInst, rootNode);
			var jsonString = Object.toJSON(rootNode);
			$('#execPlanJsonStrInput').val(jsonString);
		});
		
		// ref #14, Status of coordinator job should be synced with the job status
		(function pollBuildCaption(){
			$.get('buildCaptionHtml')
				.done(function( data, textStatus, jqXHR ) {
					$('.build-caption.page-headline').replaceWith(data);
				})
				.fail(function( jqXHR, textStatus, errorThrown){
					console.error(textStatus, errorThrown);
				})
				.always(function(){
					setTimeout(pollBuildCaption, 5000);
				})
		})();
		
		// ref #13, Atomic job building status bar does not end in the build table sometimes
		function startPollingBuildInfo(jstreeInst){
			$.get('pollActiveAtomicBuildsTableRowHtml')
				.done(function( data, textStatus, jqXHR ){
					var nodeId;
					for(nodeId in data){
						jstreeInst.update_redraw_template(nodeId, '.jstree-table-row', data[nodeId]);
						(function(nodeId){
							setTimeout(function(){
								// if need to uncheck the node
								uncheckBasedOnJobStatus(jstreeInst,
									jstreeInst.get_json(nodeId,
										{no_data: true, no_children: true, no_state: true, flat: true}));
							}, 500);
						})(nodeId);
					}
				})
				.fail(function( jqXHR, textStatus, errorThrown) {
					// popup an alert to stop the polling
					alert('Jenkins server encountered problems. Please check relevant server log.');
				})
				.always(function(){
					setTimeout(function(){
						startPollingBuildInfo(jstreeInst);
					}, 5000); // just follow jenkins itself polling interval
				})
		}
		
	});
})(jQuery.noConflict());