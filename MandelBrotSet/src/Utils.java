import static org.lwjgl.opencl.CL20.CL_DEVICE_TYPE_ALL;
import static org.lwjgl.opencl.CL20.CL_DEVICE_TYPE_GPU;
import static org.lwjgl.opencl.CL20.clGetDeviceIDs;
import static org.lwjgl.opencl.CL20.clGetPlatformIDs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

public class Utils {
	
	public static Platform[] getPlatforms() {
		
		int[] num_platforms = new int[1];
		PointerBuffer platforms = BufferUtils.createPointerBuffer(16);
		
		clGetPlatformIDs(platforms, num_platforms);
		
		if (num_platforms[0] > 16) { // there are too many platforms to fit in the list, so make the list bigger
			platforms = BufferUtils.createPointerBuffer(num_platforms[0]);
			clGetPlatformIDs(platforms, num_platforms);
		}
		
		Platform[] platformArray = new Platform[num_platforms[0]];
		
		for (int i = 0; i < num_platforms[0]; i++) {
			platformArray[i] = new Platform(platforms.get(i));
		}
		
		return platformArray;
		
	}
	
	public static Device[] getDevices(Platform platform) {
		
		int[] num_devices = new int[1];
		PointerBuffer devices = BufferUtils.createPointerBuffer(16);
		
		clGetDeviceIDs(platform.getID(), CL_DEVICE_TYPE_ALL, devices, num_devices);
		
		if (num_devices[0] > 16) { // too many devices so make list bigger
			devices = BufferUtils.createPointerBuffer(num_devices[0]);
			clGetDeviceIDs(platform.getID(), CL_DEVICE_TYPE_GPU, devices, num_devices);
		}
		
		Device[] deviceArray = new Device[num_devices[0]];
		
		for (int i = 0; i < num_devices[0]; i++) {
			deviceArray[i] = new Device(devices.get(i));
		}
		
		return deviceArray;
		
	}
	
	public static String loadText(String name) {
		if(!name.endsWith(".cls")) {
			name += ".cls";
		}
		BufferedReader br = null;
		String resultString = null;
		try {
			// Get the file containing the OpenCL kernel source code
			File clSourceFile = new File(name);
			// Create a buffered file reader to read the source file
			br = new BufferedReader(new FileReader(clSourceFile));
			// Read the file's source code line by line and store it in a string buffer
			String line = null;
			StringBuilder result = new StringBuilder();
			while((line = br.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}
			// Convert the string builder into a string containing the source code to return
			resultString = result.toString();
		} catch(NullPointerException npe) {
			// If there is an error finding the file
			System.err.println("Error retrieving OpenCL source file: ");
			npe.printStackTrace();
		} catch(IOException ioe) {
			// If there is an IO error while reading the file
			System.err.println("Error reading OpenCL source file: ");
			ioe.printStackTrace();
		} finally {
			// Finally clean up any open resources
			try {
				br.close();
			} catch (IOException ex) {
				// If there is an error closing the file after we are done with it
				System.err.println("Error closing OpenCL source file");
				ex.printStackTrace();
			}
		}

		// Return the string read from the OpenCL kernel source code file
		return resultString;
	}
	
}