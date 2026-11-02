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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//TETH, a revolver: bullets deal WHITE damage and have a 1 in 6 chance to weaken the target for 3 turns
public class Loneliness extends EgoGun {

	private static final int MAX_AMMO = 6;
	private static final float RELOAD_TIME = 2f;

	public static final float WEAKNESS_DURATION = 3f;

	{
		image = ItemSpriteSheet.LONELINESS;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.TETH;
		damageType = DamageType.WHITE;
		tier = 2;
	}

	@Override
	public int min(int lvl) {
		return  2 +             //2 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  9 +             //9 base
				lvl*(tier+1);   //+3 per level
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
		return  3 +             //3 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  7 +             //7 base
				lvl*(tier+1);   //+3 per level
	}

	@Override
	protected int shotProc(Char attacker, Char defender, int damage) {
		if (Random.Int(6) == 0 && defender.isAlive()){
			Buff.prolong(defender, Weakness.class, WEAKNESS_DURATION);
		}
		return damage;
	}

}
