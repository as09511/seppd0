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
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HarmonyOfSkinSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

//Noon of Crimson: a quick heap of flesh and grinning masks. When it dies it splits into
//two Cheers for the Beginning that scatter across the floor, and the ordeal goes on until they fall too.
public class HarmonyOfSkin extends OrdealMob {

	public static final int SPLIT = 2;

	{
		spriteClass = HarmonyOfSkinSprite.class;

		HP = HT = 225;
		defenseSkill = 20;
		EXP = 15;
		baseSpeed = 1.5f;

		tier = Angela.NOON;
		maxLvl = NOON_MAX_LVL;
		risk = RiskLevel.HE;

		attackType = DamageType.RED;
		damageMultipliers = new float[]{0.5f, 1.5f, 1.2f, 1.5f};
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(10, 25);
	}

	@Override
	public int attackSkill( Char target ) {
		return 20;
	}

	@Override
	public void die( Object cause ) {
		//the Cheers are on the floor before the ordeal checks whether anything is left
		boolean seen = Dungeon.level.heroFOV[pos];
		for (int i = 0; i < SPLIT; i++){
			CheersForTheBeginning cheers = new CheersForTheBeginning();
			cheers.tier(tier);
			Angela.Quest.place(cheers, Dungeon.level.randomRespawnCell(cheers));
			if (cheers.sprite != null){
				CellEmitter.get(cheers.pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);
			}
		}
		if (seen){
			CellEmitter.center(pos).burst(BloodParticle.BURST, 16);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.w(Messages.get(this, "split"));
		}
		super.die(cause);
	}
}
