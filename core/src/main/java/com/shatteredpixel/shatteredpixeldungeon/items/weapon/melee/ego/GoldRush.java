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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//ALEPH, same as the gauntlets, every hit adds a stack of the Golden Road:
//more melee damage but less armor, see Hero.damageRoll and Hero.drRoll
public class GoldRush extends EgoWeapon {

	{
		image = ItemSpriteSheet.GOLD_RUSH;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.2f;

		risk = RiskLevel.ALEPH;
		damageType = DamageType.RED;
		tier = 5;
		DLY = 0.5f; //2x speed
	}

	@Override
	public int max(int lvl) {
		return  Math.round(2.5f*(tier+1)) +     //15 base, down from 30
				lvl*Math.round(0.5f*(tier+1));  //+3 per level, down from +6
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		Buff.affect(attacker, GoldenRoad.class).hit();
		return damage;
	}

	@Override
	protected int abilityBoost(int lvl) {
		return 5 + lvl;
	}

	//same as the gauntlets
	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		Sai.comboStrikeAbility(hero, target, 0, dmgBoost, this);
	}

	@Override
	public String abilityInfo() {
		int dmgBoost = abilityBoost(levelKnown ? buffedLvl() : 0);
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", augment.damageFactor(dmgBoost));
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return "+" + augment.damageFactor(abilityBoost(level));
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", GoldenRoad.DURATION, GoldenRoad.MAX_STACKS, GoldenRoad.DAMAGE, GoldenRoad.ARMOR);
	}

	public static class GoldenRoad extends Buff {

		public static final int DURATION = 2;
		public static final int MAX_STACKS = 5;
		public static final int DAMAGE = 2;
		public static final int ARMOR = 1;

		private int stacks = 0;
		private int left = 0;

		{
			type = buffType.POSITIVE;
		}

		public void hit(){
			stacks = Math.min(MAX_STACKS, stacks + 1);
			left = DURATION;
			BuffIndicator.refreshHero();
		}

		public int damageBonus(){
			return DAMAGE*stacks;
		}

		public int armorPenalty(){
			return ARMOR*stacks;
		}

		@Override
		public boolean act() {
			left--;
			if (left <= 0){
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.8f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - left) / (float)DURATION);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(stacks);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", stacks, damageBonus(), armorPenalty(), left);
		}

		private static final String STACKS = "stacks";
		private static final String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(STACKS, stacks);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt(STACKS);
			left = bundle.getInt(LEFT);
		}
	}
}
