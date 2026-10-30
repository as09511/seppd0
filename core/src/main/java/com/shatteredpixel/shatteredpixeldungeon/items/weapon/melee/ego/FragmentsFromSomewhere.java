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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//TETH
public class FragmentsFromSomewhere extends EgoWeapon {

	{
		image = ItemSpriteSheet.FRAGMENTS_FROM_SOMEWHERE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.2f;

		risk = RiskLevel.TETH;
		damageType = DamageType.BLACK;
		tier = 2;
		RCH = 2;    //extra reach
	}

	@Override
	public int max(int lvl) {
		return  12 +            //12 base, down from 15
				lvl*(tier+1);   //scaling unchanged
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
