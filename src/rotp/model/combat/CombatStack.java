/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.combat;

import static java.awt.MultipleGradientPaint.CycleMethod.NO_CYCLE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static rotp.model.ships.ShipDesign.MAX_SIZE;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipWeapon;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.TechCloaking;
import rotp.model.tech.TechStasisField;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;
import rotp.util.Base;
import rotp.util.BufferedImageList;

public class CombatStack implements Base {
    static final Color shipCountTextC = new Color(255,240,78);
    static final Float MOVE_STEP = 0.1f;
    public static final Color healthBarC = new Color(0,96,0);
    public static final Color shipCountBackC = new Color(0,0,0);
    public static final Color healthBarBackC = new Color(0,48,0);
    public static final Color healthBorderC = new Color(64,192,64);
    public static final Color shipShieldC = new Color(128,128,0);
    public static final Color shipAttackC = new Color(128,0,0);
    public static final Color shipMissDefenseC = new Color(0,0,128);
    public static final Color shipBeamDefenseC = new Color(64,64,160);
    public static final Color sysShieldC = new Color(128,128,0);
    public static final Color sysPopC = new Color(128,0,0);
    public static final Color sysFactoryC = new Color(0,0,128);
    public Empire empire;
    public ShipCombatManager mgr;
    public ShipCaptain captain;
    public final List<CombatStackMissile> targetingMissiles = new ArrayList<>();
    public int num = 0;
    public int origNum = 0;
    public int x = 0;
    public int y = 0;
    public float scale = 1.0f;
    public float brighten = 0.0f;
    public float attackLevel = 0;
    public float maneuverability = 0;
    public float missileDefense = 0;
    public float beamDefense = 0;
    public float offsetX = 0;
    public float offsetY = 0;
    public float startingMaxHits = 1;
    public float maxHits = 1;
    public float maxMove = 0;
    public float move = 0;
    public float maxShield = 0;
    public float shield = 0;
    public float hits = 0;
    public float repairPct = 0;
    public int beamRangeBonus = 0;
    public boolean inStasis = false;
    public boolean cloaked = false;
    public boolean canCloak = false;
    public boolean canTeleport = false;
    public boolean atLastColony = false;
    public float damageSustained = 0;
    public boolean attacked = false;
    public boolean destroyed = false;
    public CombatStack target;
    public int distance = 0;
    public Image image;
    public boolean reversed = false;
    public boolean ally = true;
    public boolean visible = true;
    public float transparency = 1;
    public String destroyedSoundEffect() { return "ShipExplosion"; }

    public String shortString() {
        return concat(toString()," at:", str(x), ",", str(y));
    }
    public static Comparator<CombatStack> INITIATIVE = (CombatStack o1, CombatStack o2) -> Base.compare(o2.initiativeRank(), o1.initiativeRank());
    public CombatStack() { }
    public CombatStack(ShipCombatManager m, Empire c) {
        mgr = m;
        empire = c;
        captain = empire.ai().shipCaptain();
    }
    public String fullName()            { return concat(str(num), ":", raceName(), " ", name()); }
    public String raceName()            { return empire != null ? empire.raceName() : name(); }
    public String name()                { return "object"; }
    public float initiative()           { return 0; }
    public float initiativeRank() {
        if (cloaked)
            return 200+initiative();
        // modnar: replace canTeleport from this 'if' check
		// In ShipCombatManager.java, the CombatStack.INITIATIVE comparison/sort in setupBattle
		// is called before currentStack.beginTurn(). So while beginTurn() in this file
		// sets the correct value for canTeleport, it won't be used for initiative ordering.
		// This change correctly gives boosted turn/initiative order for ship stacks with teleporters.
        else if (hasTeleporting() && !mgr.interdiction())
            return 100+initiative();
        else
            return initiative();
    }
    public boolean isShip()             { return false; }
    public boolean isColony()           { return false; }
    public boolean isMonster()          { return false; }
	public boolean isNeutralShip()      { return false; } // modnar: add new type, for SpacePirates scan display
    public boolean isPlayer()           { return (empire != null) && empire.isPlayer(); }
    public boolean isPlayerControlled() { return (empire != null) && empire.isPlayerControlled(); }
    public boolean isMissile()          { return false; }
    public boolean destroyed()          { return ((num < 1) || (maxHits <= 0)); }
    public boolean isArmed()            { return false; }
    public boolean hasTarget()          { return target != null; }
    public CombatStack ward()           { return null; }
    public boolean hasWard()            { return false; }
    public void ward(CombatStack st)    { }
    public boolean hasBombs()           { return false; }
    public boolean canChangeTarget()    { return true; }
    public boolean canCollide()         { return false; }
    public boolean usingAI()            { return true; }

    public int repulsorRange()          { return 0; }
    public boolean ignoreRepulsors()    { return false; }
    public int weaponNum(ShipComponent w)  { return -1; }
    public boolean canRetreat()      { return false; }
    public boolean canTeleport()     { return canTeleport && !mgr.interdiction(); }
    public boolean hasTeleporting()  { return false; }
    public boolean canScan()         { return false; }
    public boolean retreatAllowed()  { return false; }
    public void becomeDestroyed()    { destroyed = true; num = 0;}
    public int numWeapons()          { return 0; }
    public ShipComponent weapon(int i)   { return null; }
    public ShipDesign design()       { return null; }
    public float designCost()          { return 0; }

    public void performTurn()        { captain.performTurn(this); }
    public boolean wantToRetreat()   { return captain.wantToRetreat(this); }

    public float maxHits()          { return maxHits; }
    public float maxMove()          { return maxMove; }
    public float totalHits()        { return maxHits * num; }
    public boolean canMove()        { return (move > 0) || canTeleport(); }
    public boolean canFireWeapon()  { return false; }
    public boolean canFireWeaponAtTarget(CombatStack st)  { return false; }
    public boolean immuneToStasis() { return false; }
    public float autoMissPct()      { return 0; } 
    public boolean interceptsMissile(ShipWeaponMissileType wpn)  { return random() < missileInterceptPct(wpn);}
    public float missileInterceptPct(ShipWeaponMissileType wpn)  { return 0; }
    public float maneuverablity()     { return maneuverability; }
    public float missileDefense()     { return cloaked ? missileDefense +5 : missileDefense; }
    public float beamDefense()        { return cloaked ? beamDefense + 5 : beamDefense; }
    public float attackLevel()      { return attackLevel; }
    public float bombDefense()      { return 0; }
    public float bioweaponDefense() { return 0; }
    public void cloak()             {  }
    public void uncloak()           {  }
    public boolean canEat(CombatStack st)       { return false; }
    public boolean hostileTo(CombatStack st, StarSystem sys)                  { return empire != st.empire; }
    public boolean selectBestWeapon(CombatStack target)       { return false; }
    public boolean currentWeaponCanAttack(CombatStack target) { return false; }
    public boolean canAttack(CombatStack target)              { return false; }
    public boolean canPotentiallyAttack(CombatStack target)   { return false; }
    public boolean canDamage(CombatStack target)              { return maxDamage() > target.shieldLevel(); }
    public float estimatedKills(CombatStack target)           { return 0; }
    public float estimatedKillPct(CombatStack target)         { return target.num == 0 ? 0 : estimatedKills(target) / target.num; }
    public void rotateToUsableWeapon(CombatStack target)      {  }
    public void fireWeapon(CombatStack target, int i, boolean shots) { }
    public void fireWeapon(CombatStack target, int i) { fireWeapon(target,i,false); }
    public void fireWeapon(CombatStack target)       {  }
    public int weaponIndex()                         { return 0; }
    public int shots()                               { return 1; }
    public int maxFiringRange(CombatStack tgt)       { return 1; }
    public int optimalFiringRange(CombatStack tgt)   { return 1; }
    public int minFiringRange()                      { return 1; }
    public float maxDamage()                         { return 0; }
    public float shieldLevel()                       { return shield; }
    public ShipComponent selectedWeapon()            { return null; }
    public float rotateRadians()                     { return 0; }
    public float rotateRadians(CombatStack target)   { return radiansTo(target) + ((float)Math.PI/2); }

    public float torpedoDamageMod()                  { return 1; }
    public float beamDamageMod()                     { return 1; }
    public float bombDamageMod()                     { return 1; }
    public float missileDamageMod()                  { return 1; }
    public float blackHoleDef()                      { return 0; }
    public void assignCollateralDamage(float damage) {  }
    public void recordKills(int num)                 {  }
    public boolean retreat()                         { return retreatToSystem(captain.retreatSystem(mgr.system())); }
    public boolean retreatToSystem(StarSystem s)     { return false; }

    public boolean aggressiveWith(CombatStack st)    { return empire.aggressiveWith(st.empire, mgr.system()); }

    public void usedBioweapons() { mgr.results().addBioweaponUse(empire); }
    public void reverse()                           { reversed = !reversed; }
    public List<CombatStackMissile> missiles()       { return targetingMissiles; }
    public void addMissile(CombatStackMissile miss)  { targetingMissiles.add(miss); }
    public float scale()                             { return scale; }
    public int weaponRange(ShipComponent c) {
        if (!c.isBeamWeapon())
            return c.range();
        return c.range()+beamRangeBonus;     
    }

    public boolean isTurnComplete() {
        if (inStasis)
            return true;

        if (canMove())
            return false;

        if (canFireWeapon())
            return false;

        return true;
    }
    public void beginTurn() {
        move = maxMove;
        canTeleport = hasTeleporting() && !mgr.interdiction();

        reloadWeapons();
        attemptToHeal();
        List<CombatStackMissile> missiles = new ArrayList<>(targetingMissiles);
        for (CombatStackMissile miss : missiles)
            miss.beginTurn();
    }
    public void reloadWeapons() { };
    public void attemptToHeal() {
        if (hits >= startingMaxHits)
            return;
        if (repairPct <= 0)
            return;
        float healAmt = startingMaxHits*repairPct;
        hits = min(startingMaxHits, hits+healAmt);
        maxHits = max(hits, maxHits);
    }
    public void endTurn() {
        if (!destroyed())
            finishMissileRemainingMoves();
        List<CombatStackMissile> missiles = new ArrayList<>(targetingMissiles);
        for (CombatStackMissile miss : missiles)
            miss.endTurn();
    }
    public int missileMovePointsTo(CombatStack target) { // BR: Missiles move differently
    	float dist = distanceTo(target.x, target.y);
        return (int) Math.ceil(dist-CombatStackMissile.MIN_ATTACK_DIST);
    }
    public int movePointsTo(CombatStack target) {
        int distX = Math.abs(x - target.x);
        int distY = Math.abs(y - target.y);
        return max(distX, distY);
    }

    public int movePointsTo(int x1, int y1) {
        int distX = Math.abs(x - x1);
        int distY = Math.abs(y - y1);
        return max(distX, distY);
    }
    public int movePointsTo(int x0, int y0, int x1, int y1) {
        int distX = Math.abs(x0 - x1);
        int distY = Math.abs(y0 - y1);
        return max(distX, distY);
    }
    public float distanceTo(int x1, int y1) {
        return sqrt(((x-x1)*(x-x1)) + ((y-y1)*(y-y1)));
    }
    public float distanceTo(float x1, float y1) {
        return sqrt(((x()-x1)*(x()-x1)) + ((y()-y1)*(y()-y1)));
    }
    public boolean canMoveTo(int x, int y) {
        return (movePointsTo(x, y) <= move);
    }
    public void teleportTo(int x1, int y1, float amt) {
        int oldX = x;
        int oldY = y;
        drawFadeOut(amt);
        canTeleport = false;
        x = x1;
        y = y1;
        offsetX = 0;
        offsetY = 0;
        drawFadeIn(oldX, oldY);
    }
    public int turnsToTravel(int distance) {
        int turns = 0;
        int mv = (int) move;
        int remaining = distance;
        while (remaining > 0) {
            turns++;
            remaining -= mv;
            mv = (int) maxMove();
        }
        return turns;
    }
    public boolean moveTo(int x1, int y1) {
        float plannedDistance = movePointsTo(x1,y1);

        while (submoveTo(x1,y1))
            ;

        distance += plannedDistance;
        move -= plannedDistance;        
        return !destroyed();
    }
    public boolean submoveTo(float x1, float y1) {
        boolean b = submoveTo(x1,y1, targetingMissiles);
        if (mgr.showAnimations()) 
            mgr.ui.paintAllImmediately(20);
        
        return b;
    }
    public boolean submoveTo(float x1, float y1, List<CombatStackMissile> missiles) {
        // this method performs one "sub-move" of a stack to its destination,
        // then allows each pursuing missile to perform a sub-move
        // the distance of the sub-move is dependent on the stack's maneuverability

        // float movePct = missiles.isEmpty() && !mgr.showAnimations() ? 1.0f : MOVE_STEP;
        float x0 = x();
        float y0 = y();

        float totalDist = distance(x0,y0,x1,y1);
        float stepDist = MOVE_STEP;

        float stepPct = min(1,stepDist/totalDist);

        float distX = Math.abs(x0-x1);
        float distY = Math.abs(y0-y1);
        float xIncr = stepPct*distX;
        float yIncr = stepPct*distY;

        if (x1 < x())  xIncr = -xIncr;
        if (y1 < y())  yIncr = -yIncr;

        offsetY += yIncr;
        offsetX += xIncr;
        distY -= stepDist;
        if (distY <= 0) {
            y = (int) y1; 
            offsetY = 0;
        }
        
        distX -= stepDist;
        if (distX <= 0) {
            x = (int) x1; 
            offsetX = 0;
        }
        
        // allow missiles to pursue (check for cloaking). They may damage stack
        if (!missiles.isEmpty()) {
            List<CombatStackMissile> tempMissiles = new ArrayList<>(missiles);
            for (CombatStackMissile miss : tempMissiles)
                miss.pursue(stepDist);
        }

        // return true if still alive and haven't reached x1,y1
        return (((x != x1) || (y != y1)) && (!destroyed()));
    }
    public void finishMissileRemainingMoves() {
        while (!performMissileSubmove()) { }
    }
    public boolean performMissileSubmove() {
        boolean missilesFinished = true;
        List<CombatStackMissile> targetCopy = new ArrayList<>(targetingMissiles);
        for (CombatStackMissile miss : targetCopy)
            missilesFinished = miss.pursue(MOVE_STEP) && missilesFinished;
        
        if (mgr.showAnimations()) 
            mgr.ui.paintAllImmediately(20);

        return missilesFinished;
    }
    public float x() { return x + offsetX; }
    public float y() { return y + offsetY; }
    public boolean atGrid(int x1, int y1) {
        return (x == x1) && (y == y1);
    }
    public float radiansTo(CombatStack target) {
        float dx = x() - target.x();
        float dy = y() - target.y();

        if (dy > 0) 
            return (float)(Math.PI - Math.atan(dx/dy));
        else if (dy < 0) {
            if (dx > 0)
                return 0 - (float) Math.atan(dx/dy);
            else
                return (float)(Math.PI + Math.PI - Math.atan(dx/dy));
        }
        else {
            if (dx > 0)
                return (float)(Math.PI / 2);
            else
                return (float)(1.5 * Math.PI);
        }
    }
    public void takeBioweaponDamage(float damage) { }
    public float takeHullDamage(float damage) {
        if (inStasis)
            return 0;
        
        if (damage <= 0)
            return 0;
        attacked = true;
        maxHits = max(0,maxHits - damage);
        hits = min(hits,maxHits);

        if (hits <= 0)
            loseShip();

        assignCollateralDamage(damage);
        return damage;
    }
    protected float takeDamage(float damage, float shieldAdj) {
        if (inStasis)
            return 0;
        float damageTaken = 0;
        attacked = true;
        float dmg = max(0, damage - (shieldLevel() * shieldAdj));
        damageTaken += dmg;
        if (dmg == 0)
            return damageTaken;

        if (num > 0) {
            damageSustained += min(dmg, hits);
            hits -= dmg;
            if (hits <= 0)
               loseShip();
            if (destroyed() && (mgr != null))
                mgr.destroyStack(this);
        }
        assignCollateralDamage(dmg);
        return damageTaken;
    }
    public float takeMissileDamage(float damage, float shieldAdj) {
        return takeDamage(damage*missileDamageMod(), shieldAdj);
    }
    public float takeTorpedoDamage(float damage, float shieldAdj) {
        return takeDamage(damage*torpedoDamageMod(), shieldAdj);
    }
    public float takeBeamDamage(float damage, float shieldAdj) {
        return takeDamage(damage*beamDamageMod(), shieldAdj);
    }
    public float takeBombDamage(float damage, float shieldAdj) {
        return takeDamage(damage*bombDamageMod(), shieldAdj);
    }
    public float takePulsarDamage(float damage, float shieldAdj) {
        float adjDam = damage - (shieldLevel() * shieldAdj);
        return takeHullDamage(adjDam);
    }
    public float takeStreamingDamage(float damage, float shieldAdj) {
         if (inStasis)
            return 0;
        float damageTaken = 0;
        attacked = true;
        float damageLeft = damage*beamDamageMod();
        while ((damageLeft > 0) && !destroyed()) {
            float dmg = max(0, damageLeft - (shieldLevel() * shieldAdj));
            damageTaken += dmg;
            damageSustained += min(dmg, hits);
            hits -= dmg;
            if (hits <= 0) {
                damageLeft = 0 - hits;
                loseShip();
            }
            else
                damageLeft = 0;
        }
        assignCollateralDamage(damage);
        return damageTaken;
    }
    public float takeBlackHoleDamage(float pct) {
        if (inStasis)
            return 0;
        attacked = true;
        float pctLoss = pct - (shieldLevel() /50) - blackHoleDef();
        pctLoss = max(0,pctLoss);
        int kills = Math.round(num * pctLoss);
        for(int i = 0; i < kills; ++i)
            loseShip();
        return kills * maxHits;
    }
    public void loseShip() {
        int lost = maxHits > 0 ? 1 : num;
        hits = maxHits;
        shield = maxShield;
        num = max(0, num - lost);
        if (destroyed() && (mgr != null))
            mgr.destroyStack(this);
    }
    public boolean shipComponentIsUsed(int index)                          { return true; }
    public boolean shipComponentIsOutOfMissiles(int index)                 { return false; }
    public boolean shipComponentIsOutOfBombs(int index)                    { return false; }
    public boolean shipComponentValidTarget(int index, CombatStack target) {
        return empire != target.empire ? false : weapon(index).validTarget(target);
    }
    public boolean shipComponentInRange(int index, CombatStack target)     { return false; }
    public int wpnCount(int i)                                             { return 0; }
    public int shotsRemaining(int i)                                       { return 0; }
    public float targetShieldMod(ShipComponent c)                          { return 1.0f; }
    public String wpnName(int i) { return ""; }

    public FlightPath pathTo(int x, int y) {
        return captain.pathTo(this, x, y);
    }
    public void drawFadeOut(float amt) {
        if (!mgr.showAnimations())
            return;
        
        float maxTransparency = cloaked ? TechCloaking.TRANSPARENCY : 1.0f;
        ShipBattleUI ui = mgr.ui;
        // Graphics2D g = (Graphics2D) ui.getGraphics();

        // fade out
        for (float i=maxTransparency; i>=0; i-=amt) {
            transparency = i;
            long t0 = System.currentTimeMillis();
            ui.paintCellImmediately(x,y);
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 25)
                sleep(25-t1);
        }
    }
    public void drawFadeIn(int oldX, int oldY) {
        if (!mgr.showAnimations())
            return;
        
        float maxTransparency = cloaked ? TechCloaking.TRANSPARENCY : 1.0f;
        ShipBattleUI ui = mgr.ui;
        // Graphics2D g = (Graphics2D) ui.getGraphics();

        // fade in, but ensure old position is cleared out first
        ui.paintCellImmediately(oldX, oldY);
        for (float i = 0; i <= maxTransparency; i += .10f) {
            transparency = i;  // might already be cloaked!
            long t0 = System.currentTimeMillis();
            ui.paintCellImmediately(x, y);
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 25)
                sleep(25-t1);
        }
    }
    public void drawDamageTaken(float dmg, String result) {
        if (!mgr.showAnimations())
            return;
        
        int stW = mgr.ui.stackW();
        int stH = mgr.ui.stackH();
        int st1X = mgr.ui.stackX(this);
        int st1Y = mgr.ui.stackY(this);
        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;        
        Graphics2D g = (Graphics2D) mgr.ui.getGraphics();
        drawAttackResult(g,x1,y1,x1, dmg,result);   
        mgr.ui.paintAllImmediately();    
    }
    public void drawAttackResult(Graphics g, int x1, int y1, int x0, float dmg, String result) {
        if (!mgr.showAnimations())
            return;
         
        int xleft = x0 < x1 ? x : max(0, x-1);
        Rectangle rTopLeft = mgr.ui.combatGrids[xleft][max(0,y-1)];
        
        int FRAMES = mgr.autoComplete ? 4 : 12;
        int dx = x0 <= x1 ? BasePanel.s1 : -BasePanel.s1;
        int dy = -BasePanel.s1;
        int dFont = 1;
        int dAlpha = 255/ (FRAMES/4);

        Color[] cRed = new Color[FRAMES];
        Color[] cWhite = new Color[FRAMES];
        Font[] font = new Font[FRAMES];
        int alpha = 255;
        int fontsize = 20;
        for (int i=0;i<FRAMES;i++) {
            cWhite[i] =  new Color(255,255,255,alpha);
            cRed[i] = new Color(255,64,64,alpha);
            font[i] = narrowFont(fontsize);
            if (i > FRAMES*3/4)
                alpha -= dAlpha;
            fontsize += dFont;
        }

        int x2 = x1;
        int y2 = y1;
        String displayStr =  dmg > 0 ? "-" + (int) Math.ceil(dmg) : result;

        // set a clip to minimize delays cause by potential side-effect
        // repainting of the ui
        int clipX = rTopLeft.x+(int)((offsetX-0.2)*rTopLeft.width);
        int clipY = rTopLeft.y+(int)((offsetY-0.2)*rTopLeft.height);
        int clipW = 5*rTopLeft.width/2;
        int clipH = 5*rTopLeft.height/2;
        g.setClip(clipX, clipY, clipW, clipH);

        for (int i=0;i<FRAMES;i++) {
            long t0 = System.currentTimeMillis();
            if (!mgr.showAnimations()) 
                break;
            mgr.ui.paintImmediately(clipX,clipY,clipW,clipH);
            g.setFont(font[i]);
            Color c0;
            if (dmg != 0)
                c0 = cRed[i];
            else
                c0 = cWhite[i];
            g.setColor(c0);
            //drawBorderedString(g, displayStr, x2, y2, cWhite[i], c0);
            drawString(g,displayStr, x2, y2);
            x2 += dx;
            y2 += dy;
            fontsize += dFont;
            long dur = System.currentTimeMillis() - t0;
            if (dur < 50)
                sleep(50-dur);
        }
        g.setClip(null);
    }
    public void drawStack(ShipBattleUI ui, Graphics2D g, int origCount, int x, int y, int stackW, int stackH) {
        Image img = image;

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale0 = min((float)stackW/w0, (float)stackH/h0)*9/10;

        int x1 = x;
        int y1 = y;
        int w1 = (int)(scale0*w0);
        int h1 = (int)(scale0*h0);

        if (scale != 1.0f) {
            int prevW = w1;
            int prevH = h1;
            w1 = (int) (w1*scale);
            h1 = (int) (h1*scale);
            x1 = x1 +(prevW-w1)/2;
            y1 = y1 +(prevH-h1)/2;
        }
        
        Composite prevComp = g.getComposite();
        BufferedImage overlayImg = null;
        if (brighten > 0) {
            overlayImg = newBufferedImage(w1,h1);
            Graphics2D g0 = (Graphics2D) overlayImg.getGraphics();
            if (transparency < 1) {
                AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,max(0,transparency));
                g0.setComposite(ac);
            }
            if (reversed)  // XOR
                g0.drawImage(img, 0, 0, w1, h1, w0, 0, 0, h0, ui);
            else
                g0.drawImage(img, 0, 0, w1, h1, 0, 0, w0, h0, ui);
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_IN,min(1,brighten));
            g0.setComposite(ac);
            g0.setColor(Color.white);
            g0.fillRect(0, 0, w1, h1);
            g0.setComposite(prevComp);
            g0.dispose();
        }
        
        if (transparency < 1) {
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,max(0,transparency));
            g.setComposite(ac);
        }
		// modnar: one-step progressive image downscaling, slightly better
		// there should be better methods
		if (scale0 < 0.5) {
			BufferedImage tmp = new BufferedImage(w0/2, h0/2, TYPE_INT_ARGB);
			Graphics2D g2D = tmp.createGraphics();
			g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2D.drawImage(img, 0, 0, w0/2, h0/2, 0, 0, w0, h0, ui);
			g2D.dispose();
			img = tmp;
			w0 = img.getWidth(null);
			h0 = img.getHeight(null);
			scale0 = scale0*2;
		}
		// modnar: use (slightly) better downsampling
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (reversed)  // XOR
            g.drawImage(img, x1, y1, x1+w1, y1+h1, w0, 0, 0, h0, ui);
        else
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, ui);
        
        g.setComposite(prevComp);
        if (overlayImg != null) 
            g.drawImage(overlayImg, x1, y1, ui);
            
        int y2 = y+stackH-BasePanel.s5;
        g.setFont(narrowFont(16));
        String s = text(name());
        int sw2 = g.getFontMetrics().stringWidth(s);
        int x2 = max(x1, x1+((stackW-sw2)/2));

        g.setColor(Color.lightGray);
        drawString(g,s, x2, y2);

        if (inStasis) {
            g.setColor(TechStasisField.STASIS_COLOR);
            g.fillRect(x1,y1,stackW, stackH);
            s = text("SHIP_COMBAT_STASIS");
            g.setFont(font(20));
            g.setColor(Color.white);
            int sw = g.getFontMetrics().stringWidth(s);
            int x3 = x1+(stackW-sw)/2;
            int y3 = y1+(stackH/2);
            drawBorderedString(g, s,x3,y3, Color.black, Color.white);
        }
        int mgn = BasePanel.s2;
        int x4 = x+mgn;
        int y4 = y+mgn;
        int w4 = stackW-mgn-mgn;
        int barH = BasePanel.s10;
        if (ui.showTacticalInfo()) {
            // draw health bar & hp
            g.setColor(healthBarBackC);
            g.fillRect(x4, y4, w4, barH);
            int w4a = (int)(w4*hits/maxHits);
            g.setColor(healthBarC);
            g.fillRect(x4, y4, w4a, barH);
            int numW = 0;
            // draw ship count
            if (num > 1) {
                g.setColor(healthBarC);
                String numStr = str(num);
                g.setFont(narrowFont(20));
                numW = g.getFontMetrics().stringWidth(numStr);
                int x6 = reversed ? x4: x4+w4-numW-BasePanel.s10;
                g.fillRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
                g.setColor(Color.white);
                Stroke prevStroke = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
                g.setStroke(prevStroke);
                g.drawString(numStr, x6+BasePanel.s5,y4+BasePanel.s18);
            }
            // draw hit points
            g.setColor(Color.white);
            String hpStr = ""+(int)Math.ceil(hits)+"/"+(int)Math.ceil(maxHits);
            g.setFont(narrowFont(12));
            int hpW = g.getFontMetrics().stringWidth(hpStr);
            int x5 = reversed ? x4+((w4-hpW+numW)/2) : x4+((w4-hpW-numW)/2);
            g.drawString(hpStr, x5, y4+BasePanel.s9);
        }
    }
    //Used for making AI capable of assessing the power of Monsters
    public float firePower(float shield, float defense, float missileDefense) {
        float dmg = 0;
        for (int i=0;i<numWeapons(); i++) {
            if (weapon(i).canAttackShips()) {
                if(weapon(i).isWeapon()) {
                    ShipWeapon wpn = (ShipWeapon)weapon(i);
                    float attack = attackLevel() + wpn.computerLevel();
                    float hitPct = 1;
                    if(weapon(i).isBeamWeapon())
                        hitPct = (5 + attack - defense) / 10;
                    if(weapon(i).isMissileWeapon())
                        hitPct = (5 + attack - missileDefense) / 10;
                    hitPct = max(.05f, hitPct);
                    hitPct = min(hitPct, 1.0f);
                    dmg += (wpnCount(i) * wpn.firepower(shield) * hitPct);
                }
            }
        }
        return dmg;
    }
    public Dimension shieldSize(int boxW, int boxH) {
    	final float fW  = 4.0f;	// To convert shipSize to Shield Width
    	final float fH  = 4.0f;	// To estimate the target size
    	Dimension dim = new Dimension();
        int shipSize;
    	if (isColony() || design() == null) {
    		shipSize = MAX_SIZE;
    		dim.height = (int) (boxH * (shipSize + fH) / (fH + MAX_SIZE));
    		dim.width  = dim.height;
    	} else {
    		shipSize = design().size();
    		dim.height = (int) (boxH * (shipSize + fH) / (fH + MAX_SIZE));
    		dim.width  = (int) (boxW * (shipSize + fW) / (fW + MAX_SIZE));
    	}
    	return dim;
    }
    public Color shieldBaseColor() {
    	if (empire == null)
        	return Color.red;
        else
        	return empire.color();
    }
    public BufferedImage[] shieldImg(int nWindUpF, int nHoldF, int nCloseF, int nAttacks,
    		Dimension size, int srcX, int srcY, int tarX, int tarY, int xAdj, int yAdj,
    		Color beamColor, float beamForce, float damage) { // TODO BR: BufferedImage[] shieldImg(

    	// Impact Radius is function of beam power (^)
    	// Impact Transparency is function of beam absorption ratio (v)
    	// Spreading Radius is function of beam power (^) and shield level (v)
    	// Spreading Transparency is function of beam absorption ratio (v) and and shield level (v)

    	final float topShield = 15f;
    	final float alphaImpactMin = 0.5f;
    	final float alphaImpactMax = 1.0f;
    	final float alphaSpreadMin = 0.1f;
    	final float alphaSpreadMax = 0.3f;
    	final float alphaEndDiv    = 2.0f;
        final float impactRbase    = 0.05f;
        final float impactRmin     = 0.05f;
        final float impactRmax     = 0.20f;
        final float spreadDrEnd    = 0.05f;
        final float spreadDrMin    = 0.2f;
        final float spreadingRmax  = 0.95f - spreadDrEnd;
    	final float fSh    = 2.0f;	// To estimate the shield impact position

//    	int nFrame = 1 + nHoldF + nWindUpF + nCloseF;
//    	BufferedImage[] shieldArr = new BufferedImage[nFrame];
    	BufferedImage[] shieldArr = new BufferedImage[nWindUpF];
    	if (nWindUpF == 0)
    		return shieldArr;
    	
        float shieldForce = maxShield / topShield;
        float beamPowerFactor = 0;
        float absorptionRatio = 0;       
        if (beamForce > 0) {
        	beamPowerFactor = (float) Math.log10(beamForce);
            absorptionRatio = damage>0? 1 - (damage/beamForce):1;
        }
        // Transparencies: Transparent = 0; Opaque = 1
        float spreadingFactor = bounds(0, sqrt(beamPowerFactor * absorptionRatio), 1);
        float alphaImpact = alphaImpactMin + absorptionRatio * (alphaImpactMax-alphaImpactMin);
        float alphaSpread = alphaSpreadMin + spreadingFactor * (alphaSpreadMax-alphaSpreadMin);
        // Rays
        spreadingFactor = bounds(0, sqrt(shieldForce * absorptionRatio), 1);
        float impactRay = bounds(impactRmin, impactRbase*beamPowerFactor, impactRmax);
        float spreadingRayMin = impactRay + spreadDrMin;
        float spreadRay =  spreadingRayMin + spreadingFactor * (spreadingRmax-spreadingRayMin);
        impactRay /= nWindUpF;
        spreadRay /= nWindUpF;
        float alphaEnd = min(1f, alphaSpread/alphaEndDiv);
    	// Colors
        Color noColor =  new Color(0f,0f,0f,0f);
        // Impact color based on BeamWeapon
        Color impactColor = saturateColor(beamColor, alphaImpact);
    	// Spread color based on Target color
        Color spreadColor = saturateColor(shieldBaseColor(), alphaSpread);
        Color spreadEndColor = setAlpha(spreadColor, alphaEnd);
        
        // True positions
    	int shieldW = size.width;
    	int shieldH = size.height;
    	int hitX = tarX + xAdj;
    	int hitY = tarY + yAdj;
    	
    	// Hit the shield before hitting the target
        // Relative positions
    	float norm    = distance(srcX, srcY, hitX, hitY);
    	float shiftX  = -(hitX - srcX) / norm * shieldW/2 / fSh;
    	float shiftY  = -(hitY - srcY) / norm * shieldH/2 / fSh;    	

    	// Normalization to the width to generate a circle shield:
    	// Circle positions
    	float ray     = shieldW/2.0f;
    	float ratio   = (float)shieldW / shieldH;
    	float centerX = ray;
    	float centerY = ray;
    	float focusX  = centerX + xAdj + shiftX;    	
    	float focusY  = centerY + (yAdj + shiftY)*ratio;
        Point2D center = new Point2D.Float(centerX, centerY);
        Point2D focus  = new Point2D.Float(focusX, focusY);


        Ellipse2D shieldArea = new Ellipse2D.Double(0, 0, shieldW, shieldW);
    	BufferedImage baseImg = null;
    	RadialGradientPaint paint;
        for (int i=0; i<nWindUpF; i++) {
        	int k = i+1;
        	// New shield frame
        	float[] distances = {0.0f, k*impactRay, k*spreadRay, 1.0f};
	        Color[] colors = {impactColor, impactColor, spreadColor, noColor};

	        paint = new RadialGradientPaint(center, shieldW/2, focus, distances, colors, NO_CYCLE);
	    	BufferedImage buffImg = new BufferedImage(shieldW, shieldW, TYPE_INT_ARGB);
	        Graphics2D g = (Graphics2D) buffImg.getGraphics();
	        g.setComposite(AlphaComposite.SrcOver);
	        g.setPaint(paint);
	        g.fill(shieldArea);
	        g.dispose();
	        
	        // rescale circle to ellipse
	        Image scaled = buffImg.getScaledInstance(shieldW, shieldH, Image.SCALE_SMOOTH);
	        // Add image to animation
	        shieldArr[i] = new BufferedImage(shieldW, shieldH, TYPE_INT_ARGB);
	        Graphics2D g2 = shieldArr[i].createGraphics();
	        g2.setComposite(AlphaComposite.SrcOver);
	        if (baseImg != null)
	        	g2.drawImage(baseImg, 0, 0, null);
	        g2.drawImage(scaled, 0, 0, null);
	        g2.dispose();
	        baseImg = shieldArr[i];
        }
	    // last frame goes around
    	float[] distances = {0.0f, nWindUpF*impactRay, 1-5*spreadDrEnd, 1-spreadDrEnd, 0.999f, 1.0f};
        Color[] colors = {noColor, noColor, spreadEndColor, spreadColor, spreadEndColor, noColor};
//        Color[] colors = {noColor, noColor, spreadColor, spreadEndColor, spreadEndColor, noColor};

        paint = new RadialGradientPaint(center, shieldW/2, focus, distances, colors, NO_CYCLE);
    	BufferedImage buffImg = new BufferedImage(shieldW, shieldW, TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buffImg.getGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(paint);
        g.fill(shieldArea);
        g.dispose();
        
        // rescale circle to ellipse
        Image scaled = buffImg.getScaledInstance(shieldW, shieldH, Image.SCALE_SMOOTH);
        // Add image to animation
        Graphics2D g2 = shieldArr[nWindUpF-1].createGraphics();
        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        
        return shieldArr;
    }
    public float[] shieldImpact(Dimension size, int srcX, int srcY, int tarX, int tarY, int xAdj, int yAdj) {
    	final float shieldDistanceFactor = 1.0f/3.0f /2;	// To estimate the shield impact position
    	int shieldW = size.width;
    	int shieldH = size.height;
    	
        // Impact on the ship positions
    	int shipImpactX = tarX + xAdj; // Ship center + aiming error
    	int shipImpactY = tarY + yAdj;
    	
    	// Beam trajectory
    	int beamRayDX = srcX - shipImpactX;
    	int beamRayDY = srcY - shipImpactY;
    	
    	// Normalized direction
    	float beamSpan = (float) Math.sqrt((beamRayDX*beamRayDX) + (beamRayDY*beamRayDY));
    	float normX = beamRayDX/beamSpan;
    	float normY = beamRayDY/beamSpan;
    	
    	// Shield Impact offset to shipImpact
       	float shieldAdjX = normX * shieldW * shieldDistanceFactor;
       	float shieldAdjY = normY * shieldH * shieldDistanceFactor;
       	
       	// Impact on the shield
       	float shieldImpactX = shipImpactX + shieldAdjX;
       	float shieldImpactY = shipImpactY + shieldAdjY;
       	
       	// Shield Impact offset to ship center
       	float shieldImpactDX = shieldImpactX - tarX;
       	float shieldImpactDY = shieldImpactY - tarY;
       	float veiledSpan  = distance(shieldImpactX, shieldImpactY, shipImpactX, shipImpactY);
       	float veiledRatio = veiledSpan/beamSpan;

//       	System.out.println("shieldImpactX: " + shieldImpactX);
//       	System.out.println("shieldImpactY: " + shieldImpactY);
//       	System.out.println("shieldImpactDX: " + shieldImpactDX);
//       	System.out.println("shieldImpactDY: " + shieldImpactDY);
//       	System.out.println("shieldAdjX: " + shieldAdjX);
//       	System.out.println("shieldAdjY: " + shieldAdjY);
//   		System.out.println("veiledSpan: " + veiledSpan);
//   		System.out.println("veiledRatio: " + veiledRatio);
       	
       	return new float[] {
    			shieldImpactX,  shieldImpactY,
    			shieldImpactDX, shieldImpactDY,
    			shieldAdjX,     shieldAdjY,
    			veiledSpan,     veiledRatio};
    }
    public BufferedImage[] shieldImages(int nWindUpF, int nHoldF, int nCloseF, int nAttacks,
    		Dimension size, int srcX, int srcY, int tarX, int tarY, int xAdj, int yAdj,
    		Color beamColor, float beamForce, float damage, float[] shieldImpact) { // TODO BR: BufferedImage[] shieldImg(

    	// Impact Radius is function of beam power (^)
    	// Impact Transparency is function of beam absorption ratio (v)
    	// Spreading Radius is function of beam power (^) and shield level (v)
    	// Spreading Transparency is function of beam absorption ratio (v) and and shield level (v)

    	final float topShield = 15f;
    	final float alphaImpactMin = 0.5f;
    	final float alphaImpactMax = 1.0f;
    	final float alphaSpreadMin = 0.5f;
    	final float alphaSpreadMax = 1.0f;
    	final float alphaCloseMin  = 0.5f;
    	final float alphaCloseMax  = 0.1f;
    	final float alphaEndDiv    = 2.0f;
        final float impactRbase    = 0.05f;
        final float impactRmin     = 0.20f;
        final float impactRmax     = 0.50f;
        final float spreadDrEnd    = 0.05f;
        final float spreadDrMin    = 0.2f;
        final float spreadingRmax  = 0.95f - spreadDrEnd;
        final float edgeRay        = 1.0f - spreadDrEnd;
        final float edgeRay_       = edgeRay - spreadDrEnd;

    	final float impactFadeFactor = 1.2f;
    	final float impactAlphaRatio = 0.7f;
    	final float spreadFadeFactor = 1.5f;

        // Elliptic positions (as opposed to the circularized ones)
//    	float shieldImpactX  = shieldImpact[0];  	
//    	float shieldImpactY  = shieldImpact[1];  	
       	float shieldImpactDX = shieldImpact[2];
       	float shieldImpactDY = shieldImpact[3];
    	int shieldW = size.width;
    	int shieldH = size.height;

    	
    	
//    	int nFrame = 1 + nHoldF + nWindUpF + nCloseF;
    	int nFrame = nWindUpF;
    	BufferedImage[] shieldArr = new BufferedImage[nFrame]; // TODO BR: shieldArr
    	
        float shieldForce = maxShield / topShield;
        float beamPowerFactor = 0;
        float absorptionRatio = 0;       
        if (beamForce > 0) {
        	beamPowerFactor = (float) Math.log10(beamForce);
            absorptionRatio = damage>0? 1 - (damage/beamForce):1;
        }
        // Transparencies: Transparent = 0; Opaque = 1
        float spreadingFactor = bounds(0, sqrt(beamPowerFactor * absorptionRatio), 1);
        float alphaImpact = alphaImpactMin + absorptionRatio * (alphaImpactMax-alphaImpactMin);
        float alphaSpread = alphaSpreadMin + spreadingFactor * (alphaSpreadMax-alphaSpreadMin);
        // Rays
        spreadingFactor = bounds(0, sqrt(shieldForce * absorptionRatio), 1);
        float impactRay = bounds(impactRmin, impactRbase*beamPowerFactor, impactRmax);
        float spreadingRayMin = impactRay + spreadDrMin;
        float spreadRay =  spreadingRayMin + spreadingFactor * (spreadingRmax-spreadingRayMin);
        impactRay /= nWindUpF;
        spreadRay /= nWindUpF;
        float alphaEnd = min(1f, alphaSpread/alphaEndDiv);
    	// Colors
        Color noColor =  new Color(0f,0f,0f,0f);
        // Impact color based on BeamWeapon
        Color impactColorStart = saturateColor(beamColor, alphaImpact);
        Color impactColorEdge  = setAlpha(impactColorStart, alphaImpact*impactAlphaRatio);
    	// Spread color based on Target color
        Color spreadColor = saturateColor(shieldBaseColor(), alphaSpread);
        Color spreadEndColor = setAlpha(spreadColor, alphaEnd);
        
    	

    	// Normalization to the width to generate a circle shield:
    	// Circle positions
    	float ellipseRatio = (float)shieldW / shieldH;
    	float ray      = shieldW/2.0f;
    	float centerX  = ray;
    	float centerY  = ray;
    	float focusX   = centerX + shieldImpactDX;    	
    	float focusY   = centerY + (shieldImpactDY)*ellipseRatio;
        Point2D center = new Point2D.Float(centerX, centerY);
        Point2D focus  = new Point2D.Float(focusX, focusY);


        Ellipse2D shieldArea = new Ellipse2D.Double(0, 0, shieldW, shieldW);
//        BufferedImage baseImg = null;
    	RadialGradientPaint paint;
    	float slideF = 0.0f/nWindUpF;
        for (int i=0; i<nWindUpF; i++) {
        	int k = i+1;
//        	float r1 = k * impactRay;
//        	float r2 = k * spreadRay;
//        	float[] distances = {0.0f,          r1,               r2,          1.0f};
//	        Color[] colors = {impactColorStart, impactColorStart, spreadColor, noColor};
//	        System.out.println("distances = " + distances);

//	        paint = new RadialGradientPaint(center, shieldW/2, focus, distances, colors, NO_CYCLE);
//	    	BufferedImage buffImg = new BufferedImage(shieldW, shieldW, TYPE_INT_ARGB);
//	        Graphics2D g = (Graphics2D) buffImg.getGraphics();
//	        g.setComposite(AlphaComposite.SrcOver);
//	        g.setPaint(paint);
//	        g.fill(shieldArea);
//	        g.dispose();
        	// New shield frame
        	// Impact
//        	r2i = r1i * impactFadeFactor;
//	        colors    = new Color[]{impactColorStart, impactColorEdge, noColor, noColor};
        	float r1 = k * impactRay;
        	float r2 = k * spreadRay;
	        float[] distances = new float[]{0.0f,             r1,               r2,      1.0f};
	        Color[] colors    = new Color[]{impactColorStart, impactColorStart, noColor, noColor};
	        paint = new RadialGradientPaint(center, ray, focus, distances, colors, NO_CYCLE);
//System.out.println("distances = " + distances);
	    	BufferedImage impactImg = new BufferedImage(shieldW, shieldW, TYPE_INT_ARGB);
	    	Graphics2D g = (Graphics2D) impactImg.getGraphics();
	        g.setComposite(AlphaComposite.SrcOver);
	        g.setPaint(paint);
	        g.fill(shieldArea);
	        g.dispose();
	        // Spreading
	        float dr = (r2-r1) * (0.49f-slideF);
        	float r3 = r1 + dr;
        	float r4 = r2 - dr;
//        	if (r4 < 1f) {
	        	distances = new float[]{0.0f,    r1,      r3,          r4,          r2,      1.0f};
	        	colors    = new Color[]{noColor, noColor, spreadColor, spreadColor, noColor, noColor};
//        	} else if (r3 < 1f) {
//           		r2 = min(r2, edgeRay_);
//           		r3 = min(r3, edgeRay_);
//	        	distances = new float[]{0.0f,    r2,      r3,          edgeRay ,    1.0f,           1.0f};
//	        	colors    = new Color[]{noColor, noColor, spreadColor, spreadColor, spreadEndColor, noColor};        		
//        	} else {
//        		r2 = min(r2, edgeRay_);
//	        	distances = new float[]{0.0f,    r2,      edgeRay ,       1.0f,           1.0f};
//	        	colors    = new Color[]{noColor, noColor, spreadEndColor, spreadEndColor, noColor};        		
//        	}
//System.out.println("distances Shield = " + distances);
			paint = new RadialGradientPaint(center, ray, focus, distances, colors, NO_CYCLE);
	    	BufferedImage ShieldImg = new BufferedImage(shieldW, shieldW, TYPE_INT_ARGB);
	        g = (Graphics2D) ShieldImg.getGraphics();
	        g.setComposite(AlphaComposite.SrcOver);
	        g.setPaint(paint);
	        g.fill(shieldArea);
	        g.dispose();
//System.out.println("RAYS = " + (int)(100*r1) + " / " + (int)(100*r3) + " / " + (int)(100*r4) + " / " + (int)(100*r2));
	        
	        // rescale circle to ellipse
	        // Add image to animation
	        shieldArr[i]  = new BufferedImage(shieldW, shieldH, TYPE_INT_ARGB);
	        g = shieldArr[i].createGraphics();
	        g.setComposite(AlphaComposite.SrcOver);
//	        if (baseImg != null)
//	        	g.drawImage(baseImg, 0, 0, null);
	        g.drawImage(impactImg.getScaledInstance(shieldW, shieldH, Image.SCALE_SMOOTH), 0, 0, null);
	        g.drawImage(ShieldImg.getScaledInstance(shieldW, shieldH, Image.SCALE_SMOOTH), 0, 0, null);
	        g.dispose();
//	        baseImg = shieldArr[i];
        }
//	    // last frame goes around
//    	float[] distances = {0.0f, nWindUpF*impactRay, 1-5*spreadDrEnd, 1-spreadDrEnd, 0.999f,         1.0f};
//        Color[] colors = {noColor, noColor,            spreadEndColor,  spreadColor,   spreadEndColor, noColor};
////        Color[] colors = {noColor, noColor, spreadColor, spreadEndColor, spreadEndColor, noColor};
//
//        paint = new RadialGradientPaint(center, ray, focus, distances, colors, NO_CYCLE);
//    	BufferedImage buffImg = new BufferedImage(shieldW, shieldW, TYPE_INT_ARGB);
//        Graphics2D g = (Graphics2D) buffImg.getGraphics();
//        g.setComposite(AlphaComposite.SrcOver);
//        g.setPaint(paint);
//        g.fill(shieldArea);
//        g.dispose();
//        
//        // rescale circle to ellipse
//        Image scaled = buffImg.getScaledInstance(shieldW, shieldH, Image.SCALE_SMOOTH);
//        // Add image to animation
//        Graphics2D g2 = shieldArr[nWindUpF-1].createGraphics();
//        g2.setComposite(AlphaComposite.SrcOver);
//        g2.drawImage(scaled, 0, 0, null);
//        g2.dispose();
        
        return shieldArr;
    }
    // = = = = = = = = = = = Nested Class ShieldEffect = = = = = = = = = =
    //
    
    public class ShieldEffects {
    
    	// Impact Radius is function of beam power (^)
    	// Impact Transparency is function of beam absorption ratio (v)
    	// Spreading Radius is function of beam power (^) and shield level (v)
    	// Spreading Transparency is function of beam absorption ratio (v) and and shield level (v)

    	private static final float topShieldLevel = 15f;
    	private static final float alphaImpactMin = 0.5f;
    	private static final float alphaImpactMax = 1.0f;
    	private static final float alphaSpreadMin = 0.5f;
    	private static final float alphaSpreadMax = 1.0f;
    	private static final float alphaCloseMin  = 0.5f;
    	private static final float alphaCloseMax  = 0.1f;
    	private static final float alphaEndDiv    = 2.0f;

    	private static final float rayImpactBase    = 0.05f;
    	private static final float rayImpactMin     = 0.20f;
    	private static final float rayImpactMax     = 0.50f;
    	private static final float rayEvSpreadEnd   = 0.05f;
    	private static final float rayEvSpreadMin   = 0.2f;
    	private static final float raySpreadingMax  = 0.95f - rayEvSpreadEnd;
    	private static final float rayEdgeEffect    = 1.0f - rayEvSpreadEnd;
    	private static final float rayEdgeEffectB   = rayEdgeEffect - rayEvSpreadEnd;

    	private static final float impactFadeFactor = 1.2f;
    	private static final float impactAlphaRatio = 0.3f;
    	private static final float spreadFadeFactor = 1.5f;

    	private static final float shieldDistanceFactor = 1.0f/3.0f /2;	// To estimate the shield impact position

    	private int windUpFramesNum, holdFramesNum, fadingFramesNum, framesNum;

//    	private Dimension shieldSize;
//    	private Rectangle sourceBox, targetBox;
    	private int   width,   height;
    	private int   sourceX, sourceY;
    	private int   targetX, targetY;
    	private float ray, ellipseRatio;
//    	private float aimingX, aimingY;
//    	private float shipImpactX,  shipImpactY;
//    	private float beamRaySpanX, beamRaySpanY;
//    	private float damage, beamForce;
//    	private int   attacksPerRound;
//    	private Color beamColor;
    	private int shieldX, shieldY; 

    	private float rayIo,   rayIm,   rayIe;
    	private float rayIoEv, rayImEv, rayIeEv;
    	private float raySb,   raySo,   raySm,   raySe;
    	private float raySbEv, raySoEv, raySmEv, raySeEv;
    	private float spreadRay;
    	private float veiledRatio;
    	private Color noCol, colIo, colIm, colSo, spreadEndColor;
//    	private BufferedImage[] shieldImages;
    	private Ellipse2D shieldArea;
    	
    	// = = = = = = = = = = Constructor = = = = = = = = = =
    	//
    	// None
    	
    	// = = = = = = = = = = Parameter setter = = = = = = = = = =
    	//
    	public void setFramesNum (int windUpFrames, int holdFrames, int fadingFrames) {
    		windUpFramesNum = windUpFrames;
    		holdFramesNum   = holdFrames;
    		fadingFramesNum = fadingFrames;
    		framesNum = 1+ 2*windUpFramesNum + holdFramesNum + fadingFramesNum;
    	}
    	public int[] setPositions(Rectangle sourceBox, Rectangle targetBox, Point2D offset) {
//    		this.sourceBox = sourceBox;
//    		this.targetBox = targetBox;
    		int boxWidth  = targetBox.width;
    		int boxHeight = targetBox.height;
    		Dimension shieldSize = shieldSize(boxWidth, boxHeight);
    		width  = shieldSize.width;
    		height = shieldSize.height;

        	ray = width/2.0f;
        	ellipseRatio = (float)width / height;
    		sourceX = sourceBox.x > targetBox.x ? sourceBox.x+(boxWidth/3) : sourceBox.x+(boxWidth*2/3);
    		sourceY = sourceBox.y + boxHeight/2;
//    		targetX = (float) targetBox.getCenterX();
//    		targetY = (float) targetBox.getCenterY();
    		targetX = targetBox.x + (int)(offset.getX()*boxWidth);;
    		targetY = targetBox.y + (int)(offset.getY()*boxHeight);
            shieldArea = new Ellipse2D.Double(0, 0, width, width);

            shieldX = (int) (targetX-ray);
            shieldY = (int) (targetY-height/2);
            return new int[] {
            		sourceX, sourceY,
            		targetX, targetY,
            		shieldX, shieldY
            		};
        }
    	public void setWeapons(int attacksPerRound, float damage, float beamForce, Color beamColor) {
//    		this.attacksPerRound = attacksPerRound;
//    		this.damage    = damage;
//    		this.beamForce = beamForce;
//    		this.beamColor = beamColor;
            float shieldForce = maxShield / topShieldLevel;
            float beamPowerFactor = 0;
            float absorptionRatio = 0;       
            if (beamForce > 0) {
            	beamPowerFactor = (float) Math.log10(beamForce);
                absorptionRatio = damage>0? 1 - (damage/beamForce):1;
            }
            // Transparencies: Transparent = 0; Opaque = 1
            float alphaSpreadingFactor = bounds(0, sqrt(beamPowerFactor * absorptionRatio), 1);
            float alphaImpact = alphaImpactMin + absorptionRatio * (alphaImpactMax-alphaImpactMin);
            float alphaSpread = alphaSpreadMin + alphaSpreadingFactor * (alphaSpreadMax-alphaSpreadMin);

            // Rays parameters
            // Impact
            float rayImpactMinAdj = max(rayImpactMin, (float)scaled(3)/width);
            float rayImpactMaxAdj = min(rayImpactMax, 2*rayImpactMinAdj);
            float raySpreadingFactor = bounds(0, sqrt(shieldForce * absorptionRatio), 1);
            rayIo   = bounds(rayImpactMinAdj, rayImpactBase*beamPowerFactor, rayImpactMax);
            rayIoEv = (1-rayImpactMax) / framesNum/4;
            rayIm   = rayIo * 1.4f;
            rayImEv = rayIoEv;
            rayIe   = rayIm * 1.4f;
            rayIeEv = rayImEv;

            // Shield counter effect
            float speedFactor = 1.1f;
            raySb   = rayIo;
            raySbEv = rayIoEv * speedFactor;
            raySo   = rayIm;
            raySoEv = rayIoEv * speedFactor;
            raySm   = rayIe;
            raySmEv = raySoEv * speedFactor;
            raySe   = rayIm *1.4f;
            raySeEv = raySmEv * speedFactor;
            
            float spreadingRayMin = rayIo + rayEvSpreadMin;
            spreadRay =  spreadingRayMin + raySpreadingFactor * (raySpreadingMax-spreadingRayMin);
            rayIo /= windUpFramesNum;
            spreadRay /= windUpFramesNum;
            float alphaEnd = min(1f, alphaSpread/alphaEndDiv);
        	// Colors
            noCol = new Color(0f,0f,0f,0f);
            // Impact color based on BeamWeapon
            colIo = saturateColor(beamColor, alphaImpact);
            colIm = setAlpha(colIo, alphaImpact*impactAlphaRatio);
        	// Spread color based on Target color
            colSo    = saturateColor(shieldBaseColor(), alphaSpread);
            spreadEndColor = setAlpha(colSo, alphaEnd);

    	}
    	// = = = = = = = = = = Parameter setter and Getter = = = = = = = = = =
    	//    	
    	public BufferedImageList getShields(float aimingErrorX, float aimingErrorY) {
//        	int nFrame = 1 + nHoldF + nWindUpF + nCloseF;
//        	int nFrame = windUpFramesNum;
        	BufferedImageList shieldList = new BufferedImageList(); // TODO BR: shieldArr
        	
            // Elliptic positions (as opposed to the circularized ones)
//    		float aimingX = random.nextFloat()*8-4;
//    		float aimingY = random.nextFloat()*8-4;
    		float shipImpactX = targetX + aimingErrorX;  // Ship center + aiming error
    		float shipImpactY = targetY + aimingErrorY;
    		float beamSpanX   = sourceX - shipImpactX;
    		float beamSpanY   = sourceY - shipImpactY;

    		// Normalized direction vector
    		float beamSpan = (float) Math.sqrt((beamSpanX*beamSpanX) + (beamSpanY*beamSpanY));
        	float normX = beamSpanX/beamSpan;
        	float normY = beamSpanY/beamSpan;
        	// Shield Impact offset to shipImpact
           	float shieldAdjX = normX * width  * shieldDistanceFactor;
           	float shieldAdjY = normY * height * shieldDistanceFactor;
           	// Impact on the shield
           	float shieldImpactX = shipImpactX + shieldAdjX;
           	float shieldImpactY = shipImpactY + shieldAdjY;
          	// Shield Impact offset to ship center
           	float shieldImpactDX = shieldImpactX - targetX;
           	float shieldImpactDY = shieldImpactY - targetY;
           	float veiledSpan  = distance(shieldImpactX, shieldImpactY, shipImpactX, shipImpactY);
           	veiledRatio = veiledSpan/beamSpan;

        	// Normalization to the width to generate a circle shield:
        	// Circle positions
        	float focusX   = ray + shieldImpactDX;    	
        	float focusY   = ray + (shieldImpactDY)*ellipseRatio;
            Point2D center = new Point2D.Float(ray, ray);
            Point2D focus  = new Point2D.Float(focusX, focusY);

            // = = = On Impact 
            
//            BufferedImage baseImg = null;
        	RadialGradientPaint paint;
        	float slideF = 0.0f/windUpFramesNum;
            float rIo = rayIo - rayIoEv;
            float rIm = rayIm - rayImEv;
            float rIe = rayIe - rayIeEv;
            float rSb = raySb - raySbEv;
            float rSo = raySo - raySoEv;
            float rSm = raySm - raySmEv;
            float rSe = raySe - raySeEv;

            for (int i=0; i<framesNum; i++) {
                rIo = min(1, rIo + rayIoEv);
                rIm = min(1, rIm + rayImEv);
                rIe = min(1, rIe + rayIeEv);
                rSb = min(1, rSb + raySbEv);
                rSo = min(1, rSo + raySoEv);
                rSm = min(1, rSm + raySmEv);
                rSe = min(1, rSe + raySeEv);
    	        float[] distances = new float[]{0.0f,  rIo,   rIm,   rIe,   1.0f};
    	        Color[] colors    = new Color[]{colIo, colIo, colIm, noCol, noCol};
    	    	BufferedImage impactImg = new BufferedImage(width, width, TYPE_INT_ARGB);
    	    	Graphics2D g = (Graphics2D) impactImg.getGraphics();
    	        g.setComposite(AlphaComposite.Src);
    	        paint = new RadialGradientPaint(center, ray, focus, distances, colors, NO_CYCLE);
    	        g.setPaint(paint);
    	        g.fill(shieldArea);
    	        g.dispose();

    	        // Spreading
    	        distances = new float[]{0.0f,  rSb,   rSo,   rSm,   rSe,   1.0f};
	        	colors    = new Color[]{noCol, noCol, colSo, colSo, noCol, noCol};
    	    	BufferedImage ShieldImg = new BufferedImage(width, width, TYPE_INT_ARGB);
    	        g = (Graphics2D) ShieldImg.getGraphics();
    	        g.setComposite(AlphaComposite.Src);
    			paint = new RadialGradientPaint(center, ray, focus, distances, colors, NO_CYCLE);
    	        g.setPaint(paint);
    	        g.fill(shieldArea);
    	        g.dispose();
    	        
    	        // rescale circle to ellipse
    	        // Add image to animation
    	        BufferedImage shieldFrame  = new BufferedImage(width, height, TYPE_INT_ARGB);
    	        g = shieldFrame.createGraphics();
    	        g.setComposite(AlphaComposite.SrcOver);
    	        g.drawImage(impactImg.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
    	        g.drawImage(ShieldImg.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
    	        g.dispose();
    	        shieldList.add(shieldFrame);
            }
//    	    // last frame goes around
//        	float[] distances = {0.0f, windUpFramesNum*impactRay, 1-5*spreadDrEnd, 1-spreadDrEnd, 0.999f,         1.0f};
//            Color[] colors = {noColor, noColor,            spreadEndColor,  spreadColor,   spreadEndColor, noColor};
////            Color[] colors = {noColor, noColor, spreadColor, spreadEndColor, spreadEndColor, noColor};
    //
//            paint = new RadialGradientPaint(center, ray, focus, distances, colors, NO_CYCLE);
//        	BufferedImage buffImg = new BufferedImage(width, width, TYPE_INT_ARGB);
//            Graphics2D g = (Graphics2D) buffImg.getGraphics();
//            g.setComposite(AlphaComposite.SrcOver);
//            g.setPaint(paint);
//            g.fill(shieldArea);
//            g.dispose();
//            
//            // rescale circle to ellipse
//            Image scaled = buffImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
//            // Add image to animation
//            Graphics2D g2 = shieldArr[windUpFramesNum-1].createGraphics();
//            g2.setComposite(AlphaComposite.SrcOver);
//            g2.drawImage(scaled, 0, 0, null);
//            g2.dispose();
            
            return shieldList;
    	}
    	// = = = = = = = = = = Parameter Getter = = = = = = = = = =
    	//
    	public float getVeiledRatio() { return veiledRatio; }

    }
}
