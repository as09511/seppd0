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
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Camera;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

//Food Chain, Dusk of Amber - a great dark burrowing mound with an amber dome and pale claws.
//It fills 2x2 cells: the cell it stands on is the bottom left one, so it is drawn half a cell further right.
public class FoodChainSprite extends MobSprite {

	public FoodChainSprite() {
		super();

		texture( Assets.Sprites.FOOD_CHAIN );

		TextureFilm frames = new TextureFilm( texture, 32, 28 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 1, 0 );

		run = new Animation( 6, true );
		run.frames( frames, 2, 3, 4, 3 );

		attack = new Animation( 12, false );
		attack.frames( frames, 5, 6, 7, 0 );

		die = new Animation( 8, false );
		die.frames( frames, 8, 9, 10 );

		perspectiveRaise = 2 / 16f;

		play( idle );
	}

	//faces the way it crawls: 1 right, -1 left, 0 keeps the way it faces now
	public void face( int dir ){
		if (dir > 0) flipHorizontal = false;
		else if (dir < 0) flipHorizontal = true;
	}

	@Override
	public PointF worldToCamera( int cell ) {
		final int csize = DungeonTilemap.SIZE;
		return new PointF(
				PixelScene.align(Camera.main, ((cell % Dungeon.level.width()) + 1f) * csize - width() * 0.5f),
				PixelScene.align(Camera.main, ((cell / Dungeon.level.width()) + 1f) * csize - height() - csize * perspectiveRaise)
		);
	}

	@Override
	public void turnTo( int from, int to ) {
		//it only turns around in horizontal corridors, by itself
	}

	@Override
	public int blood() {
		return 0xFF6A4020;
	}

	//the other three cells of its body are never drawn
	public static class Body extends MobSprite {

		public Body() {
			super();

			texture( Assets.Sprites.FOOD_CHAIN );
			TextureFilm frames = new TextureFilm( texture, 32, 28 );
			idle = new Animation( 1, true );
			idle.frames( frames, 0 );
			run = idle.clone();
			attack = idle.clone();
			die = idle.clone();
			play( idle );
			visible = false;
		}

		@Override
		public void update() {
			super.update();
			visible = false;
		}

		//never drawn, even in a frame where the fog of war update has just made it visible again
		@Override
		public void draw() {
		}

		@Override
		public int blood() {
			return 0xFF6A4020;
		}
	}
}
