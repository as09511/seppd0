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
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Angela;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BloodParticle;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ClimaxThroesSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

//Dusk of Crimson: a stitched heap of flesh swinging a cleaver and an axe. It runs twice as fast as anyone,
//but squeezing through a doorway takes it 2 turns. When it dies it splits into two Harmonies of Skin,
//which split into Cheers for the Beginning in their turn, and the ordeal goes on until the last of them falls.
public class ClimaxThroes extends OrdealMob {

	public static final int SPLIT = 2;
	public static final float DOOR_TIME = 2f;

	{
		spriteClass = ClimaxThroesSprite.class;

		HP = HT = 850;
		defenseSkill = 25;
		EXP = 30;
		baseSpeed = 2f;

		tier = Angela.DUSK;
		maxLvl = DUSK_MAX_LVL;
		risk = RiskLevel.WAW;

		attackType = DamageType.RED;
		damageMultipliers = new float[]{0.6f, 0.8f, 0.8f, 1.5f};
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(20, 35);
	}

	@Override
	public int attackSkill( Char target ) {
		return 25;
	}

	private static boolean door( int cell ){
		int t = Dungeon.level.map[cell];
		return t == Terrain.DOOR || t == Terrain.OPEN_DOOR || t == Terrain.LOCKED_DOOR || t == Terrain.CRYSTAL_DOOR;
	}

	@Override
	public void move( int step, boolean travelling ) {
		boolean squeeze = travelling && door(step);
		super.move(step, travelling);
		//the step itself costs 1/speed, the rest of the 2 turns comes on top
		if (squeeze) spend(DOOR_TIME - 1f/speed());
	}

	@Override
	public void die( Object cause ) {
		//the Harmonies are on the floor before the ordeal checks whether anything is left
		boolean seen = Dungeon.level.heroFOV[pos];
		for (int i = 0; i < SPLIT; i++){
			HarmonyOfSkin harmony = new HarmonyOfSkin();
			harmony.tier(tier);
			Angela.Quest.place(harmony, Dungeon.level.randomRespawnCell(harmony));
			if (harmony.sprite != null){
				CellEmitter.get(harmony.pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);
			}
		}
		if (seen){
			CellEmitter.center(pos).burst(BloodParticle.BURST, 24);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.w(Messages.get(this, "split"));
		}
		super.die(cause);
	}
}
