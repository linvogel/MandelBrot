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

	public static final int maxDepth = 2500;
	public static final int minDepth = 200;
	
	public static final float vx = 1.22875f;
	public static final float vy = 1.235f;
	
	public static final float animationSteps = 60;
	
	public final float ASPECT_RATIO;
	
	public final int imageWidth;
	public final int imageHeight;
	
	public final int frameWidth;
	public final int frameHeight;
	
	public int calcDepth = 2000;
	
	public double borderLeft = -2;
	public double borderRight = 2;
	public double borderBottom = -2;
	public double borderTop = 2;
	
	private BufferedImage[] framebuffer = new BufferedImage[2];
	private BufferedImage currentRendered;
	private int state = 0;
	private LinkedList<State> stateStack = new LinkedList<State>();
	
	private JFrame frame;
	
	public static void main(String[] args) {
		new Main(1024, 768);
	}
	
	public Main(int w, int h) {
		this.frameWidth = w;
		this.frameHeight = h;
		this.imageWidth = w - deltaWidth;
		this.imageHeight = h - deltaHeight;
		this.ASPECT_RATIO = (float) imageWidth / (float) imageHeight;
		framebuffer[0] = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		framebuffer[1] = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		currentRendered = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		MandelBrot.setup(imageWidth, imageHeight);
		frame = new JFrame("Mandelbrot Set visuals");
		frame.setSize(frameWidth, frameHeight);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		frame.addMouseListener(mouseListener);
		frame.addMouseMotionListener(mouseMotionListener);
		frame.addMouseWheelListener(mouseWheelListener);
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
		
		double BL = borderLeft + fromx*w/imageWidth;
		double BR = borderLeft + tox*w/imageWidth;
		double BB = borderBottom + fromy*h/imageHeight;
		double BT = borderBottom + toy*h/imageHeight;

		stateStack.addFirst(new State(borderLeft, borderRight, borderTop, borderBottom, calcDepth));
		
		double stepX1 = (BL - borderLeft) / animationSteps;
		double stepX2 = (borderRight - BR) / animationSteps;
		double stepY1 = (BB - borderBottom) / animationSteps;
		double stepY2 = (borderTop - BT) / animationSteps;
		
		for (int i = 1; i <= animationSteps; i++) {
	
			double nBL = borderLeft + stepX1;
			double nBR = borderRight - stepX2;
			double nBB = borderBottom + stepY1;
			double nBT = borderTop - stepY2;
			
	
			double m = Math.min(nBR - nBL, nBT - nBB);
			double r = Math.min(w, h);
			
			float f = (float) (r / m);
			calcDepth = Math.round(calcDepth * (float) Math.log(f));
	
			if (calcDepth > maxDepth) calcDepth = maxDepth;
			if (calcDepth < minDepth) calcDepth = minDepth;
			
			System.out.println("New Calculation depth: " + calcDepth);
			System.out.println("Smaller Length: " + m);
			
			borderLeft = nBL;
			borderRight = nBR;
			borderBottom = nBB;
			borderTop = nBT;
			
			long time = update();
			swap();
		}
	}
	
	private long update() {
		long start = System.currentTimeMillis();
		int[] pixels = new int[imageWidth*imageHeight];
		MandelBrot.calculate(pixels, borderLeft, borderRight, borderBottom, borderTop, calcDepth);
		currentRendered.setRGB(0, 0, imageWidth, imageHeight, pixels, 0, imageWidth);
		swap();
		return System.currentTimeMillis() - start;
	}
	
	private void swap() {
		frame.getGraphics().drawImage(getFramebuffer(), 0, 0, null);
		state = (state + 1) % 2;
		Graphics g = getFramebuffer().createGraphics();
		g.drawImage(currentRendered, 0, 0, null);
		g.dispose();
	}
	
	public void popStateStack() {
		if (!stateStack.isEmpty()) {
			System.out.println("Reverting");
			State s = stateStack.removeFirst();
			this.borderBottom = s.borderBottom;
			this.borderTop = s.borderTop;
			this.borderLeft = s.borderLeft;
			this.borderRight = s.borderRight;
			this.calcDepth = s.depth;

			System.out.println("New Calculation Depth: " + calcDepth);
			System.out.println("Smaller Length: " + Math.min(borderTop-borderBottom, borderRight-borderLeft));
			update();
			swap();
		}
	}
	
	public void scrollingPreview() {
		
		float d = (float) scrolled / 50f;
		int x1 = Math.round(mx * d);
		int y1 = Math.round(my * d);
		int x2 = Math.round(x1 + (imageWidth * (1-d)));
		int y2 = Math.round(y1 + (imageHeight * (1-d)));
		
		Graphics g = getFramebuffer().createGraphics();
		g.setColor(Color.RED);
		g.drawImage(currentRendered, 0, 0, null);
		g.drawRect(x1, y1, x2-x1, y2-y1);
		g.dispose();
		
		swap();
	}
	
	public void scrollZoom() {
		float d = (float) scrolled / 50f;
		int x1 = Math.round(mx * d);
		int y1 = Math.round(my * d);
		int x2 = Math.round(x1 + (imageWidth * (1-d)));
		int y2 = Math.round(y1 + (imageHeight * (1-d)));
		
		zoom(x1, x2, y1, y2);
		swap();
	}
	
	public void increaseDepth() {
		stateStack.addFirst(new State(borderLeft, borderRight, borderTop, borderBottom, calcDepth));
		calcDepth *= 1.1;
		if (calcDepth > maxDepth) calcDepth = maxDepth;
		System.out.println("New Calculation Depth: " + calcDepth);
		update();
		swap();
	}
	
	public void decreaseDepth() {
		stateStack.addFirst(new State(borderLeft, borderRight, borderTop, borderBottom, calcDepth));
		calcDepth *= 1.0/1.1;
		if (calcDepth < minDepth) calcDepth = minDepth;
		System.out.println("New Calculation Depth: " + calcDepth);
		update();
		swap();
	}
	
	public void reset() {
		
		float steps = 5*animationSteps;

		double ax = Math.pow(4.0 / (borderRight - borderLeft), 1.0/steps);
		double ay = Math.pow(4.0 / (borderTop - borderBottom), 1.0/steps);

		stateStack.addFirst(new State(borderLeft, borderRight, borderTop, borderBottom, calcDepth));
		
		for (int i = 1; i <= steps; i++) {

			double w = borderRight - borderLeft;
			double h = borderTop - borderBottom;
			
			double rx1 = (borderLeft + 2) / (4 - w);
			double rx2 = (2 - borderRight) / (4 - w);
			double ry1 = (borderBottom + 2) / (4 - h);
			double ry2 = (2 - borderTop) / (4 - h);

			double sx1 = rx1 * w * (1-ax);
			double sx2 = rx2 * w * (1-ax);
			double sy1 = ry1 * h * (1-ay);
			double sy2 = ry2 * h * (1-ay);
			
			borderLeft += sx1;
			borderRight -= sx2;
			borderBottom += sy1;
			borderTop -= sy2;
			
			double m = Math.min(borderRight - borderLeft, borderTop - borderTop);
			//double r = Math.min(w, h);
			//
			//float f = (float) (r / m);
			//calcDepth = Math.round(calcDepth * (float) Math.log(f));
			//
			//if (calcDepth > maxDepth) calcDepth = maxDepth;
			//if (calcDepth < minDepth) calcDepth = minDepth;
			
			System.out.println("New Calculation depth: " + calcDepth);
			System.out.println("Smaller Length: " + m);
			
			long time = update();
			swap();
		}
	}
	
	
	boolean pressed = false;
	boolean scrolling = false;
	int mx = 0;
	int my = 0;
	
	int scrolled = 0;
	
	KeyListener keyListener = new KeyListener() {
		
		@Override
		public void keyTyped(KeyEvent e) {
			
		}
		
		@Override
		public void keyReleased(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_BACK_SPACE: popStateStack(); break;
			case KeyEvent.VK_CONTROL:
				scrolling = false;
				scrollZoom();
				scrolled = 0;
				break;
			case KeyEvent.VK_ADD: increaseDepth(); break;
			case KeyEvent.VK_SUBTRACT: decreaseDepth(); break;
			case KeyEvent.VK_R: reset(); break;
			}
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_CONTROL:
				if (!scrolling) {
					scrolling = true;
				}
				break;
			}
		}
	};
	
	MouseWheelListener mouseWheelListener = new MouseWheelListener() {
		
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (scrolling && !pressed) {
				scrolled += e.getWheelRotation();
				if (scrolled > 49) scrolled = 49;
				if (scrolled < 0) scrolled = 0;
				mx = e.getX();
				my = e.getY();
				scrollingPreview();
			}
		}
	};
	
	MouseListener mouseListener = new MouseListener() {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (!pressed && !scrolling) {
				pressed = true;
				mx = e.getX();
				my = e.getY();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			pressed = false;
				
			if (!scrolling) {
				int tx = e.getX();
				int ty = e.getY();
				
				int dx = tx - mx;
				int dy = ty - my;
				
				int delta = Math.min(Math.round(dx*ASPECT_RATIO), dy);
				
				if (dx > 0 && dy > 0) {
					zoom(mx, mx+Math.round(delta*ASPECT_RATIO), my, my+delta);
					System.out.printf("Zooming into (%d, %d) - (%d, %d)\n", mx, my, mx+Math.round(delta*ASPECT_RATIO), my+delta);
				} else {
					swap();
				}
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
			if (!pressed && scrolling) {
				mx = e.getX();
				my = e.getY();
				scrollingPreview();
			}
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if (pressed && !scrolling) {
				Graphics g = getFramebuffer().createGraphics();
				g.setColor(Color.RED);
				int delta = Math.min(e.getX()-mx, e.getY()-my);
				g.drawRect(mx, my, Math.round(delta*ASPECT_RATIO), delta);
				g.drawOval(mx-3, my-3, 6, 6);
				g.drawOval(mx+Math.round(delta*ASPECT_RATIO)-3, my+delta-3, 6, 6);
				g.dispose();
				swap();
			} else if (!pressed && scrolling) {
				mx = e.getX();
				my = e.getY();
				scrollingPreview();
			}
		}
	};
	
	static class State {
		double borderLeft;
		double borderRight;
		double borderTop;
		double borderBottom;
		int depth;
		
		public State(double borderLeft, double borderRight, double borderTop, double borderBottom, int depth) {
			this.borderLeft = borderLeft;
			this.borderRight = borderRight;
			this.borderTop = borderTop;
			this.borderBottom = borderBottom;
			this.depth = depth;
		}
		
	}
}
