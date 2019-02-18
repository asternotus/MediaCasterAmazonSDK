/*
 * Copyright (c) 2015 Samsung Electronics, Inc.
 * All rights reserved.
 */

package com.connectsdk.service.samsung.statemachine;

/**
 * Interface for state change listener
 * 
 * @author t.gdala
 *
 * @param <S>
 *            Enumerator with all possible states
 */
public interface StateChangeListener<S> {
	/**
	 * Called when state is changed
	 * 
	 * @param args
	 *            arguments of state change
	 */
	void stateChanged(StateChangeArgs<S> args);
}
