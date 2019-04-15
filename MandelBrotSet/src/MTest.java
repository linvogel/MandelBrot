import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CL20;
import org.lwjgl.opencl.CLCapabilities;

public class MTest {
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		
		final boolean debug = true;
		final int size = 100_000_000; //Can be whatever size you want.
		
		
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
		
		long program = CL20.clCreateProgramWithSource(context, Utils.loadText("res/opencl/arraySum.cls"), null);
		int buildOutput = CL20.clBuildProgram(program, device, "", null, 0);
		
		if (debug) System.out.println("Context:      " + context);
		if (debug) System.out.println("CommandQueue: " + commandQueue);
		if (debug) System.out.println("Program:      " + program);

		if (debug) System.out.println(buildOutput == 0 ? "Building successful" : "Build error");
		long kernel = CL10.clCreateKernel(program, "sum", errcode);
		

		// Create float array from 0 to size-1.
		FloatBuffer aBuff = BufferUtils.createFloatBuffer(size);
		float[] tempDataA = new float[size];
		float[] tempDataB = new float[size];
		for(int i = 0; i < size; i++) {
		    tempDataA[i] = i;
		    tempDataB[i] = i;
		}
		aBuff.put(tempDataA);
		aBuff.rewind();
		// Create float array from size-1 to 0. This means that the result should be size-1 for each element.
		FloatBuffer bBuff = BufferUtils.createFloatBuffer(size);
		bBuff.put(tempDataB);
		bBuff.rewind();
		
		FloatBuffer res = BufferUtils.createFloatBuffer(size);
		float[] result = new float[size];
		
		long memA = CL20.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE, tempDataA, null);
		long memB = CL20.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE, tempDataB, null);
		long memR = CL20.clCreateBuffer(context, CL10.CL_MEM_READ_WRITE, res, null);

		PointerBuffer ptrA = BufferUtils.createPointerBuffer(1);
		PointerBuffer ptrB = BufferUtils.createPointerBuffer(1);
		PointerBuffer ptrR = BufferUtils.createPointerBuffer(1);
		IntBuffer ptrS = BufferUtils.createIntBuffer(1);

		ptrA.put(memA);
		ptrA.rewind();
		ptrB.put(memB);
		ptrB.rewind();
		ptrR.put(memR);
		ptrR.rewind();
		ptrS.put(size);
		ptrS.rewind();

		CL20.clSetKernelArg(kernel, 0, 4);
		int errArg1 = CL20.clSetKernelArg(kernel, 0, ptrA);
		CL20.clSetKernelArg(kernel, 1, 4);
		int errArg2 = CL20.clSetKernelArg(kernel, 1, ptrB);
		CL20.clSetKernelArg(kernel, 2, 4);
		int errArg3 = CL20.clSetKernelArg(kernel, 2, ptrR);
		CL20.clSetKernelArg(kernel, 3, 4);
		int errArg4 = CL20.clSetKernelArg(kernel, 3, ptrS);

		if (debug) System.out.println("Arg 1 Error: " + errArg1);
		if (debug) System.out.println("Arg 2 Error: " + errArg2);
		if (debug) System.out.println("Arg 3 Error: " + errArg3);
		if (debug) System.out.println("Arg 4 Error: " + errArg4);
		
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, size);
		
		
		int error = CL10.clEnqueueWriteBuffer(commandQueue, memA, true, 0, tempDataA, null, null);
		if (debug) System.out.println("Write error A: " + error);
		error = CL10.clEnqueueWriteBuffer(commandQueue, memB, true, 0, tempDataB, null, null);
		if (debug) System.out.println("Write error A: " + error);
		
		
		
		// This is the start of the actual computation
		
		long start = System.currentTimeMillis();
		
		
		error = CL20.clEnqueueNDRangeKernel(commandQueue, kernel, dimensions, null, globalWorkSize, null, null, null);

		if (debug) System.out.println("Enqueue Error: " + error);
		
		error = CL20.clFinish(commandQueue);
		if (debug) System.out.println("Finish Error: " + error);
		
		
		// This is the end of the calculation!
		
		long stop = System.currentTimeMillis();
		
		res.rewind();
		error = CL20.clEnqueueReadBuffer(commandQueue, memR, true, 0, res, null, null);
		if (debug) System.out.println("Read Result Error: " + error);
		res.get(result);
		
		aBuff.rewind();
		error = CL20.clEnqueueReadBuffer(commandQueue, memA, true, 0, aBuff, null, null);
		if (debug) System.out.println("Read A Error: " + error);
		aBuff.get(tempDataA);

		bBuff.rewind();
		error = CL20.clEnqueueReadBuffer(commandQueue, memB, true, 0, bBuff, null, null);
		if (debug) System.out.println("Read B Error: " + error);
		bBuff.get(tempDataB);
		
		
		
		//for (float f : tempDataA) System.out.print(f + "\t"); System.out.println();
		//for (float f : tempDataB) System.out.print(f + "\t"); System.out.println();
		//for (float f : result) System.out.print(f + "\t"); System.out.println();
		
		for (int i = 0; i < 10; i++) System.out.print(result[i] + "\t"); System.out.println();
		
		System.out.println("Terminated in " + (stop - start) + "ms (On GPU accelerated system)");
		
		start = System.currentTimeMillis();
		
		for (int i = 0; i < size; i++) {
			result[i] = tempDataA[i] + tempDataB[i];
			result[i] = (float) Math.sqrt(result[i]);
		}

		stop = System.currentTimeMillis();
		

		for (int i = 0; i < 10; i++) System.out.print(result[i] + "\t"); System.out.println();
		

		System.out.println("Terminated in " + (stop - start) + "ms (On CPU)");
		
	}
	
}