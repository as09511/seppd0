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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CheersForTheBeginningSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Dawn of Crimson: a little reveller that never moves or attacks. Once it spots the hero it cheers for
//5 turns, then calls one of the floor's own monsters to its side and vanishes elsewhere on the floor.
public class CheersForTheBeginning extends OrdealMob {

	public static final int CHEER_TURNS = 5;

	{
		spriteClass = CheersForTheBeginningSprite.class;

		HP = HT = 80;
		defenseSkill = 8;
		EXP = 10;

		damageMultipliers = new float[]{0.8f, 1.3f, 1.3f, 2.0f};

		state = PASSIVE;
	}

	//turns left until it calls a monster, -1 while it has not spotted the hero
	private int cheer = -1;

	@Override
	protected boolean act() {
		if (paralysed <= 0 && fieldOfView != null && fieldOfView.length == Dungeon.level.length()){
			cheerTurn();
		}
		return super.act();
	}

	private void cheerTurn(){
		if (cheer < 0){
			if (fieldOfView[Dungeon.hero.pos] && Dungeon.hero.invisible <= 0){
				cheer = CHEER_TURNS;
				if (sprite != null){
					sprite.showAlert();
					if (Dungeon.level.heroFOV[pos]) GLog.w(Messages.get(this, "spotted"));
				}
			}
			return;
		}
		cheer--;
		if (sprite != null && Dungeon.level.heroFOV[pos]){
			sprite.showStatus(0xFF4060, Integer.toString(cheer));
		}
		if (cheer <= 0){
			cheer = -1;
			callMonster();
			ScrollOfTeleportation.teleportChar(this);
		}
	}

	//one of the floor's own monsters comes to its side, already after the hero
	private void callMonster(){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int c = pos + i;
			if (Dungeon.level.passable[c] && !Dungeon.level.solid[c] && Actor.findChar(c) == null){
				cells.add(c);
			}
		}
		Mob mob = Dungeon.level.createMob();
		int cell = cells.isEmpty() ? Dungeon.level.randomRespawnCell(mob) : Random.element(cells);
		if (cell == -1) return;
		mob.pos = cell;
		GameScene.add(mob);
		mob.aggro(Dungeon.hero);
		if (sprite != null){
			sprite.attack(cell);
			if (Dungeon.level.heroFOV[pos]){
				sprite.centerEmitter().burst(Speck.factory(Speck.NOTE), 6);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.8f, 1.3f);
				GLog.w(Messages.get(this, "called", mob.name()));
			}
		}
	}

	//it never moves or attacks
	@Override
	protected boolean canAttack( Char enemy ) {
		return false;
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
	public int damageRoll() {
		return 0;
	}

	private static final String CHEER = "cheer";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CHEER, cheer);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		cheer = bundle.contains(CHEER) ? bundle.getInt(CHEER) : -1;
	}
}
