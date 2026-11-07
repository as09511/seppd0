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
import com.shatteredpixel.shatteredpixeldungeon.effects.StarShockwave;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

//the Blue Star, ALEPH - a burning blue heart in a wheel of grey limbs reaching out every way.
//It fills 2x2 cells: the cell it stands on is the bottom left one, so it is drawn half a cell further right.
public class BlueStarSprite extends MobSprite {

	public static final int WAVE_COLOR = 0x3AA8FF;

	public BlueStarSprite() {
		super();

		texture( Assets.Sprites.BLUE_STAR );

		TextureFilm frames = new TextureFilm( texture, 32, 32 );

		idle = new Animation( 4, true );
		idle.frames( frames, 0, 1, 2, 1 );

		run = idle.clone();

		attack = new Animation( 12, false );
		attack.frames( frames, 3, 4, 5, 4, 0 );

		die = new Animation( 6, false );
		die.frames( frames, 6, 7, 8 );

		perspectiveRaise = 2 / 16f;

		play( idle );
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
		//it faces every way at once
	}

	private StarShockwave wave;

	//the heart flares and a blue shockwave sweeps out over the whole floor.
	// Only one ring is out at a time, so turns that pass quickly don't pile them up
	public void shockwave(){
		if (wave != null && wave.alive && wave.parent != null) return;
		play( attack );
		//out to the farthest corner of the floor
		PointF c = center();
		float w = Dungeon.level.width() * DungeonTilemap.SIZE, h = Dungeon.level.height() * DungeonTilemap.SIZE;
		float reach = (float)Math.hypot(Math.max(c.x, w - c.x), Math.max(c.y, h - c.y));
		wave = new StarShockwave( c, reach, reach / 600f, WAVE_COLOR );
		GameScene.effect( wave );
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == attack){
			play( idle );
			return;
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFF3AA8FF;
	}

	//the other three cells of its body are never drawn
	public static class Body extends MobSprite {

		public Body() {
			super();

			texture( Assets.Sprites.BLUE_STAR );
			TextureFilm frames = new TextureFilm( texture, 32, 32 );
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

		@Override
		public int blood() {
			return 0xFF3AA8FF;
		}
	}
}
