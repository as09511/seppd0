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
package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//Gebura's cigarettes, from her cigarette time talent: smoking one costs 10% of max health
// (never below 1) and gives 10 turns of adrenaline
public class Cigarette extends Item {

	public static final String AC_SMOKE = "SMOKE";

	public static final float HEALTH_COST = 0.1f;
	public static final float DURATION = 10f;

	{
		image = ItemSpriteSheet.CIGARETTE;
		stackable = true;
		defaultAction = AC_SMOKE;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SMOKE);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);
		if (action.equals(AC_SMOKE)){
			detach(hero.belongings.backpack);
			smoke(hero);
			hero.sprite.operate(hero.pos);
			hero.spendAndNext(1f);
			updateQuickslot();
		}
	}

	public static void smoke( Hero hero ){
		int cost = Math.min(Math.round(hero.HT * HEALTH_COST), hero.HP - 1);
		if (cost > 0){
			hero.HP -= cost;
			hero.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(cost));
		}
		Buff.affect(hero, Adrenaline.class, DURATION);
		CellEmitter.get(hero.pos).burst(Speck.factory(Speck.SMOKE), 4);
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.5f, 1.4f);
		GLog.i(Messages.get(Cigarette.class, "smoke"));
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 5 * quantity;
	}
}
