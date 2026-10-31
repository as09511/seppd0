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
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//WAW, a crossbow with a single heavy arrow, uses the bow sounds
public class Echo extends EgoGun {

	private static final int MAX_AMMO = 1;
	private static final float RELOAD_TIME = 1f;

	{
		image = ItemSpriteSheet.ECHO;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.WAW;
		damageType = DamageType.WHITE;
		tier = 4;
	}

	@Override
	public int max(int lvl) {
		return  4*(tier+1) +    //20 base, down from 25
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	protected boolean isBow() {
		return true;
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
		return  12 +            //12 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  30 +            //30 base
				6*lvl;          //+6 per level
	}

}
