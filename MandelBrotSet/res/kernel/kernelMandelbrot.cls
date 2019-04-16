kernel void mandelbrot(global int* pixels, double fromX, double toX, double fromY, double toY, int width, int height, int size, int depth) {
	const int itemId = get_global_id(0); 
	if (itemId < size) {
		int x = itemId % width;
		int y = itemId / width;
		
		double stepSizeX = (toX - fromX) / width;
		double stepSizeY = (toY - fromY) / height;
		
		double real = fromX + (x * stepSizeX);
		double imag = fromY + (y * stepSizeY);
		
		double stateReal = 0;
		double stateImag = 0;
		
		int value = -1;
		for (int i = 0; i < depth; i++) {
			double a = stateReal;
			double b = stateImag;
			stateReal = a*a - b*b + real;
			stateImag = 2*a*b + imag;
			if (stateReal*stateReal + stateImag*stateImag >= 4) {
				value = i;
				break;
			}
		}
		
		//value = (int) (((float)(value+1)/(float)depth)*255);
		value %= 16;
		value *= 16;
		
		pixels[itemId] = (255 << 24) | (value << 16) | (value << 8) | value;
		
	}
}