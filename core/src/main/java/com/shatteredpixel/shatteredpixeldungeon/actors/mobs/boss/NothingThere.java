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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.OrdealMob;
import com.shatteredpixel.shatteredpixeldungeon.levels.LCorpHQLevel;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.NothingThereSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

//Nothing There (ALEPH), the second boss fight Angela offers on floor 26. It appears in the middle of the Central
//Command lobby at L Corp headquarters and comes for the hero. Besides its claws it has two patterns:
// "Hello?"  - every 5 turns, a spike down a straight line 8 cells long, through everyone on it
// "Good Bye" - every 8 turns, the 1x3 cells in front of it are marked for a turn, then struck by its club
//Good Bye goes first when both are ready. Hello? can be dodged like its claws, Good Bye always hits, armor counts.
//Neither starts counting down until it has first seen an enemy. Hello? goes at the enemy in sight it is most set on.
public class NothingThere extends Mob {

	public static final int HELLO_TURNS    = 5;
	public static final int HELLO_RANGE    = 8;
	public static final int GOODBYE_TURNS  = 8;
	public static final int GOODBYE_DAMAGE = 300;

	{
		spriteClass = NothingThereSprite.class;

		HP = HT = 3000;
		defenseSkill = 20;
		EXP = 0;
		maxLvl = -2;

		damageMultipliers = new float[]{0.1f, 0.4f, 0.4f, 0.8f};

		properties.add(Property.BOSS);

		immunities.add(Paralysis.class);
		immunities.add(Sleep.class);
		immunities.add(MagicalSleep.class);
		immunities.add(Terror.class);
		immunities.add(Dread.class);

		state = HUNTING;
	}

	public static final RiskLevel RISK = RiskLevel.ALEPH;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(50, 65);
	}

	//its patterns can't be dodged
	private boolean sureHit = false;

	@Override
	public int attackSkill( Char target ) {
		return sureHit ? INFINITE_ACCURACY : 40;
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = DamageType.apply(enemy, DamageType.RED, damage);
		return super.attackProc(enemy, damage);
	}

	@Override
	public String info() {
		return Messages.get(OrdealMob.class, "risk", RISK.coloredTitle()) + "\n\n" + super.info();
	}

	//puts Nothing There in the middle of the Central Command lobby of a level being arrived at
	public static void spawn( LCorpHQLevel level ){
		for (Mob m : level.mobs){
			if (m instanceof NothingThere) return;
		}
		NothingThere boss = new NothingThere();
		boss.pos = level.lobbyCentre();
		level.mobs.add(boss);
		level.locked = true;
	}

	// ---------- what it does ----------

	private boolean announced = false;
	//its patterns only start counting down once it has first seen an enemy
	private boolean engaged = false;
	private float helloCooldown = HELLO_TURNS;
	private float goodbyeCooldown = GOODBYE_TURNS;
	//the cells marked by Good Bye, struck on its next turn
	private int[] goodbyeCells = null;
	private int goodbyeFacing = -1;

	@Override
	protected boolean act() {
		if (!announced){
			announced = true;
			BossHealthBar.assignBoss(this);
			Sample.INSTANCE.play(Assets.Sounds.BOSS);
		} else if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss(this);
		}

		if (paralysed > 0 || !Dungeon.hero.isAlive()){
			return super.act();
		}

		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);

		if (engaged){
			helloCooldown--;
			goodbyeCooldown--;
		} else if (!visibleEnemies().isEmpty()){
			engaged = true;
		}

		if (goodbyeCells != null){
			return goodbye();
		}

		//it always knows where the hero is
		if (enemy == null || !enemy.isAlive() || !Actor.chars().contains(enemy)){
			Char chosen = chooseEnemy();
			enemy = chosen != null ? chosen : Dungeon.hero;
		}
		state = HUNTING;
		target = enemy.pos;

		if (goodbyeCooldown <= 0 && Dungeon.level.adjacent(pos, enemy.pos)){
			windup(enemy.pos);
			return true;
		}
		if (helloCooldown <= 0){
			//the spike goes at the enemy in sight it is most set on, the next one if that one can't be reached
			for (Char t : byAggro(visibleEnemies())){
				List<Integer> line = helloLine(t);
				if (line != null){
					return hello(line);
				}
			}
		}

		return super.act();
	}

	// ---------- Hello? ----------

	//the hero and their allies that it can see
	private ArrayList<Char> visibleEnemies(){
		ArrayList<Char> enemies = new ArrayList<>();
		for (Mob mob : Dungeon.level.mobs){
			if (mob.alignment == Alignment.ALLY && fieldOfView[mob.pos] && mob.invisible <= 0) enemies.add(mob);
		}
		if (Dungeon.hero.isAlive() && fieldOfView[Dungeon.hero.pos] && Dungeon.hero.invisible <= 0){
			enemies.add(Dungeon.hero);
		}
		return enemies;
	}

	//the same order a monster picks its enemy in: anything under aggression first, then an afterimage,
	// then the closest one, the hero first among those just as close. Whatever is charming it comes last
	private ArrayList<Char> byAggro( ArrayList<Char> enemies ){
		PathFinder.buildDistanceMap(pos, Dungeon.findPassable(this, Dungeon.level.passable, fieldOfView, true));
		final HashMap<Char, Integer> rank = new HashMap<>();
		final HashMap<Char, Integer> distances = new HashMap<>();
		Charm charm = buff(Charm.class);
		for (Char ch : enemies){
			int dist = Integer.MAX_VALUE;
			for (int n : PathFinder.NEIGHBOURS8){
				dist = Math.min(dist, PathFinder.distance[ch.pos + n]);
			}
			int group;
			if (charm != null && charm.object == ch.id())                          group = 3;
			else if (ch.buff(StoneOfAggression.Aggression.class) != null)         group = 0;
			else if (ch instanceof Feint.AfterImage)                              group = 1;
			else                                                                  group = 2;
			rank.put(ch, group);
			distances.put(ch, dist);
		}
		ArrayList<Char> sorted = new ArrayList<>(enemies);
		Collections.sort(sorted, new Comparator<Char>() {
			@Override
			public int compare( Char a, Char b ) {
				int g = Integer.compare(rank.get(a), rank.get(b));
				if (g != 0) return g;
				int d = Integer.compare(distances.get(a), distances.get(b));
				if (d != 0) return d;
				return Boolean.compare(b == Dungeon.hero, a == Dungeon.hero);
			}
		});
		return sorted;
	}

	//the cells a spike at the given enemy flies through, or null if it can't be reached in a straight line
	private List<Integer> helloLine( Char target ){
		if (!fieldOfView[target.pos] || target.invisible > 0) return null;
		Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
		if (b.dist < 1) return null;
		List<Integer> line = b.subPath(1, Math.min(b.dist, HELLO_RANGE));
		return line.contains(target.pos) ? new ArrayList<>(line) : null;
	}

	private boolean hello( final List<Integer> line ){
		helloCooldown = HELLO_TURNS;
		speak(Messages.get(this, "hello"));
		spend(attackDelay());

		final int end = line.get(line.size()-1);
		boolean visible = sprite != null && (sprite.visible || Dungeon.level.heroFOV[end]);
		if (visible){
			((NothingThereSprite) sprite).spit(end, new Callback() {
				@Override
				public void call() {
					helloHits(line);
					next();
				}
			});
			Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.6f);
			return false;
		} else {
			helloHits(line);
			return true;
		}
	}

	private void helloHits( List<Integer> line ){
		for (int cell : line){
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != this && ch.isAlive()){
				//aimed like its claws, so it can be dodged
				strike(ch, Random.NormalIntRange(80, 100), false);
			}
		}
	}

	// ---------- Good Bye ----------

	//the cell in front of it and the two either side of that one
	private int[] frontCells( int facing ){
		int dir = facing - pos;
		int[] circle = PathFinder.CIRCLE8;
		for (int i = 0; i < circle.length; i++){
			if (circle[i] == dir){
				return new int[]{ pos + circle[(i + 7) % 8], pos + circle[i], pos + circle[(i + 1) % 8] };
			}
		}
		return new int[]{ facing };
	}

	private void windup( int facing ){
		goodbyeCooldown = GOODBYE_TURNS;
		goodbyeFacing = facing;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int c : frontCells(facing)){
			if (Dungeon.level.insideMap(c) && !Dungeon.level.solid[c]) cells.add(c);
		}
		goodbyeCells = new int[cells.size()];
		for (int i = 0; i < cells.size(); i++){
			goodbyeCells[i] = cells.get(i);
			GameScene.targetedCell(cells.get(i), TICK);
		}
		if (sprite != null){
			((NothingThereSprite) sprite).windup(facing);
			if (Dungeon.level.heroFOV[pos]) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "goodbye_warn"));
		}
		spend(TICK);
	}

	private boolean goodbye(){
		int[] cells = goodbyeCells;
		goodbyeCells = null;
		speak(Messages.get(this, "goodbye"));
		if (sprite != null) ((NothingThereSprite) sprite).swing(goodbyeFacing);
		if (Dungeon.level.heroFOV[pos]){
			Sample.INSTANCE.play(Assets.Sounds.BLAST, 1f, 0.7f);
			Camera.main.shake(3, 0.3f);
		}
		for (int cell : cells){
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != this && ch.isAlive()){
				strike(ch, GOODBYE_DAMAGE, true);
			}
		}
		spend(attackDelay());
		return true;
	}

	//a hit from one of its patterns, armor and RED resistance still count
	private void strike( Char ch, int damage, boolean sure ){
		sureHit = sure;
		attack(ch, 0f, damage, 1f);
		sureHit = false;
	}

	private void speak( String line ){
		if (sprite != null && Dungeon.level.heroFOV[pos]){
			sprite.showStatus(CharSprite.NEGATIVE, line);
		}
		yell(line);
	}

	@Override
	public void die( Object cause ) {
		super.die(cause);
		//L Corp headquarters opens again, the stairs in Malkuth's room lead back up
		Dungeon.level.locked = false;
		GLog.p(Messages.get(this, "suppressed"));
		Angela.Quest.onBossSuppressed(Angela.NOTHING_THERE);
	}

	@Override
	public void beckon( int cell ) {
		//it is already coming
	}

	private static final String ANNOUNCED      = "announced";
	private static final String ENGAGED        = "engaged";
	private static final String HELLO_CD       = "hello_cd";
	private static final String GOODBYE_CD     = "goodbye_cd";
	private static final String GOODBYE_CELLS  = "goodbye_cells";
	private static final String GOODBYE_FACING = "goodbye_facing";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ANNOUNCED, announced);
		bundle.put(ENGAGED, engaged);
		bundle.put(HELLO_CD, helloCooldown);
		bundle.put(GOODBYE_CD, goodbyeCooldown);
		if (goodbyeCells != null) bundle.put(GOODBYE_CELLS, goodbyeCells);
		bundle.put(GOODBYE_FACING, goodbyeFacing);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		announced = bundle.getBoolean(ANNOUNCED);
		engaged = bundle.getBoolean(ENGAGED);
		helloCooldown = bundle.getFloat(HELLO_CD);
		goodbyeCooldown = bundle.getFloat(GOODBYE_CD);
		goodbyeCells = bundle.contains(GOODBYE_CELLS) ? bundle.getIntArray(GOODBYE_CELLS) : null;
		goodbyeFacing = bundle.getInt(GOODBYE_FACING);
		BossHealthBar.assignBoss(this);
	}
}
