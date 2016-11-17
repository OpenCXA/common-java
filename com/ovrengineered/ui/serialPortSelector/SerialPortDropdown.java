/**
 * Copyright 2013 opencxa.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ovrengineered.serialPortSelector;

import java.util.Enumeration;
import gnu.io.CommPortIdentifier;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;


/**
 * @author Christopher Armenio
 */
public class SerialPortDropdown extends JComboBox<CommPortIdentifier> implements PopupMenuListener
{

	public SerialPortDropdown()
	{
		super();
		this.addPopupMenuListener(this);
		this.setRenderer(new SerialPortRenderer());
	}
	
	
	@Override
	public CommPortIdentifier getSelectedItem()
	{
		Object retVal_raw = super.getSelectedItem();
		return (retVal_raw instanceof CommPortIdentifier) ? ((CommPortIdentifier)retVal_raw) : null;
	}
	
	
	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent arg0)
	{
		this.removeAllItems();
		
		Enumeration<CommPortIdentifier> ports = CommPortIdentifier.getPortIdentifiers();
		while( ports.hasMoreElements() )
		{
			CommPortIdentifier currIdent = ports.nextElement();
			this.addItem(currIdent);
		}
	}

	
	@Override
	public void popupMenuCanceled(PopupMenuEvent arg0){}


	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0){}
}
