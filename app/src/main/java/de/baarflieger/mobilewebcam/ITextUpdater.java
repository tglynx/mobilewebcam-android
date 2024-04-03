/* Copyright 2012 Michael Haar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package de.baarflieger.mobilewebcam;

import android.graphics.Bitmap;

// keeps track about a running operation and updates the texts/messages for visible screen
public interface ITextUpdater
{
	public int JobStarted();
	
	public void UpdateText();
	
	public void Toast(String msg, int length);
	
	public void SetPreview(Bitmap image);
	
	public int JobFinished();
}