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
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

//Nothing There, ALEPH - a tall red thing of flesh, its head mostly a mouth, a spiked club of a fist on one arm
// and long claws on the other. The club turns into a crescent axe for Good Bye.
// It stands on its own cell and reaches high above it.
public class NothingThereSprite extends MobSprite {

	private Animation windup;
	private Animation swing;

	public NothingThereSprite() {
		super();

		texture( Assets.Sprites.NOTHING_THERE );

		TextureFilm frames = new TextureFilm( texture, 26, 32 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 1, 1 );

		run = new Animation( 8, true );
		run.frames( frames, 2, 3, 4, 5 );

		//its basic attack is the spiked club brought down
		attack = new Animation( 12, false );
		attack.frames( frames, 6, 7, 7, 8 );

		zap = new Animation( 10, false );
		zap.frames( frames, 9, 9, 10 );

		//Good Bye: the club turns into a great crescent axe, raised up
		windup = new Animation( 8, false );
		windup.frames( frames, 16, 11 );

		swing = new Animation( 14, false );
		swing.frames( frames, 11, 12, 12, 12 );

		die = new Animation( 6, false );
		die.frames( frames, 13, 14, 15 );

		play( idle );
	}

	//"Good Bye": the club turns into an axe and goes up, staying there while the cells in front are marked
	public void windup( int cell ){
		turnTo( ch.pos, cell );
		play( windup );
	}

	//and the axe comes down on them
	public void swing( int cell ){
		turnTo( ch.pos, cell );
		play( swing );
	}

	//"Hello?": the mouth opens and a spike flies out down the line, all the way to its end
	public void spit( int cell, Callback onLand ){
		turnTo( ch.pos, cell );
		play( zap );
		((MissileSprite)parent.recycle( MissileSprite.class )).
				reset( this, cell, new Spike(), onLand );
	}

	@Override
	public void onComplete( Animation anim ) {
		if (anim == zap || anim == swing){
			idle();
			return;
		}
		if (anim == windup){
			//holds the club up until the swing
			return;
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFFC4161A;
	}

	public static class Spike extends Item {
		{
			image = ItemSpriteSheet.THROWING_SPIKE;
		}
	}
}
