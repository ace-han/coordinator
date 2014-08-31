
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
	$(treeId).jstree({plugins: ['checkbox', 'types', 'wholerow'],
		// this combination with tie_selection set false is what ui expected
		checkbox: {/*keep_selected_style: false, */whole_node: false, tie_selection: false},
		types: {leaf: {icon: 'coordinator-icon coordinator-leaf'},
				serial: {icon: 'coordinator-icon coordinator-serial'},
				parallel: {icon: 'coordinator-icon coordinator-parallel'}},
		})
		.on('ready.jstree', function(){
			var jstreeInst = $.jstree.reference(this);
			// since prototype.js has polluted native JSON relevant methods, might as well do it here
			var container = jstreeInst.get_container(); 
			container.find('[data-jstree]').each(function(i, e){
				var node = jstreeInst.get_node(e, true);
				var state = node.data().jstree;
				jstreeInst.set_type(e, state.type);
				if(node.hasClass('jstree-leaf') && state.checked){
					jstreeInst.check_node(e);
				}
			});
			// patch up anti wholerow selection
			container.find('.jstree-wholerow-clicked').removeClass('jstree-wholerow-clicked');
		})
		.on('changed.jstree', function(e, data){
			// patch up anti wholerow selection
			var jstreeInst = $.jstree.reference(this);
			jstreeInst.get_container().find('.jstree-wholerow-clicked').removeClass('jstree-wholerow-clicked');
		});
}