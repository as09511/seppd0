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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.gebura;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RabbitSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//A Rabbit Team member called in by Gebura. Each deals one damage type (RED, WHITE, BLACK or PALE),
// slashes enemies next to it and shoots the ones further away, and acts on its own, apart from Gebura,
// even with no enemy around. Withdraws when its operation time runs out.
//Health 10 + 3 per level, armor 0~level/3, accuracy and evasion of a hero of that level,
// slashes for level/2 ~ level+4, shots deal 70% of that. Elite squad: +15% health and damage per point.
public class RabbitMember extends Mob {

	public static final float SHOT_FACTOR = 0.7f;

	{
		spriteClass = RabbitSprite.class;

		alignment = Alignment.ALLY;
		state = WANDERING;
		WANDERING = new Roaming();

		HP = HT = 10;

		immunities.add(AllyBuff.class);
	}

	private int type = 0;
	private int heroLevel = 1;
	private float mult = 1f;
	private int left = 20;
	private boolean shooting = false;

	public RabbitMember(){
		super();
	}

	public RabbitMember( Hero hero, int type, int duration ){
		super();
		this.type = type;
		heroLevel = hero.lvl;
		mult = 1f + 0.15f*hero.pointsInTalent(Talent.ELITE_SQUAD);
		left = duration;
		HP = HT = Math.max(1, Math.round((10 + 3*heroLevel) * mult));
		defenseSkill = heroLevel + 4;
	}

	public DamageType damageType(){
		return DamageType.values()[type];
	}

	public int type(){
		return type;
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + damageType().name().toLowerCase());
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", left);
	}

	@Override
	protected boolean act() {
		left--;
		if (left <= 0){
			withdraw();
			//the last one out says the team is pulling back
			boolean anyLeft = false;
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof RabbitMember && m != this && m.isAlive()) anyLeft = true;
			}
			if (!anyLeft) GLog.i(Messages.get(RabbitMember.class, "withdrawn"));
			return true;
		}
		return super.act();
	}

	//pulls out of the operation
	public void withdraw(){
		if (sprite != null && sprite.visible){
			CellEmitter.get(pos).burst(Speck.factory(Speck.LIGHT), 6);
		}
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	@Override
	public int attackSkill( Char target ) {
		return heroLevel + 9;
	}

	@Override
	public int damageRoll() {
		int dmg = Random.NormalIntRange(Math.round(heroLevel/2f * mult), Math.round((heroLevel + 4) * mult));
		if (shooting) dmg = Math.round(dmg * SHOT_FACTOR);
		return dmg;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, heroLevel/3);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = DamageType.apply(enemy, damageType(), damage);
		return super.attackProc(enemy, damage);
	}

	//slashes what is next to it, shoots what it can see in a straight line
	@Override
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent(pos, enemy.pos)) return true;
		return new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent(pos, enemy.pos)){
			return super.doAttack(enemy);
		}
		spend(attackDelay());
		boolean visible = sprite != null && (Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos]);
		if (visible){
			sprite.turnTo(pos, enemy.pos);
			//the same shot as the firearm E.G.O
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 0.8f, 0.8f);
			MagicMissile.boltFromChar(sprite.parent, boltType(), sprite, enemy.pos, new Callback() {
				@Override
				public void call() {
					shoot(enemy);
					next();
				}
			});
			return false;
		} else {
			shoot(enemy);
			return true;
		}
	}

	private void shoot( Char enemy ){
		shooting = true;
		try {
			attack(enemy);
		} finally {
			shooting = false;
		}
	}

	private int boltType(){
		switch (damageType()){
			case RED: default:  return MagicMissile.SHAMAN_RED;
			case WHITE:         return MagicMissile.LIGHT_MISSILE;
			case BLACK:         return MagicMissile.SHADOW;
			case PALE:          return MagicMissile.SHAMAN_BLUE;
		}
	}

	//keeps going on its own: it notices enemies right away, and roams the floor when there are none
	private class Roaming extends Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV){
				return noticeEnemy();
			}
			return continueWandering();
		}
	}

	private static final String TYPE = "type";
	private static final String HERO_LEVEL = "hero_level";
	private static final String MULT = "mult";
	private static final String LEFT = "left";
	private static final String DEF_SKILL = "def_skill";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(TYPE, type);
		bundle.put(HERO_LEVEL, heroLevel);
		bundle.put(MULT, mult);
		bundle.put(LEFT, left);
		bundle.put(DEF_SKILL, defenseSkill);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		type = bundle.getInt(TYPE);
		heroLevel = bundle.getInt(HERO_LEVEL);
		mult = bundle.getFloat(MULT);
		left = bundle.getInt(LEFT);
		defenseSkill = bundle.getInt(DEF_SKILL);
	}
}
