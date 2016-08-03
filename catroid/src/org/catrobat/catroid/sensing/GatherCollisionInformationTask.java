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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.exceptions.ProjectException;
import org.catrobat.catroid.stage.PreStageActivity;
import org.catrobat.catroid.ui.dialogs.CustomAlertDialogBuilder;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.exceptions.ProjectException;
import org.catrobat.catroid.ui.dialogs.CustomAlertDialogBuilder;
/**
 * Created by Thomas on 02.08.2016.
 */

public class GatherCollisionInformationTask extends AsyncTask<Void, Void, Boolean> {

	private OnPolygonLoadedListener listener;


	public GatherCollisionInformationTask(OnPolygonLoadedListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		getCollisionInformation();
		return true;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		super.onPostExecute(success);
		Log.i("GatherCollisionInfo", "finished task");
		listener.onFinished();
	}
	private void getCollisionInformation()
	{
		Log.i("Collision Detection", "Waiting for all calculation threads to finish");
		for(Sprite s : ProjectManager.getInstance().getCurrentProject().getSpriteList())
		{
			for(LookData lookData : s.getLookDataList())
			{
				if(lookData.collisionPolygonCalculationThread == null)
					continue;
				try{
					lookData.collisionPolygonCalculationThread.join();
				}
				catch (InterruptedException e)
				{
					Log.i("Collison Detection", "thread got interupted");
				}
			}
		}
		Log.i("Collision Detection", " all threads finished!");

		for (Sprite s : ProjectManager.getInstance().getCurrentProject().getSpriteList()) {
			for (LookData l : s.getLookDataList()) {
				l.loadOrCreateCollisionPolygon();
			}
		}
	}

	public interface OnPolygonLoadedListener {
		void onFinished();
	}
}
