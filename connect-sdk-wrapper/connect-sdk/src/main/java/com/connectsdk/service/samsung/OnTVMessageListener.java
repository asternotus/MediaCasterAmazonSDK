/*
 * Copyright (c) 2015 Samsung Electronics, Inc.
 * All rights reserved.
 */

package com.connectsdk.service.samsung;

import com.connectsdk.service.samsung.msgs.CustomEvent;
import com.connectsdk.service.samsung.msgs.PlayMessage;
import com.connectsdk.service.samsung.msgs.SimpleTextMessage;
import com.connectsdk.service.samsung.msgs.StatusMessage;

/**
 * Listener interface used to dispatch messages to your application. By this
 * listener messages about playback on remote device are dispatched.
 * 
 * @author m.gajewski
 *
 */
public interface OnTVMessageListener {
	/**
	 * Called when status message is received from remote device.
	 * 
	 * @param msg
	 *            Message object.
	 */
	void onStatusMessage(StatusMessage msg);

	/**
	 * Called when play message is received from remote device.
	 * 
	 * @param msg
	 *            Message object.
	 */
	void onPlayMessage(PlayMessage msg);

	/**
	 * Called when simple text message is received from remote device.
	 * 
	 * @param msg
	 *            Message object.
	 */
	void onSimpleTextMessage(SimpleTextMessage msg);

	/**
	 * Called when MSF receive suspend message
	 */
	void onSuspendMessage();

	/**
	 * Called when MSF receive restore message
	 */
	void onRestoreMessage();

	/**
	 * Called when MSF receive custom event message registered by user
	 * 
	 * @param eventMessage
	 *            Object with all data about event
	 */
	void onCustomEventMessage(CustomEvent eventMessage);
}
