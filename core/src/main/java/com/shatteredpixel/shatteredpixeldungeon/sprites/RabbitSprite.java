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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.gebura.RabbitMember;
import com.watabou.noosa.TextureFilm;

//a Rabbit Team member: black combat armor and a helmet with long ears, its visor lit in the colour of its
// damage type. One row per type (RED, WHITE, BLACK, PALE), 21 frames of 12x15 like the hero sprites
public class RabbitSprite extends MobSprite {

	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	private static final int ROW            = 21;

	public RabbitSprite() {
		super();
		texture( Assets.Sprites.RABBIT_TEAM );
		setRow(0);
		idle();
	}

	private void setRow( int row ){
		TextureFilm film = new TextureFilm( texture, FRAME_WIDTH, FRAME_HEIGHT );
		int o = row * ROW;

		idle = new Animation( 1, true );
		idle.frames( film, o, o, o, o+1, o, o, o+1, o+1 );

		run = new Animation( 20, true );
		run.frames( film, o+2, o+3, o+4, o+5, o+6, o+7 );

		die = new Animation( 20, false );
		die.frames( film, o+8, o+9, o+10, o+11, o+12, o+11 );

		attack = new Animation( 15, false );
		attack.frames( film, o+13, o+14, o+15, o );

		zap = attack.clone();
	}

	@Override
	public void link( Char ch ) {
		if (ch instanceof RabbitMember){
			setRow(((RabbitMember) ch).type());
		}
		super.link(ch);
		idle();
	}
}
