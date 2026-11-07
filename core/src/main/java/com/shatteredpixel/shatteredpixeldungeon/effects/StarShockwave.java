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

import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

//a glowing ring that sweeps outward from a point and fades, the Blue Star's shockwave
public class StarShockwave extends Image {

	private static final Object CACHE_KEY = StarShockwave.class;
	private static final int RADIUS = 64;

	private final PointF center;
	private final float maxRadius;
	private final float duration;
	private float time = 0;

	public StarShockwave( PointF center, float maxRadius, float duration, int color ){
		super();
		if (!TextureCache.contains(CACHE_KEY)){
			Pixmap p = TextureCache.create(CACHE_KEY, 2*RADIUS + 1, 2*RADIUS + 1).bitmap;
			p.setBlending(Pixmap.Blending.None);
			p.setColor(0x00000000);
			p.fill();
			//a soft ring: brightest at its outer edge, fading towards the inside
			for (int y = 0; y <= 2*RADIUS; y++){
				for (int x = 0; x <= 2*RADIUS; x++){
					double d = Math.hypot(x - RADIUS, y - RADIUS);
					if (d > RADIUS) continue;
					double edge = RADIUS - d;
					int a;
					if (edge < 3) a = 255;
					else if (edge < 14) a = (int)(200 * (1 - (edge - 3) / 11.0));
					else a = 0;
					if (a > 0) p.drawPixel(x, y, 0xFFFFFF00 | a);
				}
			}
		}
		texture(CACHE_KEY);
		hardlight(color);
		this.center = center;
		this.maxRadius = maxRadius;
		this.duration = duration;
		place(4);
	}

	private void place( float radius ){
		scale.set(radius / RADIUS);
		x = center.x - width()/2f;
		y = center.y - height()/2f;
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		if (time >= duration){
			killAndErase();
			return;
		}
		float p = time / duration;
		place(4 + (maxRadius - 4) * p);
		alpha(0.6f * (1f - p*p));
	}

	@Override
	public void draw() {
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}

	public static void show( Group parent, PointF center, float maxRadius, float duration, int color ){
		if (parent == null) return;
		parent.add(new StarShockwave(center, maxRadius, duration, color));
	}
}
