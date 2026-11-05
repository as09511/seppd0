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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.hod.HodTrackers;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Mace;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//TETH
public class Regret extends EgoWeapon {

	{
		image = ItemSpriteSheet.REGRET;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;

		risk = RiskLevel.TETH;
		damageType = DamageType.RED;
		tier = 2;
		DLY = 1.5f; //0.67x speed
	}

	@Override
	public int max(int lvl) {
		return  20 +            //20 base, up from 15
				4*lvl;          //+4 per level, up from +3
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		//enemies in the 4 cells around the target take half of the damage this weapon would deal to them,
		//calculated separately for each of them (own damage roll, armor and damage multiplier)
		if (attacker == Dungeon.hero){
			ArrayList<Char> targets = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS4){
				Char ch = Actor.findChar(defender.pos + n);
				if (ch != null && ch != attacker && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
					targets.add(ch);
				}
			}
			for (Char ch : targets){
				int splash = Math.max(0, HodTrackers.boostDirectDamage(attacker, ch, attacker.damageRoll()) - ch.drRoll());
				splash = DamageType.apply(ch, damageType, splash) / 2;
				if (splash > 0) {
					ch.damage(splash, attacker);
				}
			}
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

}
