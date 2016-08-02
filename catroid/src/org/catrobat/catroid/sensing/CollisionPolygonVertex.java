/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2016 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.sensing;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by michi on 06.04.16.
 */
public class CollisionPolygonVertex {
	public float start_x;
	public float start_y;
	public float end_x;
	public float end_y;


	public CollisionPolygonVertex(float start_x, float start_y, float end_x, float end_y) {
		this.start_x = start_x;
		this.start_y = start_y;
		this.end_x = end_x;
		this.end_y = end_y;
	}

	public boolean equals(CollisionPolygonVertex other) {
		return other.start_x == start_x && other.start_y == start_y && other.end_x == end_x && other.end_y == end_y;
	}

	public void extend(float x, float y) {
		end_x = x;
		end_y = y;
	}

	public String toString() {
		return start_x + "/" + start_y + " -> " + end_x + "/" + end_y;
	}

	public void flip() {
		float x_temp = start_x;
		float y_temp = start_y;
		start_x = end_x;
		start_y = end_y;
		end_x = x_temp;
		end_y = y_temp;

	}

	public PointF getStartPoint() {
		return new PointF(start_x, start_y);
	}

	public PointF getEndPoint() {
		return new PointF(end_x, end_y);
	}

	public boolean isConnected(CollisionPolygonVertex other) {
		boolean connected = other.start_x == this.end_x &&
							other.start_y == this.end_y;
		return connected;
	}

	public boolean isConnectedBackwards(CollisionPolygonVertex other) {
		boolean connected_backwards = other.end_x == this.end_x &&
									  other.end_y == this.end_y;
		return connected_backwards;
	}
}
