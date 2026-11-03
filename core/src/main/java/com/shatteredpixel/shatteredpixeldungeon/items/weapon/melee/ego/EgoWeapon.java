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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.EgoText;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

//E.G.O weapons, based on the equipment extracted from abnormalities in Lobotomy Corporation
//Each one deals a damage type, which is scaled by the target's multiplier for that type
public abstract class EgoWeapon extends MeleeWeapon {

	protected RiskLevel risk = RiskLevel.ZAYIN;
	protected DamageType damageType = DamageType.RED;

	public RiskLevel risk(){
		return risk;
	}

	//the risk level Angela charges for when refining this E.G.O
	public RiskLevel costRisk(){
		return risk;
	}

	public DamageType damageType(){
		return damageType;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = DamageType.apply(defender, damageType, damage);
		damage = egoProc(attacker, defender, damage);
		return super.proc(attacker, defender, damage);
	}

	//weapon-specific on-hit effects
	protected int egoProc(Char attacker, Char defender, int damage){
		return damage;
	}

	//called when the hero kills a mob while this weapon is equipped
	public void onKill(Hero hero, Char killed){
	}

	//called after each regular melee attack by the hero, whether it hit or not
	public void onAttackComplete(Hero hero, Char target, int targetPos, boolean hit){
	}

	//weapons that ignore the target's armor entirely, see Char.attack
	public boolean ignoresArmor(){
		return false;
	}

	//the damage type as shown in the description
	protected String damageTypeName(){
		return damageType.coloredName();
	}

	protected static void healAttacker(Char attacker, int amount){
		amount = Math.min(amount, attacker.HT - attacker.HP);
		if (amount > 0 && attacker.isAlive()){
			attacker.HP += amount;
			attacker.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
		}
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	//flat damage bonus of this weapon's duelist ability at the given level
	protected int abilityBoost(int lvl){
		return 0;
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		egoAbility(hero, target, augment.damageFactor(abilityBoost(buffedLvl())));
	}

	protected abstract void egoAbility(Hero hero, Integer target, int dmgBoost);

	@Override
	public String abilityInfo() {
		int dmgBoost = abilityBoost(levelKnown ? buffedLvl() : 0);
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
		} else {
			return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		int dmgBoost = abilityBoost(level);
		return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
	}

	//risk level and damage type replace the tier in the stats line, tier is kept for balancing
	@Override
	protected String statsLine(boolean known) {
		//uses the game's own translated sentence, with the tier swapped for the risk level and the damage type added
		String template = EgoText.withDamageType(EgoText.riskTemplate(
				Messages.get(MeleeWeapon.class, known ? "stats_known" : "stats_unknown")));
		if (known){
			return Messages.format(template, risk.coloredTitle(), augment.damageFactor(min()), augment.damageFactor(max()), STRReq(), damageTypeName());
		} else {
			return Messages.format(template, risk.coloredTitle(), min(0), max(0), STRReq(0), damageTypeName());
		}
	}
}
