import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.JFrame;

public class Main {
	
	public static final int deltaHeight = 8;
	public static final int deltaWidth = 8;
	
	public static final float vx = 1.22875f;
	public static final float vy = 1.235f;
	
	public int imageWidth;
	public int imageHeight;
	
	public int frameWidth;
	public int frameHeight;
	
	public int calcDepth = 100;
	
	public double borderLeft = -2;
	public double borderRight = 2;
	public double borderBottom = -2;
	public double borderTop = 2;
	
	private BufferedImage[] framebuffer = new BufferedImage[2];
	private int state = 0;
	private LinkedList<State> stateStack = new LinkedList<State>();
	
	private JFrame frame;
	
	public static void main(String[] args) {
		new Main(900, 900);
	}
	
	public Main(int w, int h) {
		this.frameWidth = w;
		this.frameHeight = h;
		this.imageWidth = w - deltaWidth;
		this.imageHeight = h - deltaHeight;
		framebuffer[0] = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		framebuffer[1] = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		MandelBrot.setup(imageWidth, imageHeight);
		frame = new JFrame("Mandelbrot Set visuals");
		frame.setSize(frameWidth, frameHeight);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		frame.addMouseListener(mouseListener);
		frame.addMouseMotionListener(mouseMotionListener);
		frame.addKeyListener(keyListener);
		update();
		update();
	}
	
	BufferedImage getFramebuffer() {
		return framebuffer[state];
	}
	
	private void zoom(int fromx, int tox, int fromy, int toy) {
		double w = borderRight - borderLeft;
		double h = borderTop - borderBottom;

		double nBL = borderLeft + fromx*w/imageWidth;
		double nBR = borderLeft + tox*w/imageWidth;
		double nBB = borderBottom + fromy*h/imageHeight;
		double nBT = borderBottom + toy*h/imageHeight;
		
		stateStack.addFirst(new State(borderLeft, borderRight, borderTop, borderBottom));
		
		borderLeft = nBL;
		borderRight = nBR;
		borderBottom = nBB;
		borderTop = nBT;
		
		update();
		update();
	}
	
	private void update() {
		int[] pixels = new int[imageWidth*imageHeight];
		MandelBrot.calculate(pixels, borderLeft, borderRight, borderBottom, borderTop, calcDepth);
		frame.getGraphics().drawImage(framebuffer[state++], 0, 0, null);
		state %= 2;
		framebuffer[state].setRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);
	}
	
	public void popStateStack() {
		if (!stateStack.isEmpty()) {
			System.out.println("Reverting");
			State s = stateStack.removeFirst();
			this.borderBottom = s.borderBottom;
			this.borderTop = s.borderTop;
			this.borderLeft = s.borderLeft;
			this.borderRight = s.borderRight;
			update();
			update();
		}
	}
	
	
	boolean pressed = false;
	int mx = 0;
	int my = 0;
	
	KeyListener keyListener = new KeyListener() {
		
		@Override
		public void keyTyped(KeyEvent e) {
			
		}
		
		@Override
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_BACK_SPACE: popStateStack();
			}
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			
		}
	};
	
	MouseWheelListener mouseWheelListener = new MouseWheelListener() {
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			
		}
	};
	
	MouseListener mouseListener = new MouseListener() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (!pressed) {
				pressed = true;
				mx = e.getX();
				my = e.getY();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			pressed = false;
			
			int tx = e.getX();
			int ty = e.getY();
			
			int dx = tx - mx;
			int dy = ty - my;
			
			
			
			if (dx > 0 && dy > 0) {
				zoom(mx, tx, my, ty);
				System.out.printf("Zooming into (%d, %d) - (%d, %d)\n", mx, my, tx, ty);
			}
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
		
	};
	
	MouseMotionListener mouseMotionListener = new MouseMotionListener() {
		
		@Override
		public void mouseMoved(MouseEvent e) {
			
			
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if (pressed) {
				Graphics g = getFramebuffer().createGraphics();
				g.setColor(Color.RED);
				g.drawRect(mx, my, e.getX() - mx, e.getY() - my);
				update();
			}
		}
	};
	
	static class State {
		double borderLeft;
		double borderRight;
		double borderTop;
		double borderBottom;
		
		public State(double borderLeft, double borderRight, double borderTop, double borderBottom) {
			this.borderLeft = borderLeft;
			this.borderRight = borderRight;
			this.borderTop = borderTop;
			this.borderBottom = borderBottom;
		}
		
	}
}
