kernel void sum(global int* pixels, double fromX, double toX, double fromY, double toY, int width, int height, int size, int depth) {
	const int itemId = get_global_id(0); 
	if (itemId < size) {
		
		quad a = fromX[itemId];
		
		//result[itemId] = a[itemId] + b[itemId];
		//result[itemId] = sqrt(result[itemId]);
	}
}