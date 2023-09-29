package rotp.model.combat;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import rotp.model.combat.CombatShield.BufferedImageList;


public class TestShields extends JPanel implements ActionListener {
	
	private static final int[] SEMI_AXIS	= new int[] {300, 200}; // Width, ray (b, a)
	private static final float SCREEN_RATIO	= (float)9/16; 
	private static final int SCREEN_WIDTH	= 3840; 
	private static final int SCREEN_HEIGHT	= (int) (SCREEN_WIDTH * SCREEN_RATIO + 0.5f);
	private static final int COLUMNS_NUM	= 10; 
	private static final int ROWS_NUM		= 8; 
	private static final int IMAGE_SEP		= 50;
	private String imagePath = "U:\\GitHub\\Rotp-Fusion\\src\\rotp\\images\\ships\\Klackon\\";
	
	private BufferedImageList images;
	private BufferedImage shipImg;
	private int totalImages, currentImage, animationDelay;
	private Timer animationTimer;
	private CombatShield cs;
	private int[] shieldPos = new int[] {0, 0, 0}; 
	private int[] targetPos = new int[] {0, 0, 0}; 
	private int[] sourcePos = new int[] {1000, 500, 1000}; 
	private int[] semiAxis;
	private Color shieldColor = Color.green;
	private Color beamColor   = Color.red;
	private int beamSize	  = 8;
	private int windUpFramesNum = 5;
	private int holdFramesNum   = 0;
	private int landUpFramesNum = windUpFramesNum;
	private int fadingFramesNum = 5;
	private int attacksPerRound = 1;
	private float beamForce = 100f;
	private float damage    = 50f;
//	private int imageWidth  = 466;
//	private int imageHeight = 350;
//	private int shipDX = (2*semiAxis[0]+1-imageWidth)/2;
//	private int shipDY = (2*semiAxis[1]+1-imageHeight)/2;
	private int imageWidth, imageHeight;
	private int shipDX, shipDY;
	private int shipWidth, shipHeight;
	private int boxWidth, boxHeight;
	private int windowWidth,windowHeight;
	private float shipScale;
	

	private int[] initSemiAxis(float shipScale) { // TODO BR: Image analysis
		int sx = (int) (SEMI_AXIS[0] * shipScale);
		int sy = (int) (SEMI_AXIS[1] * shipScale);
		return new int[]{sx, sy};
	}
	private void initSizes() {
		// Box size
		boxWidth  = (SCREEN_WIDTH-scaled(20))/COLUMNS_NUM;
		boxHeight = (SCREEN_HEIGHT-scaled(65))/ROWS_NUM;
		// ship size
		BufferedImage baseShipImg = loadImage(imagePath + "D01a.png");
		int baseShipWidth  = baseShipImg.getWidth();
		int baseShipHeight = baseShipImg.getHeight();
		shipScale  = Math.min((float)boxWidth/baseShipWidth, (float)boxHeight/baseShipHeight)*9/10;
		shipWidth  = (int) (baseShipWidth  * shipScale);
		shipHeight = (int) (baseShipHeight * shipScale);
		
		shipImg = new BufferedImage(shipWidth, shipHeight, TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) shipImg.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); 
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(baseShipImg, 0, 0, shipWidth, shipHeight, 0, 0, baseShipWidth, baseShipHeight, null);
		g.dispose();
		semiAxis = initSemiAxis(shipScale);
		shipDX = (2*semiAxis[0]+1-shipWidth)/2;
		shipDY = (2*semiAxis[1]+1-shipHeight)/2;
		imageWidth  = 2*semiAxis[0]+1 + IMAGE_SEP;
		imageHeight = 2*semiAxis[1]+1 + IMAGE_SEP;
		
		windowWidth  = IMAGE_SEP + 2*imageWidth  + 10;
		windowHeight = IMAGE_SEP + 2*imageHeight + 30;
	}
	private int scaled(int i) {
        int maxX = SCREEN_HEIGHT*8/5;
        int maxY = SCREEN_WIDTH*5/8;
        if (maxY > SCREEN_HEIGHT)
            maxY = maxX*5/8;
        float resizeAmt = (float) maxY/768;
		return (int) (i * resizeAmt);
	}
	public TestShields() {
		initSizes();

		cs = new CombatShield(
				shieldPos[0], shieldPos[1], shieldPos[2],
				semiAxis[0], semiAxis[1], shieldColor);
		cs.setTarget(targetPos[0], targetPos[1], targetPos[2], shipImg);
		cs.setSource(sourcePos[0], sourcePos[1], sourcePos[2], beamColor, beamSize);
		cs.setFrames(windUpFramesNum, holdFramesNum, landUpFramesNum, fadingFramesNum);
		cs.setWeapons(attacksPerRound, damage, beamForce);
		images = cs.getShieldList();
		
		totalImages = images.size();
		currentImage = 0;
		animationDelay = 50;
		startAnimation();
	}
	@Override public void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;

		g.setComposite(AlphaComposite.SrcOver);
		int x = IMAGE_SEP;
		int y = IMAGE_SEP;
		BufferedImage topImg    = images.get(currentImage);
		BufferedImage bottomImg = images.get(next(currentImage, 1));

		g.drawImage(bottomImg, x, y, this);
		
		x = IMAGE_SEP + imageWidth;
		g.drawImage(topImg, x, y, this);

		y = IMAGE_SEP + imageHeight;
		g.drawImage(bottomImg, x, y, this);
		g.drawImage(shipImg, x+shipDX, y+shipDY, this);
		g.drawImage(topImg, x, y, this);

		x = IMAGE_SEP;
		g.drawImage(bottomImg, x, y, this);
		g.drawImage(shipImg, x+shipDX, y+shipDY, this);

		currentImage = next(currentImage, 2);
	}
	@Override public void actionPerformed(ActionEvent e) { repaint(); }
	private int next(int val, int incr) { return (val + incr) % totalImages; }
	private BufferedImage loadImage(String path) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return img;
	}
	public void startAnimation() {
		if (animationTimer == null) {
			currentImage = 0;
			animationTimer = new Timer(animationDelay, this);
			animationTimer.start();
		}
		else if (!animationTimer.isRunning())
			animationTimer.restart();
	}
	public void stopAnimation() { animationTimer.stop(); }
	public static void main(String args[]) {
		TestShields anim = new TestShields();
		JFrame app = new JFrame("Animator test");
		app.add(anim, BorderLayout.CENTER);
//		app.setSize(1360,980);
		app.setSize(anim.windowWidth, anim.windowHeight);
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//app.setSize(anim.getPreferredSize().width + 10, anim.getPreferredSize().height + 30);
		app.setLocation(600, 350);
		app.setVisible(true);
	}
}
