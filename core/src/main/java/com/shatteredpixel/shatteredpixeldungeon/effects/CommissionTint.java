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

import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Gizmo;

//keeps a Yesod commission target tinted purple, the way corrupted enemies are kept darkened
public class CommissionTint extends Gizmo {

	public static final int COLOR = 0xB060FF;
	public static final float STRENGTH = 0.5f;

	private CharSprite target;

	public CommissionTint( CharSprite target ) {
		super();
		this.target = target;
	}

	@Override
	public void update() {
		super.update();
		target.tint(COLOR, STRENGTH);
	}

	public void remove() {
		target.resetColor();
		killAndErase();
	}

	public static CommissionTint apply( CharSprite sprite ) {
		CommissionTint tint = new CommissionTint( sprite );
		if (sprite.parent != null)
			sprite.parent.add( tint );
		return tint;
	}
}
