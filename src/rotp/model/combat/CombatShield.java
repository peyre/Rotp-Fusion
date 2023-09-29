package rotp.model.combat;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;

// Combat Shield as ellipsoid
// Z target must be lower than Z source
// Delta Z should be tune for the best effects...
public final class CombatShield {

	private static final double	TWO_PI	= 2 * Math.PI;
	private static final double	HALF_PI	= Math.PI/2;

	private static final int ringNum	= 4;
	private static final int CENTER	= 3;
	private static final int IMPACT	= 2;
	private static final int WAVE	= 1;
	private static final int SOCLE	= 0;

	private static final int BEAM_ALPHA	  = 0xFF;
	private static final int WAVE_ALPHA   = 0x88;
	private static final int SHIELD_ALPHA = 0x33;

	private static final float BEAM_EVOL	= 1.5f;
	private static final float WAVE_EVOL	= 1.5f;
	private static final float SHIELD_EVOL	= 1.5f;
	
	// Rendering
	private final Ring[] ringArray = new Ring[ringNum];
	private int imageColumns, imageRows, paintLength;
	private Tint[] paint;

	// Locations, size, weapons
	private final Pos shieldCenter	 = new Pos();
//	private final Pos shieldSemiAxis = new Pos();
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
	private final Geodesic geodesic = new Geodesic();
	private double insideRatio;

	// Other variables
	private boolean neverInitialized = true;

	// Debug
	boolean showTiming = true;
	boolean debug = false;
	
	// = = = = = Parameters management = = = = =
	//
	private BufferedImageList shieldList() {
		if (neverInitialized)
			initShields();
		return shieldList;
	}
	private double insideRatio() {
		if (neverInitialized)
			initShields();
		return insideRatio;
	}
	// = = = = = Constructors, Setters and Initializers = = = = =
	//
	CombatShield(int x, int y, int z, int b, int a, Color color) {
		shieldCenter.set(x, y, z);
		imageColumns  = 2*b+1;
		imageRows     = 2*a+1;
		shieldColor   = color;
		geodesic.setSemiAxis(a, b);
		paintLength = 2*(a+b); // very approximative
		paint = new Tint[paintLength];
		
		for (int ring=0; ring<ringNum; ring++)
			ringArray[ring] = new Ring();

		ringArray[SOCLE].color = new Tint(color).saturate(SHIELD_ALPHA);
		ringArray[WAVE].color  = new Tint(color).saturate(WAVE_ALPHA);
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
		ringArray[IMPACT].color = new Tint(color).saturate(BEAM_ALPHA);
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
		neverInitialized = false;
		long timeStart = System.currentTimeMillis();
		long timeMid = timeStart;

		locateShieldImpact();
		System.out.println("insideRatio: " + insideRatio);
		System.out.println("Impact: " + shieldImpact);	
		geodesic.setImpact(shieldImpact);

		if (showTiming)
			System.out.println("initImpact() Time = " + (System.currentTimeMillis()-timeMid));
		timeMid = System.currentTimeMillis();
		
		drawImpactShield();

		if (showTiming)
			System.out.println("drawImpactShield() Time = " + (System.currentTimeMillis()-timeMid));
		timeMid = System.currentTimeMillis();

		for (int i=0; i<windUpFramesNum; i++)
			drawWindUpShield(i);
		for (int i=0; i<holdFramesNum; i++)
			drawHoldShield(i);
		for (int i=0; i<landUpFramesNum; i++)
			drawLandUpShield(i);
		for (int i=0; i<fadingFramesNum; i++)
			drawFadingShield(i);


		if (showTiming)
			System.out.println("initShields() Time = " + (System.currentTimeMillis()-timeStart));
	}
	
	// = = = = = getters = = = = =
	//
	BufferedImage getNextImage()	  { return shieldList().remove(0); }
	BufferedImageList getShieldList() { return shieldList(); };
	int[] shieldImpact() { return shieldImpact.toIntArray(); }
	// = = = = = Private Methods = = = = =
	//
	private void drawShield() { // TODO BR: Here
		long timeStart = System.currentTimeMillis();
		long timeMid = timeStart;

		initPaint();
		if (showTiming)
			System.out.println("- initPaint() Time = " + (System.currentTimeMillis()-timeMid));		
		timeMid = System.currentTimeMillis();

		BufferedImage topShield = new BufferedImage(imageColumns, imageRows, BufferedImage.TYPE_INT_ARGB);
		BufferedImage bottomShield = new BufferedImage(imageColumns, imageRows, BufferedImage.TYPE_INT_ARGB);

		int y = -geodesic.a;
		for (int row=0; row<imageRows; row++) {
			int x = -geodesic.b;
			for (int column=0; column<imageColumns; column++) {
				if (geodesic.insideEllipse(x, y)) {
					// Color on the shield
					int[] colors = geodesic.getColors(x, y, column);
					if (colors[0]!=0)
						topShield.setRGB(column, row, colors[0]);
					if (colors[1]!=0)
						bottomShield.setRGB(column, row, colors[1]);
				}
				x++;
			}
			y++;
		}
		this.shieldList().add(topShield);
		this.shieldList().add(bottomShield);
		
		if (showTiming)
			System.out.println("- Full drawShield() Time = " + (System.currentTimeMillis()-timeStart));		
	}
	private void initPaint() {
//		int xmax = 20;
		for (int x=0; x<paintLength; x++) {
			Tint color = new Tint();
			for (Ring r: ringArray) {
//				System.out.println("Ring color = "+ r.color.toString()); // TODO BR: Remove
//				System.out.println("Ring = " + r.toString()); // TODO BR: Remove
				Tint pointColor = paintPointColor(x, r);
				color.addColor(pointColor);
//				if (x<xmax) {
//					System.out.println("  Ring color = "+ r.color.toString()
//						+ "  PointColor = "+ pointColor.toString()
//						+ "  color = "+ color.toString()); // TODO BR: Remove
//				}
			}
			paint[x] = color;
//			if (color.alpha > 0)
//				System.out.println(x + " color = "+ color.toString()); // TODO BR: Remove
//			if (x<xmax)
//				System.out.println(x + " color = "+ color.toString()); // TODO BR: Remove
		}
	}
	private Tint paintPointColor(int x, Ring ring) { // New int[]
		if (ring.width == 0) // no 0 divisor!
			return new Tint();
		if (x < ring.center - ring.width)
			return new Tint();
		if (x > ring.center + ring.width)
			return new Tint();
		// Envelope Shape
		double phi = HALF_PI * (x - ring.center) / ring.width;
		float alphaMultiplier = (float) Math.pow((1 + Math.cos(phi))/2, ring.shape);
//		if (x<5)
//			System.out.println("  - alphaMultiplier = " + alphaMultiplier); // TODO BR: Remove
		return new Tint(ring.color).setAlphaMultiplier(alphaMultiplier);
	}
//	private int alphaFactor(int x, int y) {
	private void drawImpactShield() {
		//No black center
		Ring r = ringArray[CENTER];
		r.color		= ringArray[IMPACT].color;
		r.center	= 0f;
		r.width		= 1.0f * beamDiameter;
		r.shape		= 0.0f;
		r.ctrEvol	= 0f;
		r.widthEvol	= 0f;
		r.shapeEvol	= 0f;
		// Beam Impact
		r = ringArray[IMPACT];
		r.center	= 1f * beamDiameter;
		r.width		= 1f * beamDiameter;;
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
		for (Ring r : ringArray) {
			r.evolve();
		}
		drawShield();
	}
	private void drawHoldShield(int i) {
		
	}
	private void drawLandUpShield(int i) {
		
	}
	private void drawFadingShield(int i) {
		
	}
	
	private double findShieldImpact(Pos src, Pos tar, Pos impact) {
		// -> Center the ellipsoid on point = [0,0,0]
		// Surface equation: x*x/a/a + y*y/b/b + z*z/c/c = 1  
		// New target
		double xo = tar.x - shieldCenter.x;
		double yo = tar.y - shieldCenter.y;
		double zo = tar.z - shieldCenter.z;
		// - beam path
		double dx = src.x - tar.x;
		double dy = src.y - tar.y;
		double dz = src.z - tar.z; // Should be positive
		// beam equation: [xo, yo, zo] + m * [dx, dy, dz] = [x, y, z] with m>0
		// Intersection point (z always positive)
		// (xo + m∙dx)^2 /a +  (yo + m∙dy)^2 /b +  (zo + m∙dz)^2 /c = 1
		//
		double dxN = dx/geodesic.bb;
		double dyN = dy/geodesic.aa;
		double dzN = dz/geodesic.aa;
		double k1 = 2*(xo*dxN + yo*dyN + zo*dzN);
		double k2 = 2*(dx*dxN + dy*dyN + dz*dzN);
		double k3 = xo*xo/geodesic.bb + (yo*yo+zo*zo)/geodesic.aa;
		double sr = Math.sqrt(k1*k1 - 2*(k3-1)*k2);
		double m1 = (-sr-k1)/k2;
		double m2 = (sr-k1)/k2;
		double insideRatio = m2>m1 ? m2 : m1;
		impact.x = tar.x + insideRatio * dx;
		impact.y = tar.y + insideRatio * dy;
		impact.z = tar.z + insideRatio * dz;
		return insideRatio;
	}
	private void locateShieldImpact() { insideRatio = findShieldImpact(shipSource, shipImpact, shieldImpact); }

	// = = = = = Tools = = = = =
	//
//	private Point2D center(Rectangle rect) {
//		return new Point2D.Float((float) rect.getCenterX(), (float) rect.getCenterY());
//	}
//	private float bounds(float low, float val, float hi) {
//		return Math.min(Math.max(low, val), hi);
//    }
	// = = = = = Nested Classes = = = = =
	//
	public static class BufferedImageList extends ArrayList<BufferedImage> { }

	final class Tint {
		//private final int[] tint;
		private static final int FF = 255;
		private int trans = FF; // Transparency = 1 - alpha
//		private int alpha = 0; // = opacity
		private int red   = 0;
		private int green = 0;
		private int blue  = 0;
		
		private Tint()				{ }
		private Tint(Tint source)	{ set(source); }
		private Tint(int... source)	{ set(source); }
		private Tint(Color source)	{ set(source); }
		private Tint(int source)	{ set(source); }

		@Override protected Tint clone() {return new Tint(this); }
		@Override public String toString() { 
			return "A=" + (FF-trans) +
					" R=" + red +
					" G=" + green +
					" B=" + blue +
					" T=" + trans;
		}

		// Getters
//		private int[] vec() { return new int[] {alpha, red, green, blue}; }
		private int argb()  { return (((FF-trans) << 24) | (red << 16) | (green << 8) | blue); }
		private int argb(double transparencyFactor)	{ // most intensively used
			if (trans==FF)
				return 0;
			return ((FF-(int)(trans*transparencyFactor) << 24) 
					| (red << 16) | (green << 8) | blue);
		}
		// Setters
		private Tint set(Tint color) {
			trans = color.trans;
			red = color.red;
			green = color.green;
			blue = color.blue;
			return this;
		}
		private Tint set(int... color) {
			trans = FF-color[0];
			red	  = color[1];
			green = color[2];
			blue  = color[3];
			return this;
		}
		private Tint set(int color)	{
			trans = FF-((color >> 24) & FF);
			red   = (color >> 16) & FF; 
			green = (color >> 8) & FF;
			blue  = color & 0xff;
			return this;
		}		
		private Tint set(Color source) {
			set(source.getRGB());
			return this;
		}
		// Operations
		private Tint addColor(Tint color) {
			if (trans == FF)
				return set(color);
			if (color.trans == FF)
				return this;
			int ct = color.trans;
			trans = trans * ct / FF;
			int ca = FF-color.trans;
			red   = (red   * ct + color.red   * ca) / FF;
			green = (green * ct + color.green * ca) / FF;
			blue  = (blue  * ct + color.blue  * ca) / FF;
			return this;
		}
		Tint mult(float factor) {
			red   *= factor;
			green *= factor;
			blue  *= factor;
			return this;
		}
		private Tint saturate() { // new int[]
			int max = Math.max(red, Math.max(green, blue));
			if (max == 0) {
				red = FF;
				green = FF;
				blue = FF;
			}
			else {
				float factor = 255f/max;
				red *= factor;
				green *= factor;
				blue *= factor;
			}
			return this;
		}
		private Tint saturate(int alpha) {
			trans = FF-alpha;
			return saturate();
		}
//		private Tint setAlphaPower(float factor) {
//			alpha = (int) Math.pow(alpha, factor);
//			return this;
//		}
//		private Tint setAlpha(float alpha) {
//			this.alpha = (int) (255*alpha);
//			return this;
//		}
		private Tint setAlphaMultiplier(float mult) {
			trans = (int) (FF-mult*(FF-trans));
			return this;
		}
	}

	private final class Geodesic {
		// Lambert's formula for long lines
		// Ellipsoid parameters
		private double[] beta;
		private double[] sqSinPcosQx2;
		private double[] sqCosPsinQx2;
//		private int vSize, vOffset;
		private int a, b;		// a = The circle radius, b the third semi-axis
		private double f;		// flattening
		private double half_f;
		private int aa, bb; 
		private long aabb;
		private double iaa, ibb;
		private double b_a, aa_bb;	// b/a

		// Impact parameters
		private double xI, yI, zI;	// the impact point
		private double betaI;	// reduced latitude
		
		private Geodesic() {}
		private Geodesic(int a, int b) { setSemiAxis(a, b); }

		private void setSemiAxis(int a, int b) {
			this.a	= a;
			this.b	= b;
			f		= (a-b)/(double)a;
			half_f	= f/2;
			b_a		= b/(double)a;
			aa_bb	= 1/(b_a*b_a);
			aa	= a*a;
			bb	= b*b;
			aabb = (long)aa*bb;
			iaa = 1/(double)aa;
			ibb = 1/(double)bb;
//			vSize	= imageColumns;
//			vOffset	= b;
			sqSinPcosQx2 = new double[imageColumns];
			sqCosPsinQx2 = new double[imageColumns];
			// Fill the spheric correction
			beta = new double[imageColumns];
			for (int i=0; i<imageColumns; i++) {
				beta[i] = reducedLatitude(i-b);
			}
		}
		private void setImpact(Pos I) {
			xI = I.x;
			yI = I.y;
			zI = I.z;
			betaI = beta[(int) Math.round(xI+b)];
			for (int i=0; i<imageColumns; i++) {
				double betaP = reducedLatitude(i-b);
				double cosP = Math.cos((betaI + betaP)/2);
				double cos2P = cosP*cosP;
				double sin2P = 1-cos2P;
				double cosQ = Math.cos((betaI - betaP)/2);
				double cos2Q = cosQ*cosQ;
				double sin2Q = 1-cos2Q;
				sqSinPcosQx2[i] = 2*sin2P*cos2Q;
				sqCosPsinQx2[i] = 2*cos2P*sin2Q;
			}
		}
		private boolean insideEllipse(long x, long y) {
			return (x*x*aa + y*y*bb) <= aabb;
		}
		private double reducedLatitude(int x) { // z in earth geodesic, but x for us!
			// tan(β)=(1-f)tan(Φ) with f = (a-b)/a = 1-b/a
			// tan(β)= b/a * tan(Φ)
			// Known: x = b*sin(Φ) ==> sin(Φ) = x/b;
			// tan(β)= b/a * tan(asin(x/b))
			// Known: tan(asin(α)) = α/sqrt(1-a^2)
			// tan(β)=b/a * x/b / sqrt(1-x^2/b^2)
			// tan(β)=x/a / sqrt(1-x^2/b^2)
			// tan(β)=x*b/a / sqrt(b^2-x^2)
			
			int delta = bb - x*x;
			if (delta==0)
				if (x>0)
					return HALF_PI;
				else
					return -HALF_PI;
			else
				return Math.atan(b_a*x/Math.sqrt(delta));
		}
		private int[] getColors(int x, int y, int idx) { // TODO BR: Optimize
			// x = b*cos(θ) = b*sin(Φ)
			// y = a*cos(λ)*sin(θ) = a*cos(λ)*cos(Φ)
			// z = a*sin(λ)*sin(θ) = a*sin(λ)*cos(Φ)
			// set point
			double z = Math.sqrt(aa - y*y - x*x*aa_bb);
			double transparencyFactor = 1-z/bb;
			// Spherical central angle Δσ
			// Δσ = arcCos(sin(θ1)sin(θ2) + cos(θ1)cos(θ2)cos(λ1-λ2))
			// sin(θ1)sin(θ2) = (x1*x2)/b^2
			// cos(θ1)cos(θ2)cos(λ1-λ2) = cos(θ1)cos(θ2)cos(λ1)cos(λ2) + cos(θ1)cos(θ2)sin(λ1)sin(λ2)
			// cos(θ1)cos(θ2)cos(λ1-λ2) = (y1*y2+z1*z2)/a^2
			// Δσ = arcCos(x1*x2/b^2 + (y1*y2+z1*z2)/a^2)
			double xPN = x*xI*ibb + y*yI*iaa;
			double zzN = z*zI*iaa;
			// Top
			double cosΔσ = xPN + zzN;
			double Δσ = Math.acos(cosΔσ); 
			int dist = (int) (a*(Δσ-half_f*( 2*Δσ + Math.sin(Δσ) *
					(sqCosPsinQx2[idx]/(1-cosΔσ) - sqSinPcosQx2[idx]/(1+cosΔσ)) )));
			int topColor = paint[dist].argb(transparencyFactor);
			// Bottom
			cosΔσ = xPN -zzN;
			Δσ = Math.acos(cosΔσ); 
			dist = (int) (a*(Δσ-half_f*( 2*Δσ + Math.sin(Δσ) *
					(sqCosPsinQx2[idx]/(1-cosΔσ) - sqSinPcosQx2[idx]/(1+cosΔσ)) )));
			int bottomColor = paint[dist].argb(transparencyFactor);
			return new int[] {topColor, bottomColor};
		}
		private int[] distToTarget(int x, int y, int idx) {
			// x = b*cos(θ) = b*sin(Φ)
			// y = a*cos(λ)*sin(θ) = a*cos(λ)*cos(Φ)
			// z = a*sin(λ)*sin(θ) = a*sin(λ)*cos(Φ)
			// set point
			double z = Math.sqrt(aa - y*y - x*x*aa_bb);
			// Spherical central angle Δσ
			// Δσ = arcCos(sin(θ1)sin(θ2) + cos(θ1)cos(θ2)cos(λ1-λ2))
			// sin(θ1)sin(θ2) = (x1*x2)/b^2
			// cos(θ1)cos(θ2)cos(λ1-λ2) = cos(θ1)cos(θ2)cos(λ1)cos(λ2) + cos(θ1)cos(θ2)sin(λ1)sin(λ2)
			// cos(θ1)cos(θ2)cos(λ1-λ2) = (y1*y2+z1*z2)/a^2
			// Δσ = arcCos(x1*x2/b^2 + (y1*y2+z1*z2)/a^2)
			double xPN = x*xI*ibb + y*yI*iaa;
			double zzN = z*zI*iaa;
			// Top
			double cosΔσ = xPN + zzN;
			double Δσ = Math.acos(cosΔσ); 
			int top = (int) (a*(Δσ-half_f*( 2*Δσ + Math.sin(Δσ) *
					(sqCosPsinQx2[idx]/(1-cosΔσ) - sqSinPcosQx2[idx]/(1+cosΔσ)) )));
			// Bottom
			cosΔσ = xPN -zzN;
			Δσ = Math.acos(cosΔσ); 
			int bottom = (int) (a*(Δσ-half_f*( 2*Δσ + Math.sin(Δσ) *
					(sqCosPsinQx2[idx]/(1-cosΔσ) - sqSinPcosQx2[idx]/(1+cosΔσ)) )));
			return new int[] {top, bottom};
		}
		
		@Override public String toString() { 
			return "x=" + xI + " y=" + yI + " z=" + zI;
		}
	}
	private final class Pos {
		private double x, y, z;
		
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
		private int[] toIntArray() {
			return new int[] {(int) Math.round(x), (int) Math.round(y), (int) Math.round(z)}; 
		}
		private double[] toDoubleArray() {
			return new double[] {x, y, z}; 
		}
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
		
		private void evolve() {
			center *= ctrEvol;
			width  *= widthEvol;
			shape  *= shapeEvol;
		}
		@Override public String toString() {
			return "ctr=" + center + " w=" + width + " shape=" + shape;
		}
	}
}
