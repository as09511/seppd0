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
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//HE, twin pistols: each bullet deals WHITE and BLACK damage, calculated separately.
//Kills made while it is equipped load a bullet back in.
public class SolemnLament extends EgoGun {

	private static final int MAX_AMMO = 7;
	private static final float RELOAD_TIME = 2f;

	{
		image = ItemSpriteSheet.SOLEMN_LAMENT;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.2f;

		risk = RiskLevel.HE;
		damageType = DamageType.WHITE;
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
		return  1 +             //1 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  7 +             //7 base
				2*lvl;          //+2 per level
	}

	@Override
	protected int boltType() {
		return MagicMissile.SHAMAN_PURPLE;
	}

	//the bullet's normal hit is WHITE, then a separate BLACK hit is rolled with its own armor and multiplier
	@Override
	protected void onShotHit(Hero hero, Char ch, boolean hit) {
		if (hit && ch.isAlive()){
			int dmg = Math.max(0, new Shot(this).damageRoll(hero) - ch.drRoll());
			dmg = DamageType.apply(ch, DamageType.BLACK, dmg);
			ch.damage(dmg, hero);
		}
	}

	@Override
	public String statsInfo() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return Messages.get(this, "stats_desc", augment.damageFactor(shotMin(lvl)), augment.damageFactor(shotMax(lvl)),
				DamageType.WHITE.coloredName(), DamageType.BLACK.coloredName(), MAX_AMMO);
	}

	@Override
	public void onKill(Hero hero, Char killed) {
		if (ammo < MAX_AMMO){
			ammo++;
			updateQuickslot();
		}
	}

}
