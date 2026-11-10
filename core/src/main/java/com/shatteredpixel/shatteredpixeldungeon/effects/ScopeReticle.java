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

import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

//a red scope crosshair that closes in on a cell, holds for a moment and fades: Gebura's disposal bullet
public class ScopeReticle extends Image {

	private static final float CLOSE = 0.2f;
	private static final float HOLD = 0.25f;
	private static final float FADE = 0.35f;

	private float time = 0;

	public ScopeReticle( int pos ){
		super();
		copy(Icons.get(Icons.TARGET));
		origin.set(width/2f, height/2f);
		//centred on the character standing there, which is drawn a little above its tile
		PointF c = DungeonTilemap.raisedTileCenterToWorld(pos);
		x = c.x - width/2f;
		y = c.y - height/2f;
		hardlight(0xFF2020);
		scale.set(2.5f);
		alpha(0f);
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		if (time < CLOSE){
			float p = time / CLOSE;
			scale.set(2.5f - 1.5f*p);
			alpha(p);
		} else if (time < CLOSE + HOLD){
			scale.set(1f);
			alpha(1f);
		} else if (time < CLOSE + HOLD + FADE){
			alpha(1f - (time - CLOSE - HOLD) / FADE);
		} else {
			killAndErase();
		}
	}
}
