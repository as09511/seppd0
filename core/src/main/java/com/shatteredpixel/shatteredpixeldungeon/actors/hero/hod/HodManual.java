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


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.hod;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.levels.VaultLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

//Hod's training manual supply: +20% exp from enemies, and stacks equal to her level that power her talents
public class HodManual extends Buff {

	public static final float BASE_EXP_BONUS = 0.2f;
	public static final int VAULT_BONUS = 5;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	//leftover fractions of boosted exp and of lunch seminar exp
	private float expPartial = 0;
	private float mealPartial = 0;

	//the hero's manual, or null if the hero isn't Hod
	public static HodManual get(){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.heroClass != HeroClass.HOD) return null;
		return hero.buff(HodManual.class);
	}

	public static int stacks( Hero hero ){
		if (hero == null || hero.heroClass != HeroClass.HOD) return 0;
		int stacks = hero.lvl;
		if (hero.buff(VaultTraining.class) != null && Dungeon.level instanceof VaultLevel){
			stacks += VAULT_BONUS;
		}
		return stacks;
	}

	public static float expBonus( Hero hero ){
		//job training: +10%/+20%
		return BASE_EXP_BONUS + 0.1f*hero.pointsInTalent(Talent.JOB_TRAINING);
	}

	//exp from enemies, boosted for Hod with fractions carried over to the next gain
	public static int boostExp( int exp ){
		HodManual m = get();
		if (m == null || exp <= 0) return exp;
		float total = exp * (1f + expBonus(Dungeon.hero)) + m.expPartial;
		int whole = (int)total;
		m.expPartial = total - whole;
		return whole;
	}

	//rest time: heals 1x/2x stacks on each level up
	public static void onLevelUp( Hero hero ){
		if (hero.heroClass != HeroClass.HOD || !hero.hasTalent(Talent.REST_TIME)) return;
		int heal = Math.min(stacks(hero) * hero.pointsInTalent(Talent.REST_TIME), hero.HT - hero.HP);
		if (heal > 0){
			hero.HP += heal;
			if (hero.sprite != null){
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
	}

	//practical evaluation: the first hit on each enemy deals stacks/3 / stacks*2/3 bonus damage
	public static int onAttackProc( Hero hero, Char enemy, int dmg ){
		if (hero.hasTalent(Talent.PRACTICAL_EVAL) && enemy.buff(PracticeTracker.class) == null){
			dmg += Math.round(stacks(hero) * hero.pointsInTalent(Talent.PRACTICAL_EVAL) / 3f);
			Buff.affect(enemy, PracticeTracker.class);
		}
		return dmg;
	}

	//safety training: the first time each floor health drops below 50%, gain 1x/2x stacks as shielding
	public static void onDamaged( Hero hero ){
		if (hero.isAlive() && hero.HP < hero.HT/2f && hero.hasTalent(Talent.SAFETY_TRAINING)
				&& !SafetyTrainingUsed.usedHere()){
			int shield = stacks(hero) * hero.pointsInTalent(Talent.SAFETY_TRAINING);
			if (shield > 0){
				Buff.affect(hero, Barrier.class).incShield(shield);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			}
			SafetyTrainingUsed.markUsed();
		}
	}

	//basic fitness: +0.5x/1x stacks max HP
	public static int bonusHT( Hero hero ){
		if (hero.heroClass != HeroClass.HOD) return 0;
		return Math.round(stacks(hero) * 0.5f * hero.pointsInTalent(Talent.BASIC_FITNESS));
	}

	//demonstration training: +1%/1.5% accuracy per stack
	public static float accuracyMultiplier( Hero hero ){
		if (hero.heroClass != HeroClass.HOD || !hero.hasTalent(Talent.DEMO_TRAINING)) return 1f;
		return 1f + stacks(hero) * 0.005f * (1 + hero.pointsInTalent(Talent.DEMO_TRAINING));
	}

	//field adaptation: +0.5%/1% evasion per stack
	public static float evasionMultiplier( Hero hero ){
		if (hero.heroClass != HeroClass.HOD) return 1f;
		return 1f + stacks(hero) * 0.005f * hero.pointsInTalent(Talent.FIELD_ADAPTATION);
	}

	//lunch seminar: exp equal to 5%/7.5% of the hunger actually restored, not boosted by the manual
	public static void onFoodEaten( Hero hero, float hungerRestored ){
		if (!hero.hasTalent(Talent.LUNCH_SEMINAR) || hungerRestored <= 0) return;
		HodManual m = hero.buff(HodManual.class);
		float total = hungerRestored * (0.025f + 0.025f*hero.pointsInTalent(Talent.LUNCH_SEMINAR));
		if (m != null){
			total += m.mealPartial;
			int whole = (int)total;
			m.mealPartial = total - whole;
			if (whole > 0) giveExp(hero, whole);
		} else if ((int)total > 0){
			giveExp(hero, (int)total);
		}
	}

	private static void giveExp( Hero hero, int exp ){
		if (hero.sprite != null){
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(exp), FloatingText.EXPERIENCE);
		}
		hero.earnExp(exp, HodManual.class);
	}

	@Override
	public int icon() {
		return BuffIndicator.MANUAL;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks((Hero)target));
	}

	@Override
	public String desc() {
		Hero hero = (Hero)target;
		String desc = Messages.get(this, "desc", stacks(hero), Math.round(100*expBonus(hero)));
		if (hero.buff(VaultTraining.class) != null && Dungeon.level instanceof VaultLevel){
			desc += "\n\n" + Messages.get(this, "vault", VAULT_BONUS);
		}
		return desc;
	}

	private static final String EXP_PARTIAL = "exp_partial";
	private static final String MEAL_PARTIAL = "meal_partial";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(EXP_PARTIAL, expPartial);
		bundle.put(MEAL_PARTIAL, mealPartial);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		expPartial = bundle.getFloat(EXP_PARTIAL);
		mealPartial = bundle.getFloat(MEAL_PARTIAL);
	}

	//marks enemies that already took Hod's practical evaluation hit
	public static class PracticeTracker extends Buff {}

	public static class SafetyTrainingUsed extends Buff {
		{
			revivePersists = true;
		}

		public int depth = -1;

		public static boolean usedHere(){
			SafetyTrainingUsed b = Dungeon.hero.buff(SafetyTrainingUsed.class);
			return b != null && b.depth == Dungeon.depth;
		}

		public static void markUsed(){
			Buff.affect(Dungeon.hero, SafetyTrainingUsed.class).depth = Dungeon.depth;
		}

		private static final String DEPTH = "depth";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DEPTH, depth);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			depth = bundle.getInt(DEPTH);
		}
	}

	//the vault mirror's training: +5 stacks while inside the vault, lost when leaving it
	public static class VaultTraining extends Buff {

		@Override
		public boolean attachTo(Char target) {
			if (super.attachTo(target)){
				if (target instanceof Hero) ((Hero) target).updateHT(false);
				BuffIndicator.refreshHero();
				return true;
			}
			return false;
		}

		@Override
		public void detach() {
			super.detach();
			if (target instanceof Hero) ((Hero) target).updateHT(false);
			BuffIndicator.refreshHero();
		}
	}
}
