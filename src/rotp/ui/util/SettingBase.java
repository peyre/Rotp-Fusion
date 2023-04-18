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

package rotp.ui.util;

import static rotp.ui.UserPreferences.minListSizePopUp;
import static rotp.util.Base.random;
import static rotp.util.Base.textSubs;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import rotp.model.game.DynamicOptions;
import rotp.ui.BaseText;
import rotp.ui.RotPUI;
import rotp.ui.game.BaseModPanel;
import rotp.ui.main.SystemPanel;
import rotp.util.LabelManager;

public class SettingBase<T> implements InterfaceParam {
	
	public enum CostFormula {DIFFERENCE, RELATIVE, NORMALIZED}
	public static final Color settingPosC = SystemPanel.limeText;  // Setting name color
	public static final Color settingNegC = SystemPanel.redText;   // Setting name color
	public static final Color settingC	  = SystemPanel.whiteText; // Setting name color

	private static final boolean defaultIsList			= true;
	private static final boolean defaultIsBullet		= false;
	private static final boolean defaultLabelsAreFinals	= false;
	private static final String  costFormat				= "%6s ";

	private final LinkedList<String> cfgValueList = new LinkedList<>();
	private final LinkedList<String> labelList	  = new LinkedList<>();
	private final LinkedList<Float>	 costList	  = new LinkedList<>();
	private final LinkedList<T> 	 valueList	  = new LinkedList<>();
	private final LinkedList<String> tooltipList  = new LinkedList<>();
	private final String nameLabel;
	private final String guiLabel;

	private boolean labelsAreFinals = defaultLabelsAreFinals;
	private boolean isList			= defaultIsList;
	private boolean isBullet		= defaultIsBullet;
	private boolean allowListSelect	= false;
	private T selectedValue	  = null;
	private T defaultValue	  = null;
	private boolean isSpacer  = false;
	private boolean hasNoCost = false;
	private BaseText settingText;
	private BaseText[] optionsText;
	private String settingToolTip;
	private int bulletHeightFactor = 1;
	private int bulletMax   = 25;
	private int bulletStart = 0;
	private float lastRandomSource;
	
	// ========== Constructors and initializers ==========
	//
	/**
	 * @param guiLabel		The label header
	 * @param nameLabel		The nameLabel
	 * @param defaultIndex	The default list index
	 * @param isList		Either a list or simple value
	 * @param isBullet		To be displayed as bullet list
	 * @param labelsAreFinals when false: Labels are combined withName and Gui Label
	 */
	SettingBase(String guiLabel, String nameLabel, T defaultValue,
			boolean isList, boolean isBullet, boolean labelsAreFinals) {
		this(guiLabel, nameLabel);
		this.defaultValue	= defaultValue;
		this.isList			= isList;
		this.isBullet		= isBullet;
		this.labelsAreFinals= labelsAreFinals;
	}
	/**
	 * @param guiLabel  The label header
	 * @param nameLabel The nameLabel
	 */
	public SettingBase(String guiLabel, String nameLabel) {
		this.guiLabel	= guiLabel;
		this.nameLabel	= nameLabel;
	}
	public void settingText(BaseText settingText) {
		this.settingText = settingText;
	}
	protected void maxBullet(int maxBullet) {
		bulletMax = maxBullet;
	}
	void settingToolTip(String settingToolTip) {
		this.settingToolTip = settingToolTip;
	}
	private void loadSettingToolTip() {
		String label = getLangLabel() + LABEL_DESCRIPTION;
		settingToolTip = text(getLangLabel() + LABEL_DESCRIPTION);
		if (label.equals(settingToolTip))
			settingToolTip = "";
	}
	private void optionsText(BaseText[] optionsText) {
		this.optionsText = optionsText;
	}
	public void optionText(BaseText optionText, int i) {
		optionsText[i] = optionText;
	}
	public SettingBase<?> initOptionsText() {
		if (bulletBoxSize() > 0)
			optionsText(new BaseText[bulletBoxSize()]);
		return this;
	}
	public SettingBase<?> isSpacer(boolean isSpacer) {
		this.isSpacer = isSpacer;
		return this;
	}
	public SettingBase<?> hasNoCost(boolean hasNoCost) {
		this.hasNoCost = hasNoCost;
		return this;
	}
	public SettingBase<?> isBullet(boolean isBullet) {
		this.isBullet = isBullet;
		return this;
	}
	public SettingBase<?> allowListSelect(boolean allowListSelect) {
		this.allowListSelect = allowListSelect;
		return this;
	}
	public SettingBase<?> isList(boolean isList) {
		this.isList = isList;
		return this;
	}
	public SettingBase<?> labelsAreFinals(boolean labelsAreFinals) {
		this.labelsAreFinals = labelsAreFinals;
		return this;
	}
	public SettingBase<?> defaultValue(T value) {
		defaultValue = value;
		return this;
	}
	protected SettingBase<T> bulletHeightFactor(int bulletHeightFactor) {
		this.bulletHeightFactor = bulletHeightFactor;
		return this;
	}
	// ========== Public Interfaces ==========
	//
	@Override public void setFromCfgValue(String cfgValue) {
		int index = cfgValidIndex(indexOfIgnoreCase(cfgValue, cfgValueList));
		selectedValue(valueList.get(index));
	}
	@Override public void next() {
		int selectedIndex = cfgValidIndex()+1;
		if (selectedIndex >= cfgValueList.size())
			selectedIndex = 0;
		selectedValue(valueList.get(selectedIndex));		
	}
	@Override public void prev() {
		int selectedIndex = cfgValidIndex()-1;
		if (selectedIndex < 0)
			selectedIndex = cfgValueList.size()-1;
		selectedValue(valueList.get(selectedIndex));		
	}
	@Override public void toggle(MouseEvent e, MouseWheelEvent w, BaseModPanel frame) {
		if (e == null)
			toggle(w);
		else
			toggle(e, frame);
	}
	@Override public void toggle(MouseWheelEvent w) {
		if (getDir(w) > 0)
			next();
		else 
			prev();
	}
	@Override public void toggle(MouseEvent e, BaseModPanel frame) {
		if (getDir(e) == 0) 
			setFromDefault();
		else if (allowListSelect && frame != null && 
				(e.isControlDown() || listSize() >= minListSizePopUp.get()))
			setFromList(frame);
		else if (getDir(e) > 0)
			next();
		else 
			prev();
	}
	@Override public void setFromDefault()		{ selectedValue(defaultValue); }
	@Override public void setOptions(DynamicOptions destOptions) {
		if (!isSpacer && destOptions != null)
			destOptions.setString(getLangLabel(), getCfgValue());
	}
	@Override public void setFromOptions(DynamicOptions srcOptions) {
		if (!isSpacer && srcOptions != null)
			setFromCfgValue(srcOptions.getString(getLangLabel(), getDefaultCfgValue()));
	}
	@Override public void copyOption(DynamicOptions src, DynamicOptions dest) {
		if (!isSpacer && src != null && dest != null)
			dest.setString(getLangLabel(), getCfgValue());
		dest.setString(getLangLabel(), src.getString(getLangLabel(), getDefaultCfgValue()));
	}
	@Override public String getGuiDisplay(int idx)	{
		String str = text(getLangLabel()); // Get from label.txt
		String[] strArr = str.split(textSubs[0]);

		switch(idx) {
		case 0:
			if (strArr.length > 0)
				return strArr[0];
			else
				return "";
		case 1:
			if (strArr.length > 1)
				return guideValue() + strArr[1];
			else
				return guideValue();
		default:
			return "";
		}
	}
	@Override public String getCfgValue() 		{ return getCfgValue(settingValue()); }
	@Override public String getCfgLabel()		{ return nameLabel; }
	@Override public String getGuiDescription() { return lmText(descriptionId()); }
	@Override public String guideValue()		{ return String.valueOf(settingValue()); }
	@Override public String getGuiDisplay()		{ return text(getLangLabel(), guideValue()) + END; }
	@Override public String getToolTip()		{
		if (settingToolTip == null) {
			loadSettingToolTip();
			resetOptionsToolTip();
		}
		return settingToolTip;
	}
	@Override public String getToolTip(int idx) {
		if (idx >= tooltipList.size())
			return "";
		String tt = tooltipList.get(idx);
		if (tt == null)
			return "";
		return tt;
	}
	@Override public String guideDefaultValue()	{ return defaultValue.toString(); }
	@Override public boolean isDefaultValue()	{ return defaultValue() == settingValue(); }
	// ========== Overridable Methods ==========
	//
	public void enabledColor(float cost) {
		if (cost == 0) 
			settingText().enabledC(settingC);
		else if (cost > 0)
			settingText().enabledC(settingPosC);
		else
			settingText().enabledC(settingNegC);	
	}
	void resetOptionsToolTip() {}
	protected String getCfgValue(T value) {
		if (isList) {
			int index = valueValidIndex(valueList.indexOf(value));
			return cfgValueList.get(index);
		}
		return String.valueOf(value);
	}
	// return true if needs to repaint
	public boolean toggle(MouseEvent e, MouseWheelEvent w, int idx) { // For bullet
		if (e == null) { // Mouse Wheel Event
			if (getDir(w) > 0) { // prev
				if (bulletStart > 0) {
					bulletStart(bulletStart-1);
					repaint();
					return true;
				}
				return false;
			} else { // next
				if (listSize() > bulletEnd()) {
					bulletStart(bulletStart+1);
					repaint();
					return true;
				}
			}
			return false;
		} else { // Mouse Click
			optionalInput();
			index(idx);
			guiSelect();
			return false;
		}
	}
	public void optionalInput()	{}
	public void pushSetting()	{}
	public void pullSetting()	{}
	public void formatData(Graphics g, int maxWidth) {}
	public float maxValueCostFactor() {
		if (isList) {
			return Collections.max(costList);
		}
		return 0f;
	}
	public float minValueCostFactor() {
		if (isList) {
			return Collections.min(costList);
		}
		return 0f;
	}
	public void updateGui() {  repaint(); }
	public void repaint() { 
		if (isSpacer())
			return;
		settingText().repaint();
		int selectedIndex = cfgValidIndex();
		int bulletSize	= bulletBoxSize();
		for (int bulletIdx=0; bulletIdx < bulletSize; bulletIdx++) {
			int optionIdx = bulletStart + bulletIdx;
			optionText(bulletIdx).disabled(optionIdx == selectedIndex);
			optionText(bulletIdx).repaint();
		}
	}
	public float settingCost() {
		if (isSpacer() || hasNoCost)
			return 0f;;
		return costList.get(costValidIndex());
	}
	public T settingValue() {
		if (selectedValue == null)
			return defaultValue;
		else
			return selectedValue;
	}
	public SettingBase<?> set(T newValue) {
		if (isList) {
			selectedValue = newValue;
			selectedValue(valueList.get(valueValidIndex()));
		} else
			selectedValue(newValue);
		return this;
	}
	public String guiCostOptionStr(int idx) {
		return guiCostOptionStr(idx, 0);
	}
	public int index() { return cfgValidIndex(); }
	public void guiSelect() {
		if (isSpacer())
			return;
		pushSetting();
		updateGui();
	}
	public void setRandom(float min, float max, boolean gaussian) {
		set(randomize(min, max, gaussian));
	}
	public SettingBase<?> index(int newIndex) {
		selectedValue(valueList.get(cfgValidIndex(newIndex)));
		return this;
	}
	protected T randomize(float rand) {
		if (isList) {
			if (rand > 0)
				rand *= Collections.max(costList);
			else
				rand *= -Collections.min(costList);				
			return getValueFromCost(rand);
		}
		return null; // Should be overridden
	}
	protected T getValueFromCost(float cost) {
		if (isList) {
			int bestIdx = 0;
			float bestDev =  Math.abs(cost - costList.getFirst());
			for (int i=1; i<costList.size(); i++) {
				float dev = Math.abs(cost - costList.get(i));
				if (dev < bestDev) {
					bestIdx = i;
					bestDev = dev;
				}
			}
			return valueList.get(bestIdx);
		}
		return null; // Should be overridden
	}
	protected void selectedValue(T newValue) {
		selectedValue = newValue;
		if (isBullet && listSize()>bulletBoxSize()) {
			// center the value
			int boxSize	= bulletBoxSize();
			int start	= Math.max(0, index()-boxSize/2);
			int end		= Math.min(listSize(), start + boxSize);
			bulletStart(end - boxSize);
		}
	}
	// ========== Setter ==========
	//
	public void setRandom(float rand) {
		lastRandomSource = rand;
		set(randomize(rand));
	}
	public void setValueFromCost(float cost) {
		set(getValueFromCost(cost));
	}
	public void newCost(int index, float newCost) {
		costList.set(index, newCost);
	}
	public SettingBase<?> defaultIndex(int index) {
		setDefaultIndex(bounds(0, index, cfgValueList.size()-1));
		return this;
	}
	public SettingBase<?> defaultCfgValue(String defaultCfgValue) {
		setDefaultIndex(cfgValidIndex(indexOfIgnoreCase(defaultCfgValue, cfgValueList)));
		return this;
	}
	public String guiOptionValue(int index) { // For List
		return String.valueOf(optionValue(index));
	}
	protected void setFromLabel(String langLabel) {
		int index = cfgValidIndex(indexOfIgnoreCase(langLabel, labelList));
		// don't call selectedValue(T value) 
		// it'll mess with CustomRaceDefinitions.RaceList
		selectedValue = valueList.get(index);
	}
	// ===== Getters =====
	//
	protected T	defaultValue() { return defaultValue; }
	protected String guiOptionLabel() { return guiOptionLabel(index()); }
	protected String guiOptionLabel(int index) {
		return lmText(labelList.get(cfgValidIndex(index)));
	}
	protected String labelList(int index) {
		return labelList.get(cfgValidIndex(index));
	}
	public String guiSettingDisplayStr() {
		if (isBullet) 
			return guiSettingLabelCostStr();
		else
			return guiSettingLabelValueCostStr();		
	}
	public boolean isSpacer()	{ return isSpacer; }
	public boolean hasNoCost()	{ return hasNoCost; }
	public boolean isBullet()	{ return isBullet; }
	public String  getLabel()	{ return lmText(getLangLabel()); }
	public int bulletStart()	{ return bulletStart; }
	public int bulletEnd()		{ return bulletStart + bulletBoxSize(); }
	public int bulletHeightFactor()	{ return bulletHeightFactor; }
	public float lastRandomSource()	{ return lastRandomSource; }
	public boolean isDefaultIndex()	{ return cfgValidIndex() == rawDefaultIndex(); }
	public boolean allowListSelect(){ return allowListSelect; }
	public BaseText	settingText()	{ return settingText; }
	public BaseText[] optionsText()	{ return optionsText; }
	public BaseText optionText(int i) { return optionsText[i]; }
	public float	costFactor() {
		if (isList) {
			if (lastRandomSource<0)
				return -Collections.min(costList);
			else
				return Collections.max(costList);
		}
		if (settingCost()<0)
			return -Math.min(maxValueCostFactor(), minValueCostFactor());
		else
			return Math.max(maxValueCostFactor(), minValueCostFactor());
	}
	public int listSize() { return valueList.size(); }
	public int bulletBoxSize() {
		if (isBullet())
			return Math.min(listSize(), bulletMax);
		else
			return 0;
	}
	public LinkedList<String> getOptions(){
		LinkedList<String> list = new LinkedList<String>();
		list.addAll(cfgValueList);
		return list;
	}
	public LinkedList<String> getLabels(){
		LinkedList<String> list = new LinkedList<String>();
		list.addAll(labelList);
		return list;
	}
	// ===== Other Public Methods =====
	//
	/**
	 * Add a new Option with its Label
	 * @param cfgValue
	 * @param langLabel
	 * @param cost
	 * @param value
	 * @return this for chaining purpose
	 */
	public void put(String cfgValue, String langLabel, float cost, T value) {
		isList(true);
		cfgValueList.add(cfgValue);
		costList.add(cost);
		valueList.add(value);
		if (labelsAreFinals)
			labelList.add(langLabel);
		else
			labelList.add(getLangLabel() +"_"+ langLabel);
		tooltipList.add("");
	}
	public void put(String cfgValue, String langLabel, float cost, T value, String tooltipKey) {
		isList(true);
		cfgValueList.add(cfgValue);
		costList.add(cost);
		valueList.add(value);
		if (labelsAreFinals)
			labelList.add(langLabel);			
		else
			labelList.add(getLangLabel() +"_"+ langLabel);
		if (tooltipKey == null || tooltipKey.isEmpty())
			tooltipList.add("");
		else if (labelsAreFinals)
			tooltipList.add(lmText(tooltipKey));			
		else
			tooltipList.add(lmText(getLangLabel() +"_"+ tooltipKey));
	}
	public void put(T value, String tooltipKey) {
		cfgValueList.add("");
		costList.add(0f);
		valueList.add(value);
		labelList.add("");
		if (tooltipKey == null || tooltipKey.isEmpty())
			tooltipList.add("");
		else if (labelsAreFinals)
			tooltipList.add(lmText(tooltipKey));			
		else
			tooltipList.add(lmText(getLangLabel() +"_"+ tooltipKey));
	}
	protected int getDir(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) return -1;
		if (SwingUtilities.isLeftMouseButton(e)) return 1;
		return 0;
	}
	protected int getDir(MouseWheelEvent e) {
		if (e.getWheelRotation() < 0) return 1;
		return -1;
	}
	protected void clearLists() {
		cfgValueList.clear();
		labelList.clear();
		costList.clear();
		valueList.clear();
		tooltipList.clear();
	}
	protected T optionValue(int index)	{ return valueList.get(valueValidIndex(index)); }
	@Override public String getLangLabel()			{ return guiLabel + nameLabel; }
	// ========== Private Methods ==========
	//
	private void bulletStart(int start) {
		bulletStart = start;
		int idx = index();
		if(optionsText==null || optionsText[0]==null)
			return;
		for (int bulletIdx=0; bulletIdx < bulletBoxSize(); bulletIdx++) {
			int optionIdx = bulletStart + bulletIdx;
			optionText(bulletIdx).disabled(optionIdx == idx);
			optionText(bulletIdx).displayText(guiCostOptionStr(optionIdx));
		}
	}
	/**
	 * @param min Limit Value in %
	 * @param max Limit Value in %
	 * @param gaussian yes = smooth edges
	 * @return a randomized value
	 */
	private T randomize(float min, float max, boolean gaussian) {
		if (this.isSpacer)
			return null;
		if (hasNoCost && isList && !valueList.isEmpty()) {
			int rand = random.nextInt(valueList.size());
			return valueList.get(rand);
		}
		float rand;
		float mini = Math.min(min, max)/100;
		float maxi = Math.max(min, max)/100;
		if (gaussian)
			rand = (maxi + mini + (maxi-mini) * (float) random.nextGaussian())/2;
		else
			rand = mini + (maxi-mini) * (float) random.nextFloat();
		lastRandomSource = rand;
		return randomize(rand);
	}
	private float optionCost(int index)	{ return costList.get(index); }
	private String descriptionId()		{ return getLangLabel() + LABEL_DESCRIPTION; }
	private String getDefaultCfgValue() { return getCfgValue(defaultValue); }
	private String settingCostString()	{ return settingCostString(1); } // default decimal number
	private String settingCostString(int dec) { return costString(settingCost(), dec); }
	private String optionCostStringIdx(int idx, int dec) {
		return costString(optionCost(idx), dec);
	}
	private String guiSettingLabelCostStr() {
		if (hasNoCost)
			return getLabel();
		return getLabel() + ": " + settingCostString();
	}
	private String guiSettingLabelValueCostStr() {
		if (hasNoCost)
			return getLabel() + ": " + guideValue();
		return getLabel() + ": " + guideValue() + " " + settingCostString();
	}
	private String guiCostOptionStr(int idx, int dec) {
		if (hasNoCost)
			return guiOptionLabel(idx);
		String cost = String.format(costFormat,  optionCostStringIdx(idx, dec));
		return cost + guiOptionLabel(idx);
	}
	private void setDefaultIndex(int index) {
		defaultValue = valueList.get(cfgValidIndex(index));
	}
	private String costString(float cost, int dec) {
		String str = "(";
		switch (dec) {
		case 0:
			str += "" + Math.round(cost);
			break;
		case 2:
			str +=  new DecimalFormat("0.00").format(cost);
			break;
		case 3:
			str +=  new DecimalFormat("0.000").format(cost);
			break;
		default:
			str +=  new DecimalFormat("0.0").format(cost);
			break;
		}
		return str + ")";
	}
	private int bounds(int low, int val, int hi) {
		return Math.min(Math.max(low, val), hi);
	}
	private int cfgValidIndex() {
		return cfgValidIndex(rawSelectedIndex());
	}
	private int cfgValidIndex(int index) {
		if (index<0 || index>cfgValueList.size())
			return valueValidDefaultIndex();
		return index;
	}
	private int valueValidDefaultIndex() {
		return bounds(0, rawDefaultIndex(), valueList.size()-1);
	}
	private int valueValidIndex() {
		return valueValidIndex(rawSelectedIndex());
	}	
	private int valueValidIndex(int index) {
		if (index<0 || index>valueList.size())
			return valueValidDefaultIndex();
		return index;
	}	
	private int costValidDefaultIndex() {
		return bounds(0, rawDefaultIndex(), costList.size()-1);
	}
	private int rawSelectedIndex() {
		return valueList.indexOf(selectedValue);
	}
	private int rawDefaultIndex() {
		return valueList.indexOf(defaultValue);
	}
	private int costValidIndex() {
		return costValidIndex(rawSelectedIndex());
	}
	private int costValidIndex(int index) {
		if (index<0 || index>costList.size())
			return costValidDefaultIndex();
		return index;
	}
	protected static String lmText(String key) {
		return LabelManager.current().label(key);
	}
	private static String text(String key, String... vals) {
		String str = lmText(key);
		for (int i=0;i<vals.length;i++)
			str = str.replace(textSubs[i], vals[i]);
		return str;
	}
	private int indexOfIgnoreCase(String string, LinkedList<String> list) {
		int index = 0;
		for (String entry : list) {
			if (entry.equalsIgnoreCase(string))
				return index;
			index++;
		}
		return -1;
	}

	private int getValueIndexIgnoreCase(String value) {
		int index = 0;
		for (String entry : cfgValueList) {
			if (entry.equalsIgnoreCase(value))
				return index;
			index++;
		}
		return -1;
	}
	@SuppressWarnings("unchecked")
	private void setFromList(BaseModPanel frame) {
		String message	= "<html>" + getGuiDescription() + "</html>";
		String title	= text(getLangLabel(), "");
		// System.out.println("getIndex() = " + getIndex());
		// System.out.println("currentOption() = " + currentOption());

		String[] list = cfgValueList.toArray(new String[listSize()]);
		ListDialog dialog = new ListDialog(
				frame,	frame,					// Frame & Location component
				message, title,					// Message & Title
				list, selectedValue.toString(),	// List & Initial choice
				null, true,						// long Dialogue & isVertical
				RotPUI.scaledSize(360), RotPUI.scaledSize(300),	// size
				null, null,	// Font, Preview
				null, this);		// Alternate return

		String input = (String) dialog.showDialog();
		// System.out.println("input = " + input);
		if (input != null && getValueIndexIgnoreCase(input) >= 0)
			set((T) input);
		// System.out.println("getIndex() = " + getIndex());
	}
}
