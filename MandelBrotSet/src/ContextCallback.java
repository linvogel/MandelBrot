import org.lwjgl.opencl.CLContextCallback;

public class ContextCallback extends CLContextCallback {

	@Override
	public void invoke(long errinfo, long private_info, long cb, long user_data) {
		System.out.println(errinfo);
	}

}