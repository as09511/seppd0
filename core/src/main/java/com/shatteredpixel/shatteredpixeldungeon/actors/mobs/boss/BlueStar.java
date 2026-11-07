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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.boss;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.OrdealMob;
import com.shatteredpixel.shatteredpixeldungeon.levels.LCorpHQLevel;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlueStarSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the Blue Star (ALEPH), the first of the boss fights Angela offers on floor 26. It waits in the middle of the
//Central Command lobby at L Corp headquarters, never moves, and from the moment the fight starts it sends a
//blue shockwave over the whole floor every turn, which strikes everyone but itself and can't be dodged.
//The cell it stands on is its bottom left, three Body parts take up the rest and pass on what they are dealt.
public class BlueStar extends Mob {

	{
		spriteClass = BlueStarSprite.class;

		HP = HT = 2200;
		defenseSkill = 0;
		EXP = 0;
		maxLvl = -2;

		damageMultipliers = new float[]{0.4f, 0.2f, 0.8f, 1.2f};

		properties.add(Property.BOSS);
		properties.add(Property.IMMOVABLE);

		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
		immunities.add(Dread.class);
		immunities.add(Vertigo.class);
		immunities.add(Amok.class);
		immunities.add(Paralysis.class);
		immunities.add(Charm.class);
		immunities.add(Frost.class);
		//its other cells can't change sides with it
		immunities.add(Corruption.class);

		state = HUNTING;
	}

	public static final RiskLevel RISK = RiskLevel.ALEPH;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(45, 55);
	}

	//its shockwave can't be dodged
	@Override
	public int attackSkill( Char target ) {
		return INFINITE_ACCURACY;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = DamageType.apply(enemy, DamageType.WHITE, damage);
		return super.attackProc(enemy, damage);
	}

	@Override
	public String info() {
		return Messages.get(OrdealMob.class, "risk", RISK.coloredTitle()) + "\n\n" + super.info();
	}

	// ---------- the body ----------

	private static int[] bodyCells( int anchor ){
		int w = Dungeon.level.width();
		return new int[]{anchor, anchor + 1, anchor - w, anchor - w + 1};
	}

	private Body[] parts(){
		Body[] parts = new Body[3];
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Body && ((Body) m).owner == id() && ((Body) m).index >= 0 && ((Body) m).index < 3){
				parts[((Body) m).index] = (Body) m;
			}
		}
		return parts;
	}

	//the parts are made again where they belong if any went missing
	private void ensureParts(){
		Body[] parts = parts();
		int[] cells = bodyCells(pos);
		for (int i = 0; i < 3; i++){
			if (parts[i] == null && Actor.findChar(cells[i+1]) == null){
				Body b = new Body();
				b.owner = id();
				b.index = i;
				b.HT = HT;
				b.HP = HP;
				b.pos = cells[i+1];
				GameScene.add(b);
			}
		}
	}

	//puts the Blue Star and its parts in the middle of the Central Command lobby of a level being arrived at
	public static void spawn( LCorpHQLevel level ){
		for (Mob m : level.mobs){
			if (m instanceof BlueStar) return;
		}
		BlueStar star = new BlueStar();
		star.pos = level.lobbyCentre();
		level.mobs.add(star);
		int[] cells = bodyCells(star.pos);
		for (int i = 0; i < 3; i++){
			Body b = new Body();
			b.owner = star.id();
			b.index = i;
			b.pos = cells[i+1];
			level.mobs.add(b);
		}
		level.locked = true;
	}

	// ---------- what it does ----------

	private boolean announced = false;

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		ensureParts();

		if (!announced){
			announced = true;
			BossHealthBar.assignBoss(this);
			Sample.INSTANCE.play(Assets.Sounds.BOSS);
		} else if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss(this);
		}

		shockwave();
		spend(attackDelay());
		return true;
	}

	//a blue shockwave over the whole floor, striking everyone else on it
	private void shockwave(){
		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch != this && !(ch instanceof Body) && ch.isAlive()) targets.add(ch);
		}
		if (sprite instanceof BlueStarSprite){
			((BlueStarSprite) sprite).shockwave();
		}
		Sample.INSTANCE.play(Assets.Sounds.RAY, 1f, 0.6f);
		for (Char t : targets){
			if (!t.isAlive() || !isAlive()) continue;
			attack(t);
		}
	}

	//a blast or a cloud that covers several of its cells at once only harms it once
	private Object lastSource = null;
	private float lastSourceTime = -1;

	@Override
	public void damage( int dmg, Object src ) {
		if (src != null && src == lastSource && Actor.now() == lastSourceTime) return;
		lastSource = src;
		lastSourceTime = Actor.now();
		super.damage(dmg, src);
		for (Body b : parts()){
			if (b != null){
				b.HT = HT;
				b.HP = Math.max(1, HP);
			}
		}
	}

	@Override
	public void die( Object cause ) {
		for (Body b : parts()){
			if (b != null){
				b.destroy();
				if (b.sprite != null) b.sprite.killAndErase();
			}
		}
		super.die(cause);
		//L Corp headquarters opens again, the stairs in Malkuth's room lead back up
		Dungeon.level.locked = false;
		GLog.p(Messages.get(this, "suppressed"));
	}

	@Override
	protected boolean getCloser( int target ) {
		return false;
	}

	@Override
	protected boolean getFurther( int target ) {
		return false;
	}

	@Override
	public void beckon( int cell ) {
	}

	private static final String ANNOUNCED = "announced";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ANNOUNCED, announced);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		announced = bundle.getBoolean(ANNOUNCED);
		BossHealthBar.assignBoss(this);
	}

	//the other three cells of the Blue Star: it can't be told apart from the Blue Star,
	//everything done to it is done to the Blue Star instead, and it goes when the Blue Star does
	public static class Body extends Mob {

		{
			spriteClass = BlueStarSprite.Body.class;

			HP = HT = 2200;
			defenseSkill = 0;
			EXP = 0;
			maxLvl = -2;

			properties.add(Property.BOSS);
			properties.add(Property.IMMOVABLE);

			state = PASSIVE;
			alignment = Alignment.ENEMY;
		}

		private int owner = -1;
		private int index = -1;

		public BlueStar owner(){
			Actor a = Actor.findById(owner);
			return a instanceof BlueStar && ((BlueStar) a).isAlive() ? (BlueStar) a : null;
		}

		@Override
		protected boolean act() {
			if (owner() == null){
				destroy();
				if (sprite != null) sprite.killAndErase();
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public void damage( int dmg, Object src ) {
			BlueStar o = owner();
			if (o != null) o.damage(dmg, src);
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public int defenseSkill( Char enemy ) {
			BlueStar o = owner();
			return o != null ? o.defenseSkill(enemy) : defenseSkill;
		}

		@Override
		public int drRoll() {
			BlueStar o = owner();
			return o != null ? o.drRoll() : 0;
		}

		@Override
		public float damageMultiplier( DamageType type ) {
			BlueStar o = owner();
			return o != null ? o.damageMultiplier(type) : super.damageMultiplier(type);
		}

		@Override
		public String name() {
			return Messages.get(BlueStar.class, "name");
		}

		@Override
		public String info() {
			BlueStar o = owner();
			return o != null ? o.info() : Messages.get(BlueStar.class, "desc");
		}

		@Override
		protected boolean getCloser( int target ) {
			return false;
		}

		@Override
		protected boolean getFurther( int target ) {
			return false;
		}

		@Override
		public void beckon( int cell ) {
		}

		@Override
		protected boolean canAttack( Char enemy ) {
			return false;
		}

		private static final String OWNER = "owner";
		private static final String INDEX = "index";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle(bundle);
			bundle.put(OWNER, owner);
			bundle.put(INDEX, index);
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle(bundle);
			owner = bundle.getInt(OWNER);
			index = bundle.getInt(INDEX);
		}
	}
}
