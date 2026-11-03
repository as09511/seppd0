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
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//da capo's score: one ring of staff and notes per movement around Netzach (5x5, 7x7, 9x9, 11x11),
//alternating white and violet and turning in opposite directions. In the finale the score shakes.
public class DaCapoScore extends Group {

	private static final float LIFE  = 1.6f;
	private static final float FADE_IN  = 0.2f;
	private static final float FADE_OUT = 0.6f;
	//outer radius of the staff in the texture, in pixels
	private static final float TEX_RADIUS = 76f;

	private static final int[] COLORS = {0xFFFFFF, 0xB070E0, 0xFFFFFF, 0xB070E0};

	private final CharSprite target;
	private final boolean finale;
	private final Image[] rings;
	private final float[] spin;
	private float time = 0;

	public DaCapoScore( CharSprite target, int movement ){
		super();
		this.target = target;
		this.finale = movement >= 4;

		int count = Math.min(4, movement);
		rings = new Image[count];
		spin = new float[count];
		for (int i = 0; i < count; i++){
			Image ring = new Image(Assets.Effects.DACAPO_SCORE);
			ring.origin.set(ring.width/2f, ring.height/2f);
			//ring i reaches the edge of a (2i+5)x(2i+5) area
			float radius = (i + 2.5f) * 16f;
			ring.scale.set(radius / TEX_RADIUS);
			ring.hardlight(COLORS[i]);
			ring.alpha(0);
			ring.angle = Random.Float(360);
			spin[i] = (i % 2 == 0 ? 1 : -1) * (finale ? 220f : 45f);
			add(ring);
			rings[i] = ring;
		}
		place();
	}

	private void place(){
		PointF c = target.center();
		for (int i = 0; i < rings.length; i++){
			Image ring = rings[i];
			float jitter = finale ? Random.Float(-2f, 2f) : 0;
			ring.x = c.x - ring.width/2f + jitter;
			ring.y = c.y - ring.height/2f + (finale ? Random.Float(-2f, 2f) : 0);
		}
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float a;
		if (time < FADE_IN)                 a = time / FADE_IN;
		else if (time > LIFE - FADE_OUT)    a = Math.max(0, (LIFE - time) / FADE_OUT);
		else                                a = 1f;
		for (int i = 0; i < rings.length; i++){
			rings[i].angle += spin[i] * Game.elapsed;
			rings[i].alpha(a * 0.9f);
		}
		place();
		if (time >= LIFE){
			killAndErase();
		}
	}
}
