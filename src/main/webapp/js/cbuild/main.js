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
					serial: {icon: 'coordinator-icon coordinator-serial'},
					parallel: {icon: 'coordinator-icon coordinator-parallel'}},
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

		var needEmptyJobStatusDoubleCheck = true;

		function startPollingBuildInfo(jstreeInst, pendingMap){
			pendingMap = pendingMap || {};
			var nodeId;
			$.get('pollActiveAtomicBuildStatus')
				.done(function( data, textStatus, jqXHR ) {
					data = data || {};
					var isEmptyResult = $.isEmptyObject(data)
						, toBeFinishedList = [];

					// prepare pendingMap
					if(!$.isEmptyObject(pendingMap)){
						// filter those considered should be finished
						for(nodeId in pendingMap){
							if(!(nodeId in data)){
								toBeFinishedList.push(nodeId);
							}
						}
						needEmptyJobStatusDoubleCheck = true;
					}

					if(isEmptyResult){
						if(needEmptyJobStatusDoubleCheck){
							// see if any job finishes way too quick to update its final status
							$.each(jstreeInst.get_bottom_checked(), function(i, nodeId){
								if( jstreeInst.is_disabled(nodeId) && toBeFinishedList.indexOf(nodeId)!=-1 ){
									// since get_bottom_checked method does not filter disabled ones
									return true;
								}
								toBeFinishedList.push(nodeId);
							});
							needEmptyJobStatusDoubleCheck = false;
						}
					}

					pendingMap = data;

					for(nodeId in pendingMap){
						jstreeInst.update_redraw_template(nodeId, '.jstree-table-row', pendingMap[nodeId]);
					}

					// handle the toBeFinishedList for the very last time
					$.each(toBeFinishedList, function(i, nodeId){
						// extract the job name and build number from original_container_html
						var liContainer = jstreeInst.get_container().find('#'+nodeId); // using standard api instead of the one from decorator plugin
						var jobName = liContainer.children('.model-link').text();
						var buildNumber = liContainer.find('.buildNumberLink').text();

						// trigger an ajax call to retrieve the generated tableRow.jelly
						(function(nodeId, jobName, buildNumber){
							$.get('atomicBuildResultTableRowHtml',
								{nodeId: nodeId
								, jobName: jobName
								, buildNumber: buildNumber
								})
								.done(function( data, textStatus, jqXHR ) {
									jstreeInst.update_redraw_template(nodeId, '.jstree-table-row', data);
									setTimeout(function(){
										// if need to uncheck the node
										uncheckBasedOnJobStatus(jstreeInst,
											jstreeInst.get_json(nodeId,
												{no_data: true, no_children: true, no_state: true, flat: true}));
										}, 500);
								})
								.fail(function( jqXHR, textStatus, errorThrown){
    								jstreeInst.update_redraw_template(nodeId, '.jstree-table-row',
    									'<div class="jstree-wholerow jstree-table-row" style="background-color:#ffebeb;">'
    									+ '<div class="jstree-table-col jobStatus">&nbsp;</div>' // for padding the space
    									+ '<div class="jstree-table-col lastDuration">Network error: '
    									+ jqXHR.status + '. Please refresh the page.</div></div>')
								});
						})(nodeId, jobName, buildNumber);
					})
					var buildTriggerBtn = YAHOO.widget.Button.getButton('buildTrigger');
					if(buildTriggerBtn){
						buildTriggerBtn.set('disabled', !isEmptyResult);
					}

				})
				.fail(function( jqXHR, textStatus, errorThrown) {
					// popup an alert to stop the polling
					alert('Jenkins server encountered problems. Please check relevant server log.');
				})
				.always(function(){
					setTimeout(function(){
						startPollingBuildInfo(jstreeInst, pendingMap);
					}, 5000); // just follow jenkins itself polling interval
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
		
	});
})(jQuery.noConflict());