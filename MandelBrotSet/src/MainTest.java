import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL20;
import org.lwjgl.opencl.CLCapabilities;

public class MainTest {
	
	static int calcDepth = 255;
	static int width = 2560;
	static int height = 2560;

	static double borderLeft = -2;
	static double borderRight = 2;
	static double borderBottom = -2;
	static double borderTop = 2;

	static double stepX = 2.0d / width;
	static double stepY = 2.0d / height;
	
	public static void main(String[] args) throws Exception {
		System.out.println("Arguments: " + args.length);
		System.out.print("	"); for (String s : args) System.out.print(s + " "); System.out.println();
		if (args.length > 0 && args[1].equals("testKernel")) {
			testKernel();
			System.exit(0);
		}
		
		boolean debug = true;
		int[] pixels = new int[width*height];
		
		Platform[] platforms = Utils.getPlatforms();
		Platform gpu = null;
		ContextCallback callback = new ContextCallback();
		
		for (Platform p : platforms) if (p.getName().toLowerCase().contains("nvidia")) gpu = p;
		long device = gpu.getDevices()[0].getID();
		
		ByteBuffer byteBuffer = BufferUtils.createByteBuffer(8);
		CL10.clGetDeviceInfo(device, CL10.CL_DEVICE_MAX_MEM_ALLOC_SIZE, byteBuffer, null);
		if (debug) System.out.println(byteBuffer.asLongBuffer().get(0));
		
		CLCapabilities platformCapabilities = CL.createPlatformCapabilities(gpu.getID());
		CLCapabilities deviceCapabilities = CL.createDeviceCapabilities(gpu.getDevices()[0].getID(), platformCapabilities);
		
		PointerBuffer contextProperties = BufferUtils.createPointerBuffer(16);
		contextProperties.put(CL20.CL_CONTEXT_PLATFORM);
		long context = CL20.clCreateContext(contextProperties, gpu.getDevices()[0].getID(), callback, 0, null);

		int[] errcode = new int[16];
		
		long commandQueue = CL20.clCreateCommandQueue(context, device, CL20.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE, errcode);
		
		long program = CL20.clCreateProgramWithSource(context, Utils.loadText("res/kernel/kernelMandelbrot.cls"), null);
		int buildOutput = CL20.clBuildProgram(program, device, "", null, 0);
		
		if (debug) System.out.println(context);
		if (debug) System.out.println(commandQueue);
		if (debug) System.out.println(program);

		if (debug) System.out.println(buildOutput == 0 ? "Building successful" : "Build error");
		long kernel = CL10.clCreateKernel(program, "mandelbrot", errcode);
		
		
		//initialize the data to be worked on
		IntBuffer buffer = BufferUtils.createIntBuffer(width*height);
		
		long mem = CL20.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE, buffer, null);
		
		PointerBuffer ptrR = BufferUtils.createPointerBuffer(1);
		DoubleBuffer fromX = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer toX = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer fromY = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer toY = BufferUtils.createDoubleBuffer(1);
		IntBuffer ptrW = BufferUtils.createIntBuffer(1);
		IntBuffer ptrH = BufferUtils.createIntBuffer(1);
		IntBuffer ptrS = BufferUtils.createIntBuffer(1);
		IntBuffer ptrD = BufferUtils.createIntBuffer(1);

		ptrR.put(mem);
		ptrR.rewind();
		fromX.put(borderLeft);
		fromX.rewind();
		toX.put(borderRight);
		toX.rewind();
		fromY.put(borderBottom);
		fromY.rewind();
		toY.put(borderTop);
		toY.rewind();
		ptrW.put(width);
		ptrW.rewind();
		ptrH.put(height);
		ptrH.rewind();
		ptrS.put(width*height);
		ptrS.rewind();
		ptrD.put(calcDepth);
		ptrD.rewind();

		CL20.clSetKernelArg(kernel, 0, 4);
		int errArg1 = CL10.clSetKernelArg(kernel, 0, ptrR);
		CL20.clSetKernelArg(kernel, 1, 4);
		int errArg2 = CL10.clSetKernelArg(kernel, 1, fromX);
		CL20.clSetKernelArg(kernel, 2, 4);
		int errArg3 = CL10.clSetKernelArg(kernel, 2, toX);
		CL20.clSetKernelArg(kernel, 3, 4);
		int errArg4 = CL10.clSetKernelArg(kernel, 3, fromY);
		CL20.clSetKernelArg(kernel, 4, 4);
		int errArg5 = CL10.clSetKernelArg(kernel, 4, toY);
		CL20.clSetKernelArg(kernel, 5, 4);
		int errArg6 = CL20.clSetKernelArg(kernel, 5, ptrW);
		CL20.clSetKernelArg(kernel, 6, 4);
		int errArg7 = CL20.clSetKernelArg(kernel, 6, ptrH);
		CL20.clSetKernelArg(kernel, 7, 4);
		int errArg8 = CL20.clSetKernelArg(kernel, 7, ptrS);
		CL20.clSetKernelArg(kernel, 8, 4);
		int errArg9 = CL20.clSetKernelArg(kernel, 8, ptrD);

		if (debug) System.out.println("Arg 1 Error: " + errArg1);
		if (debug) System.out.println("Arg 2 Error: " + errArg2);
		if (debug) System.out.println("Arg 3 Error: " + errArg3);
		if (debug) System.out.println("Arg 4 Error: " + errArg4);
		if (debug) System.out.println("Arg 5 Error: " + errArg5);
		if (debug) System.out.println("Arg 6 Error: " + errArg6);
		if (debug) System.out.println("Arg 7 Error: " + errArg7);
		if (debug) System.out.println("Arg 8 Error: " + errArg8);
		if (debug) System.out.println("Arg 9 Error: " + errArg9);
		
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, width*height);
		
		
		// This is the start of the calculation
		
		long start = System.currentTimeMillis();
		
		int error = CL20.clEnqueueNDRangeKernel(commandQueue, kernel, dimensions, null, globalWorkSize, null, null, null);

		if (debug) System.out.println("Enqueue Error: " + error);
		
		error = CL20.clFinish(commandQueue);
		if (debug) System.out.println("Finish Error: " + error);
		
		
		// This is the end of the calculation!
		
		long stop = System.currentTimeMillis();
		
		buffer.rewind();
		error = CL20.clEnqueueReadBuffer(commandQueue, mem, true, 0, buffer, null, null);
		if (debug) System.out.println("Read Result Error: " + error);
		buffer.get(pixels);
		
		System.out.printf("The calculation took %dms\n", stop-start);
		
		
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		img.setRGB(0, 0, width, height, pixels, 0, width);
		
		System.out.println("Writing output...");
		ImageIO.write(img, "png", new File("out.png"));
		System.out.println("Done.");
	}
	
	public static void testKernel() throws IOException {
		int[] pixels = new int[width*height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				double stepSizeX = (borderRight - borderLeft) / width;
				double stepSizeY = (borderBottom - borderTop) / height;
				
				double real = borderLeft + (x * stepSizeX);
				double imag = borderTop + (y * stepSizeY);
				
				double stateReal = 0;
				double stateImag = 0;
				
				int value = -1;
				for (int i = 0; i < calcDepth; i++) {
					double a = stateReal;
					double b = stateImag;
					stateReal = a*a - b*b + real;
					stateImag = 2*a*b + imag;
					if (stateReal*stateReal + stateImag*stateImag >= 4) {
						value = i;
						break;
					}
				}
				//System.out.println(value);
				value = (int) (((float)(value+1)/(float)calcDepth)*255);
				
				pixels[y*width + x] = (255 << 24) | (value << 16) | (value << 8) | value;
			}
			System.out.printf("Progress: %.2f%%\n\0", (float) x / width * 100);
		}
		
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		img.setRGB(0, 0, width, height, pixels, 0, width);
		
		System.out.println("Writing output...");
		ImageIO.write(img, "png", new File("out.png"));
		System.out.println("Done.");
	}
	
	static int check(ComplexNumber number) {
		ComplexNumber state = new ComplexNumber();
		for (int i = 0; i < calcDepth; i++) {
			state = state.multiply(state).add(number);
			if (state.absSq() > 4) return i;
		}
		return -1;
	}
	
	static int map(float value, float vstart, float vstop, float ostart, float ostop) {
		return Math.round((value-vstart)/(vstop-vstart)*(ostop-ostart)+ostart);
	}
	
}
