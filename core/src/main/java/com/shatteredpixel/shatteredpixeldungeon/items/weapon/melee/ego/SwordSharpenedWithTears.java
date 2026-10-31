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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//WAW, same stats as the katana.
//Every 4th regular attack also strikes every other enemy in the 3x3 area in front of the wielder.
public class SwordSharpenedWithTears extends EgoWeapon {

	private static final int MAX_CHARGE = 3;

	private int charge = 0;

	{
		image = ItemSpriteSheet.TEARS_SWORD;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

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
	public int defenseFactor( Char owner ) {
		return 3;	//3 extra defence, same as the katana
	}

	@Override
	public void onAttackComplete(Hero hero, Char target, int targetPos, boolean hit) {
		if (charge < MAX_CHARGE){
			charge++;
		} else {
			charge = 0;
			sweep(hero, target, targetPos);
		}
		updateQuickslot();
	}

	//the 3x3 square right in front of the hero, in the direction of the target
	private void sweep(Hero hero, Char target, int targetPos){
		int width = Dungeon.level.width();
		int hx = hero.pos % width, hy = hero.pos / width;
		int dx = (int)Math.signum(targetPos % width - hx);
		int dy = (int)Math.signum(targetPos / width - hy);
		int cx = hx + 2*dx, cy = hy + 2*dy;

		ArrayList<Char> targets = new ArrayList<>();
		for (int y = cy-1; y <= cy+1; y++){
			for (int x = cx-1; x <= cx+1; x++){
				if (x < 0 || y < 0 || x >= width || y >= Dungeon.level.height()) continue;
				int cell = x + y*width;
				if (cell == hero.pos || Dungeon.level.solid[cell]) continue;
				//walls block the sweep
				if (new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID).collisionPos != cell) continue;

				WhiteSlash slash = (WhiteSlash) hero.sprite.parent.recycle(WhiteSlash.class);
				hero.sprite.parent.bringToFront(slash);
				slash.reset(cell);
				slash.angle = (float)Math.toDegrees(Math.atan2(dy, dx));

				Char ch = Actor.findChar(cell);
				if (ch != null && ch != target && ch.alignment == Char.Alignment.ENEMY){
					targets.add(ch);
				}
			}
		}

		for (Char ch : targets){
			if (ch.isAlive()) hero.attack(ch);
		}
	}

	@Override
	public String status() {
		return Integer.toString(charge);
	}

	@Override
	protected int abilityBoost(int lvl) {
		//same as the katana
		return 8 + Math.round(2f*lvl);
	}

	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Rapier.lungeAbility(hero, target, 1, dmgBoost, this);
	}

	private static final String CHARGE = "charge";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
	}

	public static class WhiteSlash extends Wound {
		public WhiteSlash(){
			super();
			hardlight(1f, 1f, 1f);
		}
	}

}
