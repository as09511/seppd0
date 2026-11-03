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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

//da capo's score: one ring of staff and notes per movement around Netzach (5x5, 7x7, 9x9, 11x11), alternating white
//and violet and turning in opposite directions. The rings stay for as long as the performance goes on.
//In the finale no ring is added: the score gathers into Netzach and then spreads out, striking each enemy it reaches.
public class DaCapoScore extends Group {

	public interface Hit {
		void hit( Char ch );
	}

	private static final float FADE_IN  = 0.25f;
	private static final float FADE_OUT = 0.5f;
	private static final float GATHER   = 0.2f;
	private static final float BURST    = 0.55f;
	//outer radius of the staff in the texture, in pixels
	private static final float TEX_RADIUS = 76f;
	//the spreading score reaches past the corners of the 13x13 finale area
	private static final float REACH = 6.5f * 16f * 1.45f;
	private static final float MIN_RADIUS = 4f;

	private static final int[] COLORS = {0xFFFFFF, 0xB070E0, 0xFFFFFF, 0xB070E0};
	public static final int MAX_RINGS = 4;

	private enum Phase { PLAYING, FADING, GATHER, BURST }

	private static class Ring {
		Image img;
		float base, radius, start;
		float alpha = 0;
		float spin;
	}

	private final CharSprite target;
	private final ArrayList<Ring> rings = new ArrayList<>();
	private Phase phase = Phase.PLAYING;
	private float time = 0;

	//finale
	private final ArrayList<Char> targets = new ArrayList<>();
	private final ArrayList<Float> reach = new ArrayList<>();
	private Hit hit;
	private Callback done;

	public DaCapoScore( CharSprite target, int rings ){
		super();
		this.target = target;
		for (int i = 0; i < rings; i++) addRing();
		place();
	}

	public CharSprite target(){
		return target;
	}

	public int rings(){
		return rings.size();
	}

	//the next movement adds its ring, the earlier ones keep turning
	public void addRing(){
		if (phase != Phase.PLAYING || rings.size() >= MAX_RINGS) return;
		int i = rings.size();
		Ring r = new Ring();
		r.img = new Image(Assets.Effects.DACAPO_SCORE);
		r.img.origin.set(r.img.width/2f, r.img.height/2f);
		r.img.hardlight(COLORS[i]);
		r.img.alpha(0);
		r.img.angle = Random.Float(360);
		//ring i reaches the edge of a (2i+5)x(2i+5) area
		r.base = r.radius = (i + 2.5f) * 16f;
		r.spin = (i % 2 == 0 ? 1 : -1) * 45f;
		add(r.img);
		rings.add(r);
		place();
	}

	//the performance ended without a finale
	public void fade(){
		if (phase != Phase.PLAYING) return;
		phase = Phase.FADING;
		time = 0;
	}

	//the finale: the score gathers in, then spreads out and strikes every target as it passes them
	public void spread( ArrayList<Char> targets, Hit hit, Callback done ){
		this.targets.addAll(targets);
		this.hit = hit;
		this.done = done;
		for (Ring r : rings){
			r.start = r.radius;
			r.spin *= 5f;
		}
		phase = Phase.GATHER;
		time = 0;
	}

	private void place(){
		if (target == null) return;
		PointF c = target.center();
		boolean shaking = phase == Phase.BURST;
		for (Ring r : rings){
			r.img.scale.set(Math.max(0.01f, r.radius / TEX_RADIUS));
			r.img.x = c.x - r.img.width/2f + (shaking ? Random.Float(-1.5f, 1.5f) : 0);
			r.img.y = c.y - r.img.height/2f + (shaking ? Random.Float(-1.5f, 1.5f) : 0);
		}
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float fade = 1f;

		switch (phase){
			case PLAYING: default:
				break;
			case FADING:
				fade = Math.max(0, 1f - time / FADE_OUT);
				break;
			case GATHER: {
				float p = Math.min(1f, time / GATHER);
				for (Ring r : rings){
					r.radius = r.start + (MIN_RADIUS - r.start) * p * p;
				}
				if (time >= GATHER){
					startBurst();
				}
				break;
			}
			case BURST: {
				float p = Math.min(1f, time / BURST);
				//the outermost ring leads, the others follow a little behind it
				float lead = MIN_RADIUS + (REACH - MIN_RADIUS) * (1f - (1f - p) * (1f - p));
				for (int i = 0; i < rings.size(); i++){
					rings.get(i).radius = Math.max(MIN_RADIUS, lead * (1f - 0.1f * (rings.size() - 1 - i)));
				}
				for (int i = targets.size() - 1; i >= 0; i--){
					if (p >= 1f || reach.get(i) <= lead){
						strike(i);
					}
				}
				if (p > 0.6f) fade = Math.max(0, (1f - p) / 0.4f);
				if (p >= 1f){
					Callback d = done;
					done = null;
					killAndErase();
					if (d != null) d.call();
					return;
				}
				break;
			}
		}

		for (Ring r : rings){
			r.img.angle += r.spin * Game.elapsed;
			if (phase == Phase.PLAYING || phase == Phase.GATHER){
				r.alpha = Math.min(1f, r.alpha + Game.elapsed / FADE_IN);
			}
			r.img.alpha(r.alpha * fade * 0.9f);
		}
		place();

		if (phase == Phase.FADING && time >= FADE_OUT){
			killAndErase();
		}
	}

	private void startBurst(){
		phase = Phase.BURST;
		time = 0;
		PointF c = target.center();
		for (Char ch : targets){
			float d;
			if (ch.sprite != null){
				PointF t = ch.sprite.center();
				d = (float)Math.hypot(t.x - c.x, t.y - c.y);
			} else {
				d = 8f;
			}
			reach.add(d);
		}
	}

	private void strike( int i ){
		Char ch = targets.remove(i);
		reach.remove(i);
		if (hit != null) hit.hit(ch);
	}
}
