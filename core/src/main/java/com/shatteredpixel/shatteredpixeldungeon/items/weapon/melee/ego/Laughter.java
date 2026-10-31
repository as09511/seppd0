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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//ALEPH, same as the glaive, eating whatever it kills to restore some health and hunger
public class Laughter extends EgoWeapon {

	private static final float HUNGER_RESTORED = 15f;

	{
		image = ItemSpriteSheet.LAUGHTER;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;

		risk = RiskLevel.ALEPH;
		damageType = DamageType.BLACK;
		tier = 5;
		DLY = 1.5f; //0.67x speed
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  Math.round(6.67f*(tier+1)) +    //40 base, up from 30
				lvl*Math.round(1.33f*(tier+1)); //+8 per level, up from +6
	}

	public int healOnKill(int lvl){
		return 10 + 2*lvl;
	}

	@Override
	public void onKill(Hero hero, Char killed) {
		healAttacker(hero, healOnKill(buffedLvl()));
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) hunger.satisfy(HUNGER_RESTORED);
	}

	@Override
	protected int abilityBoost(int lvl) {
		return 12 + Math.round(2.5f*lvl);
	}

	//same as the glaive
	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Spear.spikeAbility(hero, target, 1, dmgBoost, this);
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", healOnKill(levelKnown ? buffedLvl() : 0));
	}
}
