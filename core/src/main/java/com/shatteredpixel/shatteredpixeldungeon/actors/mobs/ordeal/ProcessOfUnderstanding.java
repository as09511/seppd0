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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ProcessOfUnderstandingSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//Noon of Green: a box-headed machine with a chainsaw arm and a gun arm. It saws what is next to it,
//shoots what is further away, and after 5 turns of fighting it stands still for 2 turns, throwing sparks.
public class ProcessOfUnderstanding extends OrdealMob {

	public static final int WORK_TURNS  = 5;
	public static final int SPARK_TURNS = 2;

	{
		spriteClass = ProcessOfUnderstandingSprite.class;

		HP = HT = 300;
		defenseSkill = 8;
		EXP = 20;

		tier = Angela.NOON;
		maxLvl = NOON_MAX_LVL;
		risk = RiskLevel.HE;

		attackType = DamageType.RED;
		damageMultipliers = new float[]{0.8f, 1.3f, 2.0f, 1.0f};

		properties.add(Property.INORGANIC);
	}

	//turns spent fighting since it last overheated
	private int worked = 0;
	//turns left standing still among the sparks
	private int sparking = 0;
	//whether the attack being made is a gunshot
	private boolean shooting = false;

	@Override
	public int damageRoll() {
		return shooting ? Random.NormalIntRange(5, 15) : Random.NormalIntRange(15, 25);
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	public boolean sparking(){
		return sparking > 0;
	}

	@Override
	protected boolean act() {
		if (sparking > 0 && paralysed <= 0){
			sparking--;
			if (sprite != null){
				sprite.centerEmitter().burst(SparkParticle.FACTORY, 8);
				if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.5f, 1.4f);
			}
			if (sparking == 0){
				worked = 0;
				if (sprite != null) ((ProcessOfUnderstandingSprite) sprite).spark(false);
			}
			spend(TICK);
			return true;
		}

		boolean fighting = state == HUNTING && paralysed <= 0;
		boolean result = super.act();
		if (fighting && ++worked >= WORK_TURNS){
			sparking = SPARK_TURNS;
			if (sprite != null){
				((ProcessOfUnderstandingSprite) sprite).spark(true);
				if (Dungeon.level.heroFOV[pos]){
					sprite.showStatus(CharSprite.WARNING, Messages.get(this, "overheat"));
				}
			}
		}
		return result;
	}

	private boolean canShoot( Char enemy ){
		return fieldOfView != null && enemy.pos < fieldOfView.length && fieldOfView[enemy.pos]
				&& new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack(enemy) || canShoot(enemy);
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent(pos, enemy.pos) || !canShoot(enemy)){
			shooting = false;
			return super.doAttack(enemy);
		}
		if (sprite != null && (sprite.visible || enemy.sprite.visible)){
			sprite.zap(enemy.pos);
			return false;
		}
		shoot();
		return true;
	}

	public void onZapComplete(){
		shoot();
		next();
	}

	private void shoot(){
		shooting = true;
		attack(enemy);
		shooting = false;
		Invisibility.dispel(this);
		spend(attackDelay());
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		if (sparking > 0) ((ProcessOfUnderstandingSprite) sprite).spark(true);
	}

	private static final String WORKED   = "worked";
	private static final String SPARKING = "sparking";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(WORKED, worked);
		bundle.put(SPARKING, sparking);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		worked = bundle.getInt(WORKED);
		sparking = bundle.getInt(SPARKING);
	}
}
