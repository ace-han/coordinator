
function optimized4NetworkTransmission(node){
	delete node.li_attr;
	delete node.a_attr;
	delete node.data;
	for(var i=0; i<node.children.length; i++){
		optimized4NetworkTransmission(node.children[i]);
	}
}