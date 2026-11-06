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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.LCorpHQLevel;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FoodChainSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

//Dusk of Amber: a great burrowing thing that fills a whole corridor of L Corp headquarters (2x2 cells).
//It crawls from one end of its corridor to the other at half speed and only strikes what is right in front of it,
//knocking it back. Its own kind in the way swap places with it. At the far end it
//sinks away and comes up at the end of another corridor. In each corridor, as room opens up behind it,
//it leaves Perfect Food in the corridor behind it, up to 4 in each corridor.
//The cell it stands on is its bottom left, three Body parts take up the rest and pass on what they are dealt.
public class FoodChain extends OrdealMob {

	public static final int MAX_FOOD = 4;

	{
		spriteClass = FoodChainSprite.class;

		HP = HT = 550;
		defenseSkill = 15;
		EXP = 30;
		baseSpeed = 0.5f;

		tier = Angela.DUSK;
		maxLvl = DUSK_MAX_LVL;
		risk = RiskLevel.WAW;

		attackType = DamageType.RED;
		damageMultipliers = new float[]{1.2f, 0.8f, 0.5f, 2.0f};

		properties.add(Property.IMMOVABLE);
		//its other cells can't change sides with it
		immunities.add(Corruption.class);
		immunities.add(Charm.class);
		immunities.add(Amok.class);

		state = HUNTING;
	}

	//which corridor of L Corp headquarters it is crawling through, and which way along it (+1 right/down, -1 left/up)
	private int corridor = 0;
	private int dir = 1;
	//how much Perfect Food it has left in this corridor
	private int fedHere = 0;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(45, 65);
	}

	@Override
	public int attackSkill( Char target ) {
		return 35;
	}

	// ---------- geometry ----------

	private static int W(){
		return Dungeon.level.width();
	}

	private static boolean horizontal( int[] c ){
		return c[2] > c[3];
	}

	//the bottom left cell of the body at the end of a corridor it starts from when crawling dir-wards
	private static int startCell( int[] c, int dir ){
		int w = W();
		if (horizontal(c)){
			int x = dir > 0 ? c[0] : c[0] + c[2] - 2;
			return x + (c[1] + 1)*w;
		} else {
			int y = dir > 0 ? c[1] + 1 : c[1] + c[3] - 1;
			return c[0] + y*w;
		}
	}

	private int[] corridorRect(){
		ArrayList<int[]> all = LCorpHQLevel.corridors();
		return all.get(Math.max(0, Math.min(corridor, all.size()-1)));
	}

	//one step along the corridor
	private int step(){
		return horizontal(corridorRect()) ? dir : dir*W();
	}

	//the cells of the body: this one, then those of the three parts
	private static int[] bodyCells( int anchor ){
		int w = W();
		return new int[]{anchor, anchor + 1, anchor - w, anchor - w + 1};
	}

	//the two cells right in front of it, one per lane
	private int[] front( int anchor ){
		int w = W();
		if (horizontal(corridorRect())){
			int x = dir > 0 ? anchor + 2 : anchor - 1;
			return new int[]{x, x - w};
		} else {
			int y = dir > 0 ? anchor + w : anchor - 2*w;
			return new int[]{y, y + 1};
		}
	}

	//the two cells right behind it, in the same lanes
	private int[] behind( int anchor ){
		dir = -dir;
		int[] r = front(anchor);
		dir = -dir;
		return r;
	}

	private boolean atEnd(){
		return pos == startCell(corridorRect(), -dir);
	}

	private static boolean inside( int cell ){
		return cell >= 0 && cell < Dungeon.level.length();
	}

	// ---------- the parts ----------

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
	private Body[] ensureParts(){
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
				parts[i] = b;
			}
		}
		return parts;
	}

	private void placeBody( int anchor, boolean animate ){
		int old = pos;
		Body[] parts = parts();
		int[] cells = bodyCells(anchor);
		pos = anchor;
		for (int i = 0; i < 3; i++){
			if (parts[i] != null){
				parts[i].pos = cells[i+1];
				if (parts[i].sprite != null) parts[i].sprite.place(cells[i+1]);
			}
		}
		if (sprite != null){
			if (animate) moveSprite(old, anchor);
			else sprite.place(anchor);
		}
		for (int c : cells) Dungeon.level.occupyCell(Actor.findChar(c));
	}

	//puts a Food Chain and its parts at one end of a corridor of a level that is being arrived at
	public static boolean spawn( LCorpHQLevel level, int corridor, int heroPos ){
		int[] c = LCorpHQLevel.corridors().get(corridor);
		int dir = Random.Int(2) == 0 ? 1 : -1;
		int anchor = startCell(c, dir);
		for (int cell : bodyCells(anchor)){
			if (!level.passable[cell] || level.findMob(cell) != null || cell == heroPos
					|| level.distance(cell, heroPos) < 4) return false;
		}
		FoodChain chain = new FoodChain();
		chain.corridor = corridor;
		chain.dir = dir;
		chain.pos = anchor;
		level.mobs.add(chain);
		int[] cells = bodyCells(anchor);
		for (int i = 0; i < 3; i++){
			Body b = new Body();
			b.owner = chain.id();
			b.index = i;
			b.pos = cells[i+1];
			level.mobs.add(b);
		}
		return true;
	}

	//the Food Chain or one of its parts, whichever is next to the hero (or else the one given)
	public static Char nearestCell( Char ch, Char hero ){
		FoodChain chain = ch instanceof FoodChain ? (FoodChain) ch
				: ch instanceof Body ? ((Body) ch).owner() : null;
		if (chain == null || Dungeon.level.adjacent(ch.pos, hero.pos)) return ch;
		for (int c : bodyCells(chain.pos)){
			Char part = Actor.findChar(c);
			if (part != null && (part == chain || part instanceof Body) && Dungeon.level.adjacent(c, hero.pos)) return part;
		}
		return ch;
	}

	// ---------- what it does ----------

	private boolean foe( Char ch ){
		return ch != null && ch.isAlive() && !(ch instanceof Body)
				&& ch.alignment != alignment && ch.alignment != Alignment.NEUTRAL;
	}

	private final ArrayList<Char> targets = new ArrayList<>();

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);

		//it only knows the corridors of L Corp headquarters
		if (!(Dungeon.level instanceof LCorpHQLevel)){
			spend(TICK);
			return true;
		}
		ensureParts();
		if (sprite instanceof FoodChainSprite) ((FoodChainSprite) sprite).face(facing());

		if (paralysed > 0){
			spend(TICK);
			return true;
		}

		//whatever stands right in front of it is struck
		targets.clear();
		for (int c : front(pos)){
			if (inside(c) && foe(Actor.findChar(c))) targets.add(Actor.findChar(c));
		}
		if (!targets.isEmpty()){
			enemy = targets.get(0);
			if (sprite != null && (sprite.visible || enemy.sprite.visible)){
				sprite.attack(enemy.pos);
				return false;
			}
			strike();
			spend(attackDelay());
			return true;
		}

		leaveFood();

		if (atEnd()){
			burrow();
			spend(TICK);
			return true;
		}

		if (rooted || !crawl()){
			spend(TICK);
			return true;
		}
		spend(1f / speed());
		return true;
	}

	@Override
	public void onAttackComplete() {
		strike();
		Invisibility.dispel(this);
		spend(attackDelay());
		next();
	}

	//everything in front of it is struck and knocked back along the corridor
	private void strike(){
		int push = step();
		for (Char t : new ArrayList<>(targets)){
			boolean stillThere = false;
			for (int c : front(pos)) if (t.pos == c) stillThere = true;
			if (!stillThere || !t.isAlive()) continue;
			if (attack(t) && t.isAlive()){
				if (!Char.hasProp(t, Property.IMMOVABLE)){
					WandOfBlastWave.throwChar(t, new Ballistica(t.pos, t.pos + push, Ballistica.MAGIC_BOLT),
							1, false, false, this);
				}
			}
		}
		targets.clear();
	}

	//one step along the corridor, its own kind in the way swaps places with it
	private boolean crawl(){
		int[] ahead = front(pos);

		//another Food Chain right in front: the two of them trade places whole, and each goes on its way
		FoodChain other = chainAt(ahead[0]);
		if (other != null && other != this && other.isAlive() && other.pos == pos + 2*step()){
			int mine = pos;
			placeBody(other.pos, true);
			other.placeBody(mine, true);
			return true;
		}

		Char[] inWay = new Char[2];
		for (int i = 0; i < 2; i++){
			int c = ahead[i];
			if (!inside(c) || !Dungeon.level.passable[c]) return false;
			Char ch = Actor.findChar(c);
			if (ch != null){
				if (ch.alignment != alignment || Char.hasProp(ch, Property.IMMOVABLE)) return false;
				inWay[i] = ch;
			}
		}
		//the rear lane cells it leaves: for a step right or down, those are its left or top cells
		int[] left = behindLanes(pos);
		placeBody(pos + step(), true);
		for (int i = 0; i < 2; i++){
			if (inWay[i] != null){
				Char ch = inWay[i];
				int from = ch.pos;
				ch.pos = left[i];
				if (ch.sprite != null) ch.sprite.move(from, left[i]);
				Dungeon.level.occupyCell(ch);
			}
		}
		return true;
	}

	//the Food Chain whose body covers a cell, if any
	private static FoodChain chainAt( int cell ){
		if (!inside(cell)) return null;
		Char ch = Actor.findChar(cell);
		if (ch instanceof FoodChain) return (FoodChain) ch;
		if (ch instanceof Body) return ((Body) ch).owner();
		return null;
	}

	//the two cells of the body at its rear, one per lane, which a step forward leaves empty
	private int[] behindLanes( int anchor ){
		int[] b = behind(anchor);
		int s = step();
		return new int[]{b[0] + s, b[1] + s};
	}

	//whether a cell lies in the corridor it is crawling through
	private boolean inCorridor( int cell ){
		int[] c = corridorRect();
		int x = cell % W(), y = cell / W();
		return x >= c[0] && x < c[0] + c[2] && y >= c[1] && y < c[1] + c[3];
	}

	//as it crawls on, it leaves Perfect Food in the corridor cells it frees behind it, up to 4 in each corridor
	// (what it left in other corridors doesn't count)
	private void leaveFood(){
		boolean seen = false;
		for (int c : behind(pos)){
			if (fedHere >= MAX_FOOD) break;
			if (!inside(c) || !inCorridor(c) || !Dungeon.level.passable[c] || Dungeon.level.solid[c]
					|| Actor.findChar(c) != null){
				continue;
			}
			PerfectFood f = new PerfectFood();
			f.tier(tier);
			Angela.Quest.place(f, c);
			if (f.pos == c){
				fedHere++;
				if (Dungeon.level.heroFOV[c]){
					CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
					seen = true;
				}
			}
		}
		if (seen && sprite != null) sprite.showStatus(0xFFB040, Messages.get(this, "food"));
	}

	//at the end of its corridor it sinks away and comes up at the end of another
	private void burrow(){
		ArrayList<Integer> order = new ArrayList<>();
		for (int i = 0; i < LCorpHQLevel.corridors().size(); i++) if (i != corridor) order.add(i);
		Collections.shuffle(order, new java.util.Random(Random.Long()));
		for (int c : order){
			int d = Random.Int(2) == 0 ? 1 : -1;
			for (int t = 0; t < 2; t++, d = -d){
				int anchor = startCell(LCorpHQLevel.corridors().get(c), d);
				boolean free = true;
				for (int cell : bodyCells(anchor)){
					Char ch = Actor.findChar(cell);
					if (!Dungeon.level.passable[cell] || (ch != null && ch != this && !(ch instanceof Body && ((Body) ch).owner == id()))){
						free = false;
					}
				}
				if (!free) continue;

				boolean seenBefore = Dungeon.level.heroFOV[pos];
				if (seenBefore) CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 8);
				corridor = c;
				dir = d;
				fedHere = 0;
				placeBody(anchor, false);
				if (sprite != null) ((FoodChainSprite) sprite).face(horizontal(corridorRect()) ? dir : 0);
				if (Dungeon.level.heroFOV[pos]){
					CellEmitter.get(pos).burst(Speck.factory(Speck.ROCK), 8);
				}
				if (seenBefore || Dungeon.level.heroFOV[pos]){
					Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.8f, 0.8f);
				}
				return;
			}
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

	public int facing(){
		return horizontal(corridorRect()) ? dir : 0;
	}

	private static final String CORRIDOR = "corridor";
	private static final String DIR      = "dir";
	private static final String FED      = "fed_here";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CORRIDOR, corridor);
		bundle.put(DIR, dir);
		bundle.put(FED, fedHere);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		corridor = bundle.getInt(CORRIDOR);
		dir = bundle.getInt(DIR);
		if (dir == 0) dir = 1;
		fedHere = bundle.getInt(FED);
	}

	//the other three cells of a Food Chain's body: it can't be told apart from the Food Chain,
	//everything done to it is done to the Food Chain instead, and it goes when the Food Chain does
	public static class Body extends Mob {

		{
			spriteClass = FoodChainSprite.Body.class;

			HP = HT = 550;
			defenseSkill = 15;
			EXP = 0;
			maxLvl = -2;

			properties.add(Property.IMMOVABLE);

			state = PASSIVE;
			alignment = Alignment.ENEMY;
		}

		private int owner = -1;
		private int index = -1;

		public FoodChain owner(){
			Actor a = Actor.findById(owner);
			return a instanceof FoodChain && ((FoodChain) a).isAlive() ? (FoodChain) a : null;
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
			FoodChain o = owner();
			if (o != null) o.damage(dmg, src);
		}

		@Override
		public boolean add( Buff buff ) {
			return false;
		}

		@Override
		public int defenseSkill( Char enemy ) {
			FoodChain o = owner();
			return o != null ? o.defenseSkill(enemy) : defenseSkill;
		}

		@Override
		public int drRoll() {
			FoodChain o = owner();
			return o != null ? o.drRoll() : 0;
		}

		@Override
		public float damageMultiplier( DamageType type ) {
			FoodChain o = owner();
			return o != null ? o.damageMultiplier(type) : super.damageMultiplier(type);
		}

		@Override
		public String name() {
			return Messages.get(FoodChain.class, "name");
		}

		@Override
		public String info() {
			FoodChain o = owner();
			return o != null ? o.info() : Messages.get(FoodChain.class, "desc");
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
