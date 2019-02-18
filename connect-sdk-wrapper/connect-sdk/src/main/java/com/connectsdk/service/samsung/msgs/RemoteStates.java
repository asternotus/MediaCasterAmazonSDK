/*
 * Copyright (c) 2015 Samsung Electronics, Inc.
 * All rights reserved.
 */

package com.connectsdk.service.samsung.msgs;

/**
 * Enumerator with states of remote player. These states are used in
 * StatusMessage
 * 
 * @see StatusMessage
 * 
 * @author m.gajewski
 *
 */
public enum RemoteStates {
	NONE, IDLE, BUFFERING, READY, PLAYING, PAUSED, ERROR, STOPPED
}
