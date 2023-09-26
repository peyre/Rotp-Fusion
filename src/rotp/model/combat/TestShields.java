package rotp.model.combat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
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
	
	private BufferedImageList images;
	private BufferedImage shipImg;
	private int totalImages, currentImage, animationDelay;
	private Timer animationTimer;
	private String imagePath = "U:\\GitHub\\Rotp-Fusion\\src\\rotp\\images\\ships\\Klackon\\";
	private CombatShield cs;
//	private int[] source = new int[] {0, 0, 200}; 
//	private int[] shield = new int[] {200, 100, 0}; 
//	private int[] target = new int[] {200, 100, 0}; 
	private int[] shield = new int[] {0, 0, 0}; 
	private int[] target = new int[] {0, 0, 0}; 
	private int[] source = new int[] {1000, 500, 100}; 
	private int[] semiAx = new int[] {300, 200, 100};
	private Color shieldColor = Color.yellow;
	private Color beamColor   = Color.red;
	private int beamSize	  = 8;
	private int windUpFramesNum = 5;
	private int holdFramesNum   = 0;
	private int landUpFramesNum = windUpFramesNum;
	private int fadingFramesNum = 5;
	private int attacksPerRound = 1;
	private float beamForce = 100f;
	private float damage    = 50f;
	private int ImageSep    = 50;
	private int ImageWidth  = 466;
	private int ImageHeight = 350;

	public TestShields() {
		shipImg = loadImage(imagePath + "D01a.png");
		ImageWidth  = 2*semiAx[0] + ImageSep;
		ImageHeight = 2*semiAx[1] + ImageSep;
		System.out.println("Ship Image Width: " + shipImg.getWidth() + " Height: " + shipImg.getHeight());
		cs = new CombatShield(
				shield[0], shield[1], shield[2],
				semiAx[0], semiAx[1], semiAx[2],
				shieldColor);
		cs.setTarget(target[0], target[1], target[2], shipImg);
		cs.setSource(source[0], source[1], source[2], beamColor, beamSize);
		cs.setFrames(windUpFramesNum, holdFramesNum, landUpFramesNum, fadingFramesNum);
		cs.setWeapons(attacksPerRound, damage, beamForce);
		cs.getShieldList();
		System.exit(0);

		
		images = new BufferedImageList();
		images.add(loadImage(imagePath + "D01a.png"));
		images.add(loadImage(imagePath + "D01b.png"));
		images.add(loadImage(imagePath + "D01c.png"));
		images.add(loadImage(imagePath + "D01d.png"));
		totalImages = images.size();
		currentImage = 0;
		animationDelay = 200;
		startAnimation();
	}
	@Override public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int x = ImageSep;
		int y = ImageSep;
		BufferedImage img = images.get(currentImage);
		g.drawImage(img, x, y, this);
		
		x = ImageSep + ImageWidth;
		img = images.get(next(currentImage, 1));
		g.drawImage(img, x, y, this);

		y = ImageSep + ImageHeight;
		img = images.get(next(currentImage, 2));
		g.drawImage(img, x, y, this);

		x = ImageSep;
		img = images.get(next(currentImage, 3));
		g.drawImage(img, x, y, this);

		currentImage = next(currentImage, 1);
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
		app.setSize(1250,1000);
		app.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//app.setSize(anim.getPreferredSize().width + 10, anim.getPreferredSize().height + 30);
		app.setLocation(500, 150);
		app.setVisible(true);
	}
}
