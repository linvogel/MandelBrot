import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL20;

public class MandelBrot {
	
	private static int width;
	private static int height;
	
	private static long context;
	private static long commandQueue;
	private static long program;
	private static long kernel;

	public static MathContext mc = new MathContext(256);
	
	public static void setup(int width, int height) {
		
		MandelBrot.width = width;
		MandelBrot.height = height;
		
		Platform[] platforms = Utils.getPlatforms();
		Platform gpu = platforms[0];
		ContextCallback callback = new ContextCallback();
		
		for (Platform p : platforms) if (p.getName().toLowerCase().contains("nvidia")) gpu = p;
		long device = gpu.getDevices()[0].getID();
		
		ByteBuffer byteBuffer = BufferUtils.createByteBuffer(8);
		CL10.clGetDeviceInfo(device, CL10.CL_DEVICE_MAX_MEM_ALLOC_SIZE, byteBuffer, null);
		
		PointerBuffer contextProperties = BufferUtils.createPointerBuffer(16);
		contextProperties.put(CL20.CL_CONTEXT_PLATFORM);
		context = CL20.clCreateContext(contextProperties, gpu.getDevices()[0].getID(), callback, 0, null);

		int[] errcode = new int[16];
		
		commandQueue = CL20.clCreateCommandQueue(context, device, CL20.CL_QUEUE_OUT_OF_ORDER_EXEC_MODE_ENABLE, errcode);
		
		program = CL20.clCreateProgramWithSource(context, Utils.loadText("res/kernel/kernelMandelbrot.cls"), null);
		int buildOutput = CL20.clBuildProgram(program, device, "", null, 0);
		
		System.out.println(context);
		System.out.println(commandQueue);
		System.out.println(program);

		System.out.println(buildOutput == 0 ? "Building successful" : "Build error");
		kernel = CL10.clCreateKernel(program, "mandelbrot", errcode);
	}
	
	public static int[] calculate(int[] pixels, double borderLeft, double borderRight, double borderBottom, double borderTop, int calcDepth) {
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
		
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, width*height);
		
		int error = CL20.clEnqueueNDRangeKernel(commandQueue, kernel, dimensions, null, globalWorkSize, null, null, null);
		error = CL20.clFinish(commandQueue);
		
		buffer.rewind();
		error = CL20.clEnqueueReadBuffer(commandQueue, mem, true, 0, buffer, null, null);
		buffer.get(pixels);
		
		return pixels;
	}
	
	public static int[] calculateBig(int[] pixels, BigDecimal left, BigDecimal right, BigDecimal bottom, BigDecimal top, int calcDepth, int width, int height) {
		
		ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
		LinkedList<Future> futures = new LinkedList<Future>();
		
		for (int y = 0; y < height; y++) {
			futures.add(service.submit(new LineCalculatorCallable(pixels, y, width, height, left, right, bottom, top, calcDepth)));
		}
		
		while (!futures.isEmpty()) {
			try {
				futures.removeFirst().get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Picture Rendered...");
		
		return pixels;
	}
}

class LineCalculatorCallable implements Callable<Void> {
	
	BigDecimal left, right, top, bottom;
	int calcDepth;
	int width, height, y;
	int[] pixels;
	
	
	public LineCalculatorCallable(int[] pixels, int y, int width, int height, BigDecimal left, BigDecimal right, BigDecimal bottom, BigDecimal top, int calcDepth) {
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		this.calcDepth = calcDepth;
		this.width = width;
		this.height = height;
		this.y = y;
		this.pixels = pixels;
	}
	
	@Override
	public Void call() throws Exception {

		BigDecimal stepX = right.subtract(left).divide(BigDecimal.valueOf(width), MandelBrot.mc);
		BigDecimal stepY = top.subtract(bottom).divide(BigDecimal.valueOf(height), MandelBrot.mc);
		
		BigDecimal x0 = left;
		BigDecimal y0 = bottom.add(stepY.multiply(BigDecimal.valueOf(y), MandelBrot.mc));
		
		
		for (int x = 0; x < width; x++) {
			BigComplexNumber state = new BigComplexNumber();
			BigComplexNumber number = new BigComplexNumber(x0, y0);
			
			int value = -1;
			
			for (int i = 0; i < calcDepth; i++) {
				state = state.multiply(state).add(number);
				
				if (state.absSq().compareTo(BigDecimal.valueOf(4)) > 0) {
					value = i;
					break;
				}
			}
			
			value = map(value, -1, calcDepth, 0, 255);
			
			pixels[y*width + x] = (0xFF << 24) | (value << 16) | (value << 8) | value;
			
			x0 = x0.add(stepX, MandelBrot.mc);
		}
		System.out.println("Progress: " + y + "/" + height);
		return null;
	}
	
	static int map(float value, float vstart, float vstop, float ostart, float ostop) {
		return Math.round((value-vstart)/(vstop-vstart)*(ostop-ostart)+ostart);
	}
	
}
