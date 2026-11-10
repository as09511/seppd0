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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

//the Red Mist's great split slashes, red crescents with a white-hot core:
// vertical cuts down through the target from high above it,
// horizontal sweeps a huge half circle of radius 5 around Gebura towards the chosen direction
public class GreatSplitSlash extends Image {

	private final boolean vertical;
	private float time = 0;

	private static final float GROW = 0.1f;
	private static final float HOLD = 0.12f;
	private static final float FADE = 0.4f;

	private GreatSplitSlash( String asset, boolean vertical ){
		super(asset);
		this.vertical = vertical;
	}

	//a big cut from above the target down through it
	public static GreatSplitSlash vertical( int cell ){
		GreatSplitSlash s = new GreatSplitSlash(Assets.Effects.GREAT_SPLIT_V, true);
		PointF c = DungeonTilemap.raisedTileCenterToWorld(cell);
		s.origin.set(s.width/2f, 0);
		s.x = c.x - s.width/2f;
		s.y = c.y - s.height*0.7f;
		s.scale.set(1f, 0f);
		return s;
	}

	//a half circle sweep around the hero, facing the chosen cell
	public static GreatSplitSlash horizontal( int from, int towards ){
		GreatSplitSlash s = new GreatSplitSlash(Assets.Effects.GREAT_SPLIT_H, false);
		PointF c = DungeonTilemap.raisedTileCenterToWorld(from);
		s.origin.set(s.width/2f, s.height - 6);
		s.x = c.x - s.origin.x;
		s.y = c.y - s.origin.y;
		int w = Dungeon.level.width();
		float dx = towards % w - from % w;
		float dy = towards / w - from / w;
		//the texture faces up, screen angles turn clockwise
		s.angle = (float)Math.toDegrees(Math.atan2(dx, -dy));
		s.scale.set(0.4f);
		return s;
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		if (time < GROW){
			float p = time / GROW;
			if (vertical) scale.set(1f, p);
			else          scale.set(0.4f + 0.6f*p);
			alpha(1f);
		} else if (time < GROW + HOLD){
			scale.set(1f);
			alpha(1f);
		} else if (time < GROW + HOLD + FADE){
			float p = (time - GROW - HOLD) / FADE;
			alpha(1f - p);
			if (!vertical) scale.set(1f + 0.08f*p);
		} else {
			killAndErase();
		}
	}
}
