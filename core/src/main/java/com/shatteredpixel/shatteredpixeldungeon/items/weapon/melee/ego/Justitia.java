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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

//ALEPH, every attack strikes 3 times, each with its own accuracy roll, and ignores armor
public class Justitia extends EgoWeapon {

	public static final int HITS = 3;

	{
		image = ItemSpriteSheet.JUSTITIA;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

		risk = RiskLevel.ALEPH;
		damageType = DamageType.PALE;
		tier = 5;
	}

	@Override
	public int min(int lvl) {
		return  2 +             //2 base, down from 5
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  10 +            //10 base, down from 30
				2*lvl;          //+2 per level, down from +6
	}

	@Override
	public boolean ignoresArmor() {
		return true;
	}

	//the first strike is the regular attack, the other two follow right away without taking extra time
	@Override
	public void onAttackComplete(Hero hero, Char target, int targetPos, boolean hit) {
		for (int i = 1; i < HITS; i++){
			if (target == null || !target.isAlive() || !hero.isAlive()) return;
			hero.attack(target);
		}
	}

	@Override
	protected int abilityBoost(int lvl) {
		return 2 + lvl;
	}

	//duelist ability: 3 strikes that always hit, each with bonus damage
	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		if (target == null) return;

		Char enemy = Actor.findChar(target);
		if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
			GLog.w(Messages.get(this, "ability_no_target"));
			return;
		}

		hero.belongings.abilityWeapon = this;
		if (!hero.canAttack(enemy)){
			GLog.w(Messages.get(this, "ability_target_range"));
			hero.belongings.abilityWeapon = null;
			return;
		}
		hero.belongings.abilityWeapon = null;

		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				beforeAbilityUsed(hero, enemy);
				AttackIndicator.target(enemy);
				for (int i = 0; i < HITS && enemy.isAlive(); i++){
					hero.attack(enemy, 1, dmgBoost, Char.INFINITE_ACCURACY);
				}
				if (!enemy.isAlive()){
					onAbilityKill(hero, enemy);
				}
				Invisibility.dispel();
				hero.spendAndNext(hero.attackDelay());
				afterAbilityUsed(hero);
			}
		});
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = abilityBoost(levelKnown ? buffedLvl() : 0);
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
		}
	}
}
