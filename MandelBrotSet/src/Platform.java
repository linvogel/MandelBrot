import static org.lwjgl.opencl.CL10.CL_PLATFORM_NAME;
import static org.lwjgl.opencl.CL10.clGetPlatformInfo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class Platform {
	
	private long id;
	private String name;
	
	private Device[] devices;
	
	public Platform(long id) {
		this.id= id;
		ByteBuffer nameBuffer = BufferUtils.createByteBuffer(256);
		clGetPlatformInfo(this.id, CL_PLATFORM_NAME, nameBuffer, BufferUtils.createPointerBuffer(1).put(256).flip());
		byte[] name = new byte[256];
		nameBuffer.get(name);
		try {
			this.name = new String(name, "ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		this.devices = Utils.getDevices(this);
	}
	
	public long getID() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Device[] getDevices() {
		return this.devices;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Platform: \n\tName: " + this.name + "\n\tID: " + this.id + "\n\tDevices: " + this.devices.length);
		return builder.toString();
	}
}