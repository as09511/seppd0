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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WhereWeMustReachSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Dusk of Green: a great machine that never moves and never strikes. It builds the machines of the earlier ordeals:
//three Doubts and a Process of Understanding as it appears, then one Doubt every 10 turns, every 4th one a Process
//of Understanding instead. Each blow it takes doubles its output for 5 turns.
public class WhereWeMustReach extends OrdealMob {

	public static final int FIRST_DOUBTS   = 3;
	public static final int BUILD_TURNS    = 10;
	public static final int PROCESS_EVERY  = 4;
	public static final float RUSH_TURNS   = 5f;

	{
		spriteClass = WhereWeMustReachSprite.class;

		HP = HT = 1000;
		defenseSkill = 0;
		EXP = 40;

		tier = Angela.DUSK;
		maxLvl = DUSK_MAX_LVL;
		risk = RiskLevel.WAW;

		attackType = DamageType.RED;
		damageMultipliers = new float[]{0.8f, 1.0f, 1.2f, 1.0f};

		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		immunities.add(Paralysis.class);

		state = PASSIVE;
	}

	//whether the first machines have been built yet
	private boolean started = false;
	//progress toward the next machine, it builds one at BUILD_TURNS
	private float progress = 0;
	//machines built since the first ones
	private int built = 0;
	//turns left of doubled output after being struck
	private float rush = 0;

	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView(this, fieldOfView);
		throwItems();

		if (!started){
			started = true;
			for (int i = 0; i < FIRST_DOUBTS; i++) build(new Doubt());
			build(new ProcessOfUnderstanding());
		} else {
			progress += rush > 0 ? 2 : 1;
			if (rush > 0) rush--;
			if (progress >= BUILD_TURNS){
				progress -= BUILD_TURNS;
				built++;
				build(built % PROCESS_EVERY == 0 ? new ProcessOfUnderstanding() : new Doubt());
			}
		}

		spend(TICK);
		return true;
	}

	//a new machine of this ordeal, next to it if there is room
	private void build( OrdealMob machine ){
		machine.tier(tier);
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.solid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		Angela.Quest.place(machine, cells.isEmpty() ? Dungeon.level.randomRespawnCell(machine) : Random.element(cells));
		if (machine.sprite != null && machine.pos >= 0){
			CellEmitter.get(machine.pos).burst(Speck.factory(Speck.LIGHT), 4);
			if (Dungeon.level.heroFOV[pos]) Sample.INSTANCE.play(Assets.Sounds.EVOKE, 0.6f, 0.6f);
		}
		if (sprite != null) ((WhereWeMustReachSprite) sprite).build();
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(dmg, src);
		if (isAlive() && dmg > 0){
			if (rush <= 0 && sprite != null && Dungeon.level.heroFOV[pos]){
				sprite.showStatus(CharSprite.WARNING, Messages.get(this, "rush"));
			}
			rush = RUSH_TURNS;
		}
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
	protected boolean canAttack( Char enemy ) {
		return false;
	}

	@Override
	public void beckon( int cell ) {
		//it doesn't come
	}

	private static final String STARTED  = "started";
	private static final String PROGRESS = "progress";
	private static final String BUILT    = "built";
	private static final String RUSH     = "rush";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(STARTED, started);
		bundle.put(PROGRESS, progress);
		bundle.put(BUILT, built);
		bundle.put(RUSH, rush);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		started = bundle.getBoolean(STARTED);
		progress = bundle.getFloat(PROGRESS);
		built = bundle.getInt(BUILT);
		rush = bundle.getFloat(RUSH);
	}
}
