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


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//Lobotomy Corporation's risk levels, used by abnormalities and their E.G.O
//This is only the displayed grade, item power is still balanced by tier
public enum RiskLevel {
	ZAYIN   (0x8CE04A), //light green
	TETH    (0x4A9CFF), //blue
	HE      (0xFFE040), //yellow
	WAW     (0xB060FF), //purple
	ALEPH   (0xFF3030); //red

	private final int color;

	RiskLevel(int color){
		this.color = color;
	}

	public int color(){
		return color;
	}

	public String title(){
		return Messages.get(RiskLevel.class, name());
	}

	//the title in its own color, see RenderedTextBlock
	public String coloredTitle(){
		return DamageType.colored(title(), color);
	}
}
