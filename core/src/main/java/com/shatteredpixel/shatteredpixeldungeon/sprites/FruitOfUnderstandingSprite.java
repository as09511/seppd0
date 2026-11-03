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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal.FruitOfUnderstanding;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Fruit of Understanding, Dawn of Violet: a low fleshy violet mound with a spiny ridge.
//While it charges, the 5x5 area it will strike is marked with violet particles, like Goo's pump up.
public class FruitOfUnderstandingSprite extends MobSprite {

	private Animation charge;
	private Animation blast;

	private int lastChargePos = -1;
	private boolean charging = false;
	private ArrayList<Emitter> warnEmitters = new ArrayList<>();

	public FruitOfUnderstandingSprite() {
		super();

		texture( Assets.Sprites.FRUIT_OF_UNDERSTANDING );

		TextureFilm frames = new TextureFilm( texture, 20, 14 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 1, 1 );

		run = new Animation( 6, true );
		run.frames( frames, 2, 3 );

		charge = new Animation( 8, true );
		charge.frames( frames, 4, 5 );

		blast = new Animation( 10, false );
		blast.frames( frames, 6, 6, 7 );

		attack = new Animation( 12, false );
		attack.frames( frames, 4, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 8, 9, 10 );

		play( idle );
	}

	//0 stops charging, 1 or 2 is the turn of the charge
	public void charge( int turn ){
		if (turn <= 0){
			charging = false;
			clearEmitters();
			return;
		}
		if (curAnim != charge) play(charge);
		if (!charging || turn > 1){
			Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 1f, turn == 1 ? 0.7f : 0.85f );
		}
		charging = true;
		updateEmitters();
	}

	public void blast(){
		for (Emitter e : warnEmitters){
			e.burst( VioletParticle.BURST, 10 );
		}
		warnEmitters.clear();
		charging = false;
		play( blast );
	}

	private void updateEmitters(){
		clearEmitters();
		if (!charging || ch == null) return;
		lastChargePos = ch.pos;
		int range = FruitOfUnderstanding.BLAST_RANGE;
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.heroFOV[i]
					&& Dungeon.level.distance(i, ch.pos) <= range
					&& new Ballistica(ch.pos, i, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == i
					&& new Ballistica(i, ch.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == ch.pos){
				Emitter e = CellEmitter.get(i);
				e.pour( VioletParticle.FACTORY, 0.04f );
				warnEmitters.add(e);
			}
		}
	}

	private void clearEmitters(){
		for (Emitter e : warnEmitters){
			e.on = false;
		}
		warnEmitters.clear();
	}

	@Override
	public void play( Animation anim ) {
		if (anim != charge && anim != blast){
			charging = false;
			clearEmitters();
		}
		super.play(anim);
	}

	@Override
	public void update() {
		super.update();
		if (charging && ch != null && ch.pos != lastChargePos){
			updateEmitters();
		}
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == blast){
			idle();
			return;
		}
		super.onComplete(anim);
	}

	@Override
	public int blood() {
		return 0xFF9852C0;
	}

	public static class VioletParticle extends PixelParticle.Shrinking {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((VioletParticle)emitter.recycle( VioletParticle.class )).reset( x, y, false );
			}
		};

		public static final Emitter.Factory BURST = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((VioletParticle)emitter.recycle( VioletParticle.class )).reset( x, y, true );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public VioletParticle() {
			super();
			lifespan = 0.3f;
			acc.set( 0, +50 );
		}

		public void reset( float x, float y, boolean burst ) {
			revive();

			this.x = x;
			this.y = y;

			color( burst ? 0xD08CF0 : 0x8A3CC0 );
			left = lifespan = burst ? 0.5f : 0.3f;

			size = 4;
			speed.polar( -Random.Float( PointF.PI ), burst ? Random.Float( 48, 80 ) : Random.Float( 32, 48 ) );
		}

		@Override
		public void update() {
			super.update();
			float p = left / lifespan;
			am = p > 0.5f ? (1 - p) * 2f : 1;
		}
	}
}
