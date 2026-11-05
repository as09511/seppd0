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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.ProcessOfUnderstanding;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

//Process of Understanding, Noon of Green: a box-headed machine with red eyes, a chainsaw arm and a cloth-bound gun
public class ProcessOfUnderstandingSprite extends MobSprite {

	private Animation spark;

	public ProcessOfUnderstandingSprite() {
		super();

		texture( Assets.Sprites.PROCESS_OF_UNDERSTANDING );

		TextureFilm frames = new TextureFilm( texture, 20, 18 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1 );

		run = new Animation( 10, true );
		run.frames( frames, 2, 3, 4, 5 );

		attack = new Animation( 15, false );
		attack.frames( frames, 6, 7, 8, 0 );

		zap = new Animation( 12, false );
		zap.frames( frames, 9, 10, 0 );

		spark = new Animation( 6, true );
		spark.frames( frames, 11, 12 );

		die = new Animation( 10, false );
		die.frames( frames, 13, 14, 15 );

		play( idle );
	}

	public void spark( boolean on ){
		if (on){
			//an attack or shot still playing finishes first, idle() then picks the sparks up
			if (curAnim != spark && curAnim != attack && curAnim != zap && curAnim != die) play(spark);
		} else if (curAnim == spark){
			idle();
		}
	}

	//while it overheats it keeps sparking between its other animations
	@Override
	public void idle() {
		if (ch instanceof ProcessOfUnderstanding && ((ProcessOfUnderstanding) ch).sparking()){
			play(spark);
		} else {
			super.idle();
		}
	}

	@Override
	public void zap( int cell ) {
		super.zap( cell );
		MagicMissile.boltFromChar( parent, MagicMissile.SHAMAN_RED, this, cell, new Callback() {
			@Override
			public void call() {
				((ProcessOfUnderstanding)ch).onZapComplete();
			}
		} );
		Sample.INSTANCE.play( Assets.Sounds.HIT_CRUSH, 1f, 0.8f );
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap) {
			idle();
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFF2E2C32;
	}
}
