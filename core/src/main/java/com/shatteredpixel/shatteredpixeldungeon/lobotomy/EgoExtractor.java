/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ego.EgoArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego.EgoWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//testing tool, creates any E.G.O equipment: identified, uncursed, unenchanted and +0
public class EgoExtractor extends Item {

	public static final String AC_EXTRACT = "EXTRACT";

	{
		image = ItemSpriteSheet.CRYSTAL_CHEST;
		defaultAction = AC_EXTRACT;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_EXTRACT);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_EXTRACT)){
			chooseRisk(hero);
		}
	}

	private static RiskLevel riskOf(Item item){
		if (item instanceof EgoWeapon) return ((EgoWeapon) item).risk();
		if (item instanceof EgoArmor) return ((EgoArmor) item).risk();
		return null;
	}

	//every E.G.O of the given risk level, in catalog order
	private static ArrayList<Item> egoOf(RiskLevel risk){
		ArrayList<Item> result = new ArrayList<>();
		for (Class<? extends Item> cls : EgoCatalog.ALL){
			Item item = Reflection.newInstance(cls);
			if (riskOf(item) == risk) result.add(item);
		}
		return result;
	}

	//first pick a risk level, so the list does not grow too long
	private void chooseRisk(Hero hero){
		final ArrayList<RiskLevel> risks = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();
		for (RiskLevel risk : RiskLevel.values()){
			int count = egoOf(risk).size();
			if (count > 0){
				risks.add(risk);
				names.add(risk.title() + " (" + count + ")");
			}
		}

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()), Messages.get(this, "choose_risk"), names.toArray(new String[0])){
			@Override
			protected void onSelect(int index) {
				chooseEgo(hero, risks.get(index));
			}
		});
	}

	private void chooseEgo(Hero hero, RiskLevel risk){
		final ArrayList<Item> options = egoOf(risk);
		String[] names = new String[options.size()+1];
		for (int i = 0; i < options.size(); i++){
			names[i] = Messages.titleCase(options.get(i).name());
		}
		names[options.size()] = Messages.get(this, "back");

		GameScene.show(new WndOptions(new ItemSprite(this), Messages.titleCase(name()) + " - " + risk.title(), Messages.get(this, "choose"), names){
			@Override
			protected void onSelect(int index) {
				if (index == options.size()){
					chooseRisk(hero);
					return;
				}
				Item item = options.get(index).identify();
				if (!item.collect()){
					Dungeon.level.drop(item, hero.pos).sprite.drop();
				}
				GLog.p(Messages.get(EgoExtractor.class, "extracted", item.name()));
			}

			@Override
			protected boolean hasIcon(int index) {
				return index < options.size();
			}

			@Override
			protected Image getIcon(int index) {
				return new ItemSprite(options.get(index));
			}
		});
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

}
