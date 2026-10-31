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

//HE, a heavy old rifle with strong bullets and a slow reload
public class Laetitia extends EgoGun {

	private static final int MAX_AMMO = 5;
	private static final float RELOAD_TIME = 2f;

	{
		image = ItemSpriteSheet.LAETITIA;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		risk = RiskLevel.HE;
		damageType = DamageType.RED;
		tier = 3;
	}

	@Override
	public int max(int lvl) {
		return  12 +            //12 base, down from 20
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
		return  8 +             //8 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  12 +            //12 base
				lvl*(tier+1);   //+4 per level, same as other tier 3 weapons
	}

}
