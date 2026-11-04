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
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndEgoExtract;
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

	//the same paged list as Angela's extraction, but free and with every E.G.O, combined ones included
	private void chooseRisk(Hero hero){
		GameScene.show(new WndEgoExtract(new WndEgoExtract.Picker() {
			@Override
			public boolean includes(Item item) {
				return riskOf(item) != null;
			}

			@Override
			public String title() {
				return Messages.titleCase(name());
			}

			@Override
			public String riskTitle(RiskLevel risk) {
				return risk.title();
			}

			@Override
			public void pick(WndEgoExtract wnd, Class<? extends Item> cls, Item shown) {
				Item item = Reflection.newInstance(cls).identify();
				if (!item.collect()){
					Dungeon.level.drop(item, hero.pos).sprite.drop();
				}
				GLog.p(Messages.get(EgoExtractor.class, "extracted", item.name()));
			}
		}));
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
