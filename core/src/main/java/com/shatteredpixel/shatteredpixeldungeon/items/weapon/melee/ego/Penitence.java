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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

//ZAYIN
public class Penitence extends EgoWeapon {

	{
		image = ItemSpriteSheet.PENITENCE;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		risk = RiskLevel.ZAYIN;
		damageType = DamageType.WHITE;
		tier = 2;
	}

	private static final int HITS_PER_SHIELD = 5;
	private static final int SHIELD = 10;

	private int hits = 0;

	@Override
	public int max(int lvl) {
		return  12 +            //12 base, down from 15
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		//every 5th hit grants 10 shielding, which does not stack with itself
		if (attacker instanceof Hero && ++hits >= HITS_PER_SHIELD){
			hits = 0;
			Buff.affect(attacker, Barrier.class).setShield(SHIELD);
		}
		return damage;
	}

	@Override
	protected int abilityBoost(int lvl) {
		//same as the hand axe
		return 4 + Math.round(1.5f*lvl);
	}

	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Mace.heavyBlowAbility(hero, target, 1, dmgBoost, this);
	}

	private static final String HITS = "hits";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HITS, hits);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		hits = bundle.getInt(HITS);
	}

}
