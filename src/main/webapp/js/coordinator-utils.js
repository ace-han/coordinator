
function optimized4NetworkTransmission(node){
	delete node.li_attr;
	delete node.a_attr;
	delete node.data;
	for(var i=0; i<node.children.length; i++){
		optimized4NetworkTransmission(node.children[i]);
	}
}


function jstreeTablization(treeId, $){
	if(treeId.indexOf('#') == -1){
		treeId = '#' + treeId;
	}
	if(!$){
		$ = jQuery;
	}
	$(treeId).jstree({plugins: ['checkbox', 'types'],
		// this combination with tie_selection set false is what ui expected
		checkbox: {/*keep_selected_style: false, */whole_node: false, tie_selection: false},
		types: {leaf: {icon: 'coordinator-icon coordinator-leaf'},
				serial: {icon: 'coordinator-icon coordinator-serial'},
				parallel: {icon: 'coordinator-icon coordinator-parallel'}}
		})
		.on('ready.jstree', function(){
			var jstreeInst = $.jstree.reference(this);
			// since prototype.js has polluted native JSON relevant methods, might as well do it here
			var container = jstreeInst.get_container();
			var cols = ['<div class="jstree-table-col jobStatus">N/A</div>',
			              	'<div class="jstree-table-col lastDuration">N/A</div>',
			              	'<div class="jstree-table-col lastLaunch">N/A</div>',
			              	'<div class="jstree-table-col buildNum">#</div>',
			              	'<div class="clear"></div>'];
			//var maxOffsetRight = 0;
			container.children('.jstree-container-ul').addClass('jstree-wholerow-container jstree-no-dots')
			container.find('[data-jstree]').each(function(i, e){
				var node = jstreeInst.get_node(e, true);
				var state = node.data().jstree;
				var anchor = node.children('a.jstree-anchor');//, offsetRight;
				
				jstreeInst.set_type(e, state.type);
				node.prepend('<div class="jstree-wholerow ' 
						+ (i%2 === 1? 'jstree-table-row-odd': 'jstree-table-row-even') 
						+'"></div>')
				if(jstreeInst.is_leaf(e)){
					if(state.checked){
						jstreeInst.check_node(e);
					}		
					node.children('.jstree-wholerow').html(cols.join(''));
					/** 
					 * we will consider mobile first design in the future
					 * just keep rolling out this plugin first
					 * TODO mobile first design
					offsetRight = anchor.offset().left + anchor.outerWidth();
					if(offsetRight > maxOffsetRight){
						maxOffsetRight = offsetRight;
					}
					*/
				} else if (jstreeInst.is_parent(e)) {
					anchor.addClass('jstree-parent-node-text');
				}
			})
			// patch up anti wholerow selection
			container.find('.jstree-wholerow-clicked').removeClass('jstree-wholerow-clicked');
			
			// add header
			var headers = ['<div class="jstree-table-header-cont">',
			               '<div class="jstree-table-header jobStatus">Status</div>',
			              	'<div class="jstree-table-header lastDuration">Last Duration</div>',
			              	'<div class="jstree-table-header lastLaunch">Last Launch</div>',
			              	'<div class="jstree-table-header buildNum">Build #</div>',
			              	'<div class="clear"></div>',
			              '</div>'];
			container.prepend(headers.join(''))
		});
}