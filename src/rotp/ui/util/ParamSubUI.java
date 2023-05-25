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

import static rotp.ui.util.InterfaceParam.langLabel;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedList;

import rotp.model.game.IGameOptions;
import rotp.ui.game.BaseModPanel;
import rotp.ui.game.CompactOptionsUI;


public class ParamSubUI extends AbstractParam<LinkedList<LinkedList<InterfaceParam>>> {
	
	private final String GUI_TITLE_ID;
	private final String GUI_ID;
	private CompactOptionsUI ui;
	public  final LinkedList<LinkedList<InterfaceParam>> optionsMap;
	public  final LinkedList<InterfaceParam> optionsList = new LinkedList<>();
	
	// ===== Constructors =====
	//
	/**
	 * @param gui  The label header
	 * @param name The name
	 * @param optionsMap Full map of options
	 * @param guiTitleID Label for the GUI Title
	 * @param guiID Unique GUI ID for load and save
	 */
	public ParamSubUI(String gui, String name,
			LinkedList<LinkedList<InterfaceParam>> optionsMap,
			String guiTitleID, String guiID)
	{
		super(gui, name, optionsMap);
		GUI_TITLE_ID = gui + guiTitleID;
		GUI_ID = guiID;
		this.optionsMap = optionsMap;
		for (LinkedList<InterfaceParam> list : optionsMap) {
			for (InterfaceParam param : list) {
				if (param != null && !param.isTitle())
					optionsList.add(param);
			}
		}
	}
	// ===== Overriders =====
	//
	@Override public boolean isDefaultValue()	{ 
		boolean is = true;
		for (InterfaceParam param : optionsList)
			is &= param.isDefaultValue();
		return is;
	}
	@Override public void copyOption(IGameOptions src, IGameOptions dest) {
		super.copyOption(src, dest);
		for (InterfaceParam param : optionsList)
			param.copyOption(src, dest);
	}
	@Override public void updateOption() {
		super.updateOption();
		for (InterfaceParam param : optionsList)
			param.updateOption();
	}
	@Override public void updateOptionTool() {
		super.updateOptionTool();
		for (InterfaceParam param : optionsList)
			param.updateOptionTool();
	}
	@Override public void setFromDefault() {
		for (InterfaceParam param : optionsList)
			param.setFromDefault();
	}
	@Override protected LinkedList<LinkedList<InterfaceParam>> getOptionValue(
			IGameOptions options) {
		return get();
	}
	@Override protected void setOptionValue(IGameOptions options,
			LinkedList<LinkedList<InterfaceParam>> value) {}
	@Override public void setFromCfgValue(String val) {
		for (InterfaceParam param : optionsList)
			param.setFromCfgValue(val);
	}
	@Override public void next() {  }
	@Override public void prev() {  }
	@Override public void toggle(MouseWheelEvent e) { }
	@Override public void toggle(MouseEvent e, BaseModPanel frame) { }
	@Override public void toggle(MouseEvent e, int p, BaseModPanel frame) { ui().start(p); };
	@Override public String guideValue()	{
		String label = isDefaultValue()? "SUB_UI_DEFAULT_YES" : "SUB_UI_DEFAULT_NO";
		return langLabel(label);
	}
	@Override public String getCfgValue()	{ return super.getCfgValue(); }
	@Override public String getCfgLabel()	{ return super.getCfgLabel(); }
	@Override public boolean isSubMenu()	{ return true; }
	@Override public String getHeadGuide()	{ return headerHelp(true); }

	// ===== Other Methods =====
	//
	private CompactOptionsUI ui() {
		if (ui == null)
			ui = new CompactOptionsUI(GUI_TITLE_ID, GUI_ID, optionsMap);
		return ui;
	}
	public LinkedList<InterfaceParam> optionsList() { return optionsList; }
}
