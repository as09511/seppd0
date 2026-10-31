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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//WAW, a gun whose bullets build up from bleeding, to burning, to extra damage
public class Hornet extends EgoGun {

	private static final int MAX_AMMO = 5;
	private static final float RELOAD_TIME = 2f;

	{
		image = ItemSpriteSheet.HORNET;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.WAW;
		damageType = DamageType.RED;
		tier = 4;
	}

	@Override
	public int max(int lvl) {
		return  15 +            //15 base, down from 25
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int maxAmmo() {
		return MAX_AMMO;
	}

	@Override
	public float reloadTime() {
		return RELOAD_TIME;
	}

	@Override
	public int shotMin(int lvl) {
		return  6 +             //6 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  15 +            //15 base
				lvl*(tier+1);   //+5 per level
	}

	//every effect the target qualifies for applies, based on its state before the hit
	@Override
	protected int shotProc(Char attacker, Char defender, int damage) {
		boolean wasBleeding = defender.buff(Bleeding.class) != null;
		boolean wasBurning = defender.buff(Burning.class) != null;

		if (wasBurning){
			damage = Math.round(damage * 1.5f);
		}
		if (wasBleeding && !defender.isImmune(Burning.class)){
			Buff.affect(defender, Burning.class).reignite(defender, 3f);
		}
		if (damage > 0){
			Buff.affect(defender, Bleeding.class).set((float)Math.ceil(damage * 0.33f));
		}
		return damage;
	}

}
