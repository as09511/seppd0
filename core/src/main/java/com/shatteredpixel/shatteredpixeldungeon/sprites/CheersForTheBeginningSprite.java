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

//Cheers for the Beginning, Dawn of Crimson: a small reveller in a crimson cap that throws its arms up to cheer
public class CheersForTheBeginningSprite extends MobSprite {

	public CheersForTheBeginningSprite() {
		super();

		texture( Assets.Sprites.CHEERS_FOR_THE_BEGINNING );

		TextureFilm frames = new TextureFilm( texture, 12, 14 );

		idle = new Animation( 3, true );
		idle.frames( frames, 0, 0, 1, 0, 2, 0 );

		//it never walks, the run animation is only its idle bounce
		run = new Animation( 6, true );
		run.frames( frames, 0, 1 );

		//the cheer that calls a monster
		attack = new Animation( 8, false );
		attack.frames( frames, 2, 1, 2, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 3, 4, 5 );

		play( idle );
	}

	@Override
	public int blood() {
		return 0xFFC4283C;
	}
}
