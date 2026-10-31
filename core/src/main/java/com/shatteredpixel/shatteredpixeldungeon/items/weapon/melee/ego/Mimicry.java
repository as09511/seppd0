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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//ALEPH, same as the greatsword, heals the wielder for a quarter of the damage dealt
public class Mimicry extends EgoWeapon {

	private static final float LIFESTEAL = 0.25f;

	{
		image = ItemSpriteSheet.MIMICRY;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		risk = RiskLevel.ALEPH;
		damageType = DamageType.RED;
		tier = 5;
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		int dealt = Math.min(damage, defender.HP + defender.shielding());
		healAttacker(attacker, Math.round(dealt * LIFESTEAL));
		return damage;
	}

	@Override
	protected int baseChargeUse(Hero hero, Char target){
		if (hero.buff(Sword.CleaveTracker.class) != null){
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	protected int abilityBoost(int lvl) {
		return 7 + lvl;
	}

	//same as the greatsword
	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Sword.cleaveAbility(hero, target, 1, dmgBoost, this);
	}
}
