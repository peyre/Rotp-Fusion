package rotp.model.combat;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

// Combat Shield as ellipsoid
// Z target must be lower than Z source
// Delta Z should be tune for the best effects...
public final class CombatShield {

	//private static final int[]	noColor	= new int[] {0, 0, 0, 0};
	private static final double	TWO_PI	= 2 * Math.PI;
	private static final int	ringNum	= 4;
	private static final int	CENTER	= 0;
	private static final int	IMPACT	= 1;
	private static final int	SOCLE	= 2;
	private static final int	WAVE	= 3;
	
	// Rendering
	private final Ring[] ringArray = new Ring[ringNum];
	private int imageColumns, imageRows, paintLength;
	private Tint[] paint;
	private float beamAlpha, shieldAlpha;

	// Locations, size, weapons
	private final Pos shieldCenter	 = new Pos();
	private final Pos shieldSemiAxis = new Pos();
	private final Pos shipSource	 = new Pos();
	private final Pos shipImpact	 = new Pos();
	private int beamDiameter;
	private Color shieldColor, beamColor;
	private int windUpFramesNum, holdFramesNum, landUpFramesNum, fadingFramesNum, framesNum;
	private int attacksPerRound;
	private BufferedImage targetImage;

	// Results
	private final BufferedImageList shieldList = new BufferedImageList();;
	private final Pos shieldImpact = new Pos();
//	private final Spheric shieldImpact = new Spheric();
	private final Geodesic geodesic = new Geodesic();
//	private Geodesic toDisplay = new Geodesic();
	private float insideRatio;

	// Other variables
	private boolean neverInitialized = true;
	private float aa, bb, cc;

	// Debug
	boolean showTiming = true;
	
	// = = = = = Parameters management = = = = =
	//
	private BufferedImageList shieldList() {
		if (neverInitialized)
			initShields();
		return shieldList;
	}
	private float insideRatio() {
		if (neverInitialized)
			initShields();
		return insideRatio;
	}
	// = = = = = Constructors, Setters and Initializers = = = = =
	//
	CombatShield(int x, int y, int z, int a, int b, int c, Color color) {
		shieldCenter.set(x, y, z);
		shieldSemiAxis.set(a, b, c);
		imageColumns  = 2 * a;
		imageRows     = 2 * b;
		shieldColor   = color;
		aa = a*a;
		bb = b*b;
		cc = c*c;
		geodesic.setSemiAxis(a, b);
//		geodesic.iaaG = 1/aa;
//		geodesic.ibbG = 1/bb;
//		geodesic.icc = 1/cc;
		paintLength = 2*(a+b); // very approximative
		paint = new Tint[paintLength];
		
		for (int ring=0; ring<ringNum; ring++)
			ringArray[ring] = new Ring();

		ringArray[SOCLE].color = new Tint(color).saturate(0xFF);
		ringArray[WAVE].color  = new Tint(color).saturate(0xFF);
	}
	void setTarget(int x, int y, int z, BufferedImage image) {
		shipImpact.set(x, y, z);
//		geodesic.setImpact(shipImpact);
		
		targetImage   = image;
	}
	void setSource(int x, int y, int z, Color color, int beamSize) {
		shipSource.set(x, y, z);
		beamDiameter = beamSize;
		beamColor	 = color;
		ringArray[IMPACT].color = new Tint(color).saturate(0xFF);
	}
	void setFrames (int windUpFrames, int holdFrames, int landUpFrames, int fadingFrames) {
		windUpFramesNum = windUpFrames;
		holdFramesNum   = holdFrames;
		landUpFramesNum = landUpFrames;
		fadingFramesNum = fadingFrames;
		framesNum = 1 + windUpFramesNum + holdFramesNum + landUpFrames + fadingFramesNum;
	}
	void setWeapons(int attacksPerRound, float damage, float beamForce) {
		this.attacksPerRound = attacksPerRound;
	}
	private void initShields() { // TODO BR: initShields() {
		long timeStart = System.currentTimeMillis();

		locateShieldImpact();
		System.out.println("insideRatio: " + insideRatio);
		System.out.println("Impact: " + shieldImpact);	
		geodesic.setImpact(shieldImpact);

		drawImpactShield();

		for (int i=0; i<windUpFramesNum; i++)
			drawWindUpShield(i);
		for (int i=0; i<holdFramesNum; i++)
			drawHoldShield(i);
		for (int i=0; i<landUpFramesNum; i++)
			drawLandUpShield(i);
		for (int i=0; i<fadingFramesNum; i++)
			drawFadingShield(i);


		neverInitialized = false;
		if (showTiming)
			System.out.println("initShields() Time = " + (System.currentTimeMillis()-timeStart));
	}
	
	// = = = = = getters = = = = =
	//
	BufferedImage getNextImage()	  { return shieldList().remove(0); }
	BufferedImageList getShieldList() { return shieldList(); };
	int[] shieldImpact() { return shieldImpact.toArray(); }
	// = = = = = Private Methods = = = = =
	//
	private void drawShield() { // TODO BR: Here
		long timeStart = System.currentTimeMillis();

		initPaint();
		if (showTiming)
			System.out.println("initPaint() Time = " + (System.currentTimeMillis()-timeStart));		

		int[][] rgbaArr = new int[imageRows][imageColumns];
		int x = -shieldSemiAxis.x;
		int y = -shieldSemiAxis.y;
		for (int row=0; row<imageRows; row++) {
			x++;
			for (int column=0; column<imageColumns; column++) {
				y++;
				if (outsideEllipse(x, y))
					rgbaArr[row][column] = 0;
				else {
					// Color on the shield
					int[] distance = geodesic.distToTarget(x, y); // TODO
					Tint colorOnShield = new Tint(paint[distance[0]]);
					// Perspective view
					int alphaFactor = alphaFactor(x, y);
					colorOnShield.setAlphaPower(alphaFactor);
					rgbaArr[row][column] = colorOnShield.argb();
				}
			}
		}
		if (showTiming)
			System.out.println("drawShield() Time = " + (System.currentTimeMillis()-timeStart));		
	}
	private void initPaint() {
		for (int x=0; x<paintLength; x++) {
			Tint color = new Tint();
			for (Ring r: ringArray) {
				Tint pointColor = paintPointColor(x, r);
				color.addColor(pointColor);
			}
			paint[x] = color;
		}
	}
	private Tint paintPointColor(int x, Ring ring) { // New int[]
		if (ring.width == 0) // no 0 divisor!
			return new Tint();
		if (x < ring.center - ring.width)
			return new Tint();
		if (x < ring.center + ring.width)
			return new Tint();
		double phi = TWO_PI * (x - ring.center) / ring.width;
		float alphaMultiplier = (float) Math.pow((1 + Math.cos(phi))/2, ring.shape);
		return new Tint(ring.color).mult(alphaMultiplier);
	}
	private int alphaFactor(int x, int y) {
		double z = Math.sqrt(1 - x*x/aa - y*y/bb);
		double cos = Math.cos(z); 
		return (int)cos;
	}
	private void drawImpactShield() {
		//No black center
		Ring r = ringArray[CENTER];
		r.color		= ringArray[IMPACT].color;
		r.center	= 0f;
		r.width		= beamDiameter;
		r.shape		= 0.5f;
		r.ctrEvol	= 0f;
		r.widthEvol	= 0f;
		r.shapeEvol	= 0f;
		// Beam Impact
		r = ringArray[IMPACT];
		r.center	= 2f * beamDiameter;
		r.width		= 2f * beamDiameter;;
		r.shape		= 1f;
		r.ctrEvol	= 1.2f;
		r.widthEvol	= 1.2f;
		r.shapeEvol	= 0.9f;
		// Shield socle
		r = ringArray[SOCLE];
		r.center	= 3f * beamDiameter;
		r.width		= 4f * beamDiameter;;
		r.shape		= 0.5f;
		r.ctrEvol	= 1.2f;
		r.widthEvol	= 1.2f;
		r.shapeEvol	= 0.8f;
		// shield wave
		r = ringArray[WAVE];
		r.center	= 3f * beamDiameter;
		r.width		= 0.5f * beamDiameter;;
		r.shape		= 2f;
		r.ctrEvol	= 1.2f;
		r.widthEvol	= 1.2f;
		r.shapeEvol	= 0.8f;
		
		drawShield();
	}
	private void drawWindUpShield(int i) {
		
	}
	private void drawHoldShield(int i) {
		
	}
	private void drawLandUpShield(int i) {
		
	}
	private void drawFadingShield(int i) {
		
	}
	
	private void mapColor() {
		// Ellipsoid: latitude φ and longitude λ
		// work on sphere for impact
		// One line Gradient
		// Impact:
		// - get ray to center
		// Point to plot on plan (ellipse image)
		// - position Ellipse to position circle
		// - position circle to position on sphere
		// - project point to impact ray -> rp
		// - rp determine distance to impact di
		// - di determine color
		// Rendering
		// - x & y determine slope
		// - Slope determine Alpha multiplication 
		// - plot point color & alpha
	}
	private boolean outsideEllipse(int x, int y) { return (x*x/aa + y*y/bb) > 1; }
	private float findShieldImpact(Pos src, Pos tar, Pos impact) {
		// -> Center the ellipsoid on point = [0,0,0]
		// Surface equation: x*x/a/a + y*y/b/b + z*z/c/c = 1  
		// New target
		float xo = tar.x - shieldCenter.x;
		float yo = tar.y - shieldCenter.y;
		float zo = tar.z - shieldCenter.z;
		// - beam path
		float dx = src.x - tar.x;
		float dy = src.y - tar.y;
		float dz = src.z - tar.z; // Should be positive
		// beam equation: [xo, yo, zo] + m * [dx, dy, dz] = [x, y, z] with m>0
		// Intersection point (z always positive)
		// (xo + m∙dx)^2 /a +  (yo + m∙dy)^2 /b +  (zo + m∙dz)^2 /c = 1
		//
		float k1 = 2*(xo*dx/aa + yo*dy/bb + zo*dz/cc);
		float k2 = 2*(dx*dx/aa + dy*dy/bb + dz*dz/cc);
		float k3 = xo*xo/aa + yo*yo/bb + zo*zo/cc;
		float sr = (float) Math.sqrt(k1*k1 - 2*(k3-1)*k2);
		float m1 = (-sr-k1)/k2;
		float m2 = (sr-k1)/k2;
		float insideRatio = m2>m1 ? m2 : m1;
		impact.x = Math.round(tar.x + insideRatio * dx);
		impact.y = Math.round(tar.y + insideRatio * dy);
		impact.z = Math.round(tar.z + insideRatio * dz);
		return insideRatio;
	}
	private void locateShieldImpact() { insideRatio = findShieldImpact(shipSource, shipImpact, shieldImpact); }

	// = = = = = Tools = = = = =
	//
	private Point2D center(Rectangle rect) {
		return new Point2D.Float((float) rect.getCenterX(), (float) rect.getCenterY());
	}
	private float bounds(float low, float val, float hi) {
		return Math.min(Math.max(low, val), hi);
    }
	// = = = = = Nested Classes = = = = =
	//
	public static class BufferedImageList extends ArrayList<BufferedImage> { }

	final class Tint {
		//private final int[] tint;
		private static final int FF = 255;
		private int a = 0;
		private int r = 0;
		private int g = 0;
		private int b = 0;
		
		private Tint()				{ }
		private Tint(Tint source)	{ set(source); }
		private Tint(int[] source)	{ set(source); }
		private Tint(Color source)	{ set(source); }
		private Tint(int source)	{ set(source); }

		@Override protected Tint clone() {return new Tint(this); }

		// Getters
		private int[] vec() { return new int[] {a, r, g, b}; }
		private int argb()  { return ((a << 24) | (r << 16) | (g << 8) | b); }
		// Setters
		private Tint set(Tint color) {
			a = color.a;
			r = color.r;
			g = color.g;
			b = color.b;
			return this;
		}
		private Tint set(int[] color) {
			a = color[0];
			r = color[1];
			g = color[2];
			b = color[3];
			return this;
		}
		private Tint set(int color)	{
			a = (color >> 24) & FF;
			r = (color >> 16) & FF; 
			g = (color >> 8) & FF;
			b = color & 0xff;
			return this;
		}		
		private Tint set(Color source) {
			set(source.getRGB());
			return this;
		}
		// Operations
		private Tint addColor(Tint color) {
			if (a == 0)
				return set(color);
			if (color.a == 0)
				return this;
			int ac = FF-color.a;
			a = 255 - (((FF - a) * ac) / FF);
			r = (r * ac + color.r * color.a) / FF;
			g = (g * ac + color.g * color.a) / FF;
			b = (b * ac + color.b * color.a) / FF;
			return this;
		}
		Tint mult(float factor) {
			r *= factor;
			g *= factor;
			b *= factor;
			return this;
		}
		private Tint saturate() { // new int[]
			int max = Math.max(r, Math.max(g, b));
			if (max == 0) {
				r = FF;
				g = FF;
				b = FF;
			}
			else {
				float factor = 1f/max;
				r *= factor;
				g *= factor;
				b *= factor;
			}
			return this;
		}
		private Tint saturate(int alpha) {
			a = alpha;
			return saturate();
		}
		private Tint setAlphaPower(float factor) {
			a = (int) Math.pow(a, factor);
			return this;
		}
	}

	private final class Geodesic {
		// Lambert's formula for long lines
		// Ellipsoid parameters
		private static final double	HALF_PI	= Math.PI/2;
		private int aG, bG;		// a = The circle radius, b the third semi-axis
		private double fG;		// flattening
		private int aaG, bbG;
		private double iaaG, ibbG;
		private double b_a, bb_aa;	// b/a

		// Impact parameters
		private int xI, yI, zI;	// the impact point
		private double betaI;	// reduced latitude
		
		private Geodesic() {}
		private Geodesic(int a, int b) { setSemiAxis(a, b); }

		private void setSemiAxis(int a, int b) {
			aG = a;
			bG = b;
			fG	= (a-b)/(double)a;
			b_a	= b/(double)a;
			bb_aa = b_a*b_a;
			aaG	= a*a;
			bbG	= b*b;
			iaaG = 1/(double)aaG;
			ibbG = 1/(double)bbG;
		}
		private void setImpact(Pos I) {
			xI = I.x;
			yI = I.y;
			zI = I.z;
			betaI = reducedLatitude(zI);
		}
		private double reducedLatitude(int z) {
			int delta = bbG - z*z;
			if (delta==0)
				return HALF_PI;
			else
				return Math.atan(b_a*z/Math.sqrt(delta));
		}
		private int[] distToTarget(int x, int y) {
			// set point
			int d = x*x -y*y;
			int z = (int) Math.round(Math.sqrt(bbG - d*bb_aa));
			// spherical Parameters
			// Sigma
			int zS	 = zI+z;
			int xySP = x*xI + y*yI;
			double cosSigma = iaaG*zS + ibbG*xySP ;
			double sigma = Math.acos(cosSigma);
			double sinSigma = Math.sin(sigma);
			// Reduced Latitude
			double betaP = reducedLatitude(z);
			double cosP = Math.cos((betaI + betaP)/2);
			double cos2P = cosP*cosP;
			double sin2P = 1-cos2P;
			double cosQ = Math.cos((betaI - betaP)/2);
			double cos2Q = cosQ*cosQ;
			double sin2Q = 1-cos2Q;
			// X and Y
			double X = sigma - 2 * sinSigma * sin2P*cos2Q/(1+cosSigma);
			double Y = sigma + 2 * sinSigma * cos2P*sin2Q/(1-cosSigma);
			int top = (int) (aG*(sigma*fG/2*(X+Y)));
			return new int[] {top};
		}
		
		@Override public String toString() { 
			return "x=" + xI + " y=" + yI + " z=" + zI;
		}
	}
	private final class Pos {
		private int x, y, z;
		
		private Pos() {}
		private Pos(int x, int y, int z) { set(x, y, z); }
		private Pos(int[] src) { set(src); }
		private Pos set(int[] src) {
			x = src[0];
			y = src[1];
			z = src[2];
			return this;
		}
		private Pos set(int X, int Y, int Z) {
			x = X;
			y = Y;
			z = Z;
			return this;
		}
		private int aGeo() { return y; } // Geodesic def
		private int bGeo() { return x; } // Geodesic def
		private int[] toArray() { return new int[] {x, y, z}; }
		@Override public String toString() { return "x=" + x + " y=" + y + " z=" + z; }
	}

	private final class Ring {
		private Tint  color     = new Tint();
		private float center	= 0;
		private float width		= 0;
		private float shape		= 1f;
		private float ctrEvol	= 1f;
		private float widthEvol	= 1f;
		private float shapeEvol	= 1f;

	}
}
