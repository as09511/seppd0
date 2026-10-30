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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//TETH, a small pistol whose bullets stop at the first character they hit
public class Beak extends EgoGun {

	private static final int MAX_AMMO = 4;
	private static final float RELOAD_TIME = 1f;

	{
		image = ItemSpriteSheet.BEAK;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.2f;

		risk = RiskLevel.TETH;
		damageType = DamageType.RED;
		tier = 2;
	}

	@Override
	public int min(int lvl) {
		return  2 +             //2 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  9 +             //9 base, down from 15
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
		return  3 +             //3 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  7 +             //7 base
				lvl*(tier+1);   //+3 per level, same as other tier 2 weapons
	}

	@Override
	protected void fireShot(Hero hero, int target) {
		//shooting is affected by attack speed, just like melee attacks
		float delay = hero.attackDelay();
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.PROJECTILE);
		int cell = path.collisionPos;

		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1f, 1.6f);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SHAMAN_RED, hero.sprite, cell, new Callback() {
			@Override
			public void call() {
				Char ch = Actor.findChar(cell);
				if (ch != null && ch != hero){
					shoot(hero, ch);
				}
				hero.spendAndNext(delay);
			}
		});
	}

}
