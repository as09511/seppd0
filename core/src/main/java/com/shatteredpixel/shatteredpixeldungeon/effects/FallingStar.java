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


package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//our galaxy's starlight: a blue star that falls from the sky onto a point, trailing sparks,
//then bursts and calls back
public class FallingStar extends Group {

	private static final float SPEED = 320f;

	private final Image star;
	private final Emitter trail;
	private final PointF to;
	private final PointF from;
	private final float duration;
	private final Callback callback;
	private float time = 0;
	private boolean landed = false;

	public FallingStar( PointF to, Callback callback ){
		super();
		this.to = to;
		this.from = new PointF(to.x + Random.Float(-20f, 20f), to.y - 180f);
		this.duration = PointF.distance(from, to) / SPEED;
		this.callback = callback;

		trail = new Emitter();
		trail.pour(BlueSpark.FACTORY, 0.01f);
		add(trail);

		star = new Image(Assets.Effects.SPECKS);
		star.frame(new TextureFilm(star.texture, 7, 7).get(Speck.STAR));
		star.origin.set(star.width/2f, star.height/2f);
		star.scale.set(2f);
		star.tint(0x4A8CFF, 0.85f);
		add(star);

		place(from);
	}

	private void place( PointF p ){
		star.x = p.x - star.width/2f;
		star.y = p.y - star.height/2f;
		trail.pos(p.x, p.y);
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		//the sparks of the burst fade within half a second
		if (landed) {
			if (time > duration + 0.5f) killAndErase();
			return;
		}
		float t = Math.min(1f, time / duration);
		place(new PointF(from.x + (to.x - from.x)*t, from.y + (to.y - from.y)*t));
		star.angle += 720f * Game.elapsed;
		if (t >= 1f){
			landed = true;
			star.visible = false;
			trail.on = false;
			trail.burst(BlueSpark.FACTORY, 12);
			if (callback != null) callback.call();
		}
	}

	public static class BlueSpark extends PixelParticle.Shrinking {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((BlueSpark)emitter.recycle(BlueSpark.class)).reset(x, y);
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};

		public BlueSpark(){
			super();
			lifespan = 0.4f;
		}

		public void reset( float x, float y ){
			revive();
			this.x = x;
			this.y = y;
			color(Random.Int(2) == 0 ? 0x60A0FF : 0xC0E0FF);
			left = lifespan;
			size = 3;
			speed.polar(Random.Float(PointF.PI2), Random.Float(10, 40));
		}
	}
}
