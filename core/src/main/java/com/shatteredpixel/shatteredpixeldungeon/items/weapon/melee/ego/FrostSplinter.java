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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//HE, a spear with a built-in chilling effect on top of any enchantment
public class FrostSplinter extends EgoWeapon {

	{
		image = ItemSpriteSheet.FROST_SPLINTER;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.1f;

		risk = RiskLevel.HE;
		damageType = DamageType.WHITE;
		tier = 3;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  16 +            //16 base, down from 20
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		//works exactly like the chilling enchantment
		return new Chilling().proc(this, attacker, defender, damage);
	}

	@Override
	public String statsInfo() {
		return super.statsInfo() + "\n" + Messages.get(Chilling.class, "desc");
	}

	@Override
	protected int abilityBoost(int lvl) {
		//same as the spear
		return 9 + 2*lvl;
	}

	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Spear.spikeAbility(hero, target, 1, dmgBoost, this);
	}

}
