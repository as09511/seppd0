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
import com.watabou.noosa.TextureFilm;

//Where We Must Reach, Dusk of Green - a towering black machine of vents and grilles with red lights and a green gauge
public class WhereWeMustReachSprite extends MobSprite {

	private Animation build;

	public WhereWeMustReachSprite() {
		super();

		texture( Assets.Sprites.WHERE_WE_MUST_REACH );

		TextureFilm frames = new TextureFilm( texture, 24, 26 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 1, 0, 0, 2 );

		run = idle.clone();

		attack = new Animation( 10, false );
		attack.frames( frames, 3, 4, 5, 0 );

		build = attack.clone();

		zap = attack.clone();

		die = new Animation( 8, false );
		die.frames( frames, 6, 7, 8 );

		perspectiveRaise = 3 / 16f;

		play( idle );
	}

	//a machine of its ordeal has just been built
	public void build(){
		if (curAnim == idle || curAnim == null || curAnim == build) play( build );
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == build){
			idle();
			return;
		}
		super.onComplete(anim);
	}

	@Override
	public int blood() {
		return 0xFF404040;
	}
}
